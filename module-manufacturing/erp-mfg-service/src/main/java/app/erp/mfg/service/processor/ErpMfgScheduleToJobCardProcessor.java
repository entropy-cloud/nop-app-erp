package app.erp.mfg.service.processor;

import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.biz.IErpApsLoadSourceProvider;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * APS 排程 → 工序卡自动生成 Processor（plan 2026-07-05-0427-3）。
 *
 * <p>Facade {@code ErpMfgWorkOrderBizModel} 的 {@code generateJobCardsFromSchedule} /
 * {@code findWorkOrdersPendingJobCards} / {@code generatePendingJobCards} 委托本类。
 *
 * <p>跨域读 APS 已排程工序：复用 {@link IErpApsLoadSourceProvider} SPI（plan 0306-2 范式，声明于 mfg-dao、
 * 实现于 aps-service，经 {@code ioc:collect-beans by-type} 收集）。选用理由：SPI 已暴露本计划所需的
 * workOrderId→PLANNED 工序时段（workcenterId/plannedStartT/plannedEndT/sequence/operationOrderId），
 * 且 APS 模块缺失时收集到空 list（行为降级为「无排程」），无需 mfg-service 新增对 aps-dao 实体的编译依赖，
 * 也无需向 {@code IErpApsOperationOrderBiz} 增加专用查询方法（避免扩大跨域 I*Biz 契约）。
 *
 * <p>同域持久化：{@link IDaoProvider}（对齐既有 {@code ErpMfgJobCardProcessor}/{@code ErpMfgWorkOrderProcessor}
 * 的 Processor 层范式——Processor 非 BizModel，同域实体读写用 IDaoProvider）。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpMfgScheduleToJobCardProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpMfgScheduleToJobCardProcessor.class);

    static final Set<String> ALLOWED_STATES_FOR_GEN = new HashSet<>(Arrays.asList(
            ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED,
            ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED,
            ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL,
            ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS,
            ErpMfgConstants.WORK_ORDER_STATUS_STOPPED));

    static final int DEFAULT_PENDING_LIMIT = 100;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 跨域 APS 排程来源 SPI（可选注入——APS 模块缺失时为空 list，{@code fetchSlots} 返回空）。
     */
    @Inject
    List<IErpApsLoadSourceProvider> apsLoadSourceProviders;

    public void setApsLoadSourceProviders(List<IErpApsLoadSourceProvider> apsLoadSourceProviders) {
        this.apsLoadSourceProviders = apsLoadSourceProviders;
    }

    public ErpMfgWorkOrder generateJobCardsFromSchedule(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId);
        validateStatusForJobCardGen(wo);

        List<ApsLoadSlot> slots = fetchSlots(Collections.singletonList(workOrderId));
        if (slots.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_NO_SCHEDULED_OPERATIONS)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }

        List<ErpMfgJobCard> existing = findJobCardsForWorkOrder(workOrderId);
        List<ApsLoadSlot> toBuild = resolveSlotsToBuild(slots, existing, wo);
        if (toBuild.isEmpty()) {
            return wo;
        }

        for (ApsLoadSlot slot : toBuild) {
            ErpMfgJobCard jc = newJobCard(wo, slot);
            jobCardDao().saveEntity(jc);
        }

        markWorkOrderScheduled(wo, slots);
        workOrderDao().updateEntity(wo);
        return wo;
    }

    public List<ErpMfgWorkOrder> findWorkOrdersPendingJobCards(Integer limit, IServiceContext context) {
        if (apsLoadSourceProviders == null || apsLoadSourceProviders.isEmpty()) {
            return Collections.emptyList();
        }
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_PENDING_LIMIT : limit;

        List<ErpMfgWorkOrder> candidates = findCandidateWorkOrders(effectiveLimit);
        if (candidates.isEmpty()) {
            return candidates;
        }

        List<Long> ids = new ArrayList<>(candidates.size());
        for (ErpMfgWorkOrder wo : candidates) {
            ids.add(wo.getId());
        }
        Set<Long> scheduledIds = new HashSet<>();
        for (ApsLoadSlot slot : fetchSlots(ids)) {
            if (slot.getWorkOrderId() != null) {
                scheduledIds.add(slot.getWorkOrderId());
            }
        }
        if (scheduledIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> alreadyHasJobCards = new HashSet<>(findWorkOrderIdsWithJobCards(scheduledIds));
        List<ErpMfgWorkOrder> result = new ArrayList<>(effectiveLimit);
        for (ErpMfgWorkOrder wo : candidates) {
            if (!scheduledIds.contains(wo.getId())) {
                continue;
            }
            if (alreadyHasJobCards.contains(wo.getId())) {
                continue;
            }
            result.add(wo);
            if (result.size() >= effectiveLimit) {
                break;
            }
        }
        return result;
    }

    public Integer generatePendingJobCards(IServiceContext context) {
        if (!isAutoGenerateEnabled()) {
            LOG.info("erp-mfg-jobcard-auto-gen-skipped: erp-mfg.jobcard-auto-generate-on-schedule=false");
            return 0;
        }
        List<ErpMfgWorkOrder> pending = findWorkOrdersPendingJobCards(DEFAULT_PENDING_LIMIT, context);
        if (pending.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (ErpMfgWorkOrder wo : pending) {
            try {
                generateJobCardsFromSchedule(wo.getId(), context);
                success++;
            } catch (Exception e) {
                LOG.warn("erp-mfg-jobcard-auto-gen-failed: workOrderId={} code={}", wo.getId(), wo.getCode(), e);
            }
        }
        LOG.info("erp-mfg-jobcard-auto-gen-done: total={} success={}", pending.size(), success);
        return success;
    }

    // ---------- step：校验/解析（protected，下游可逐个覆盖） ----------

    protected void validateStatusForJobCardGen(ErpMfgWorkOrder wo) {
        String status = wo.getDocStatus();
        if (status == null || !ALLOWED_STATES_FOR_GEN.contains(status)) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_STATUS_NOT_ALLOWED_FOR_JOB_CARD_GEN)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, status);
        }
    }

    /**
     * 决定本次实际建卡的工序集合：无既有卡→全部；有既有卡且 incremental 配置开→仅补缺；否则抛幂等错。
     */
    protected List<ApsLoadSlot> resolveSlotsToBuild(List<ApsLoadSlot> slots, List<ErpMfgJobCard> existing,
                                                    ErpMfgWorkOrder wo) {
        if (existing.isEmpty()) {
            return slots;
        }
        if (!isIncrementalRebuildEnabled()) {
            throw new NopException(ErpMfgErrors.ERR_JOB_CARDS_ALREADY_GENERATED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                    .param(ErpMfgErrors.ARG_EXISTING_COUNT, existing.size());
        }
        Set<Long> existingSourceIds = new HashSet<>();
        for (ErpMfgJobCard jc : existing) {
            if (jc.getSourceScheduleId() != null) {
                existingSourceIds.add(jc.getSourceScheduleId());
            }
        }
        List<ApsLoadSlot> missing = new ArrayList<>();
        for (ApsLoadSlot slot : slots) {
            Long srcId = slot.getOperationOrderId();
            if (srcId == null || !existingSourceIds.contains(srcId)) {
                missing.add(slot);
            }
        }
        if (missing.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_JOB_CARDS_ALREADY_GENERATED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                    .param(ErpMfgErrors.ARG_EXISTING_COUNT, existing.size());
        }
        return missing;
    }

    protected ErpMfgJobCard newJobCard(ErpMfgWorkOrder wo, ApsLoadSlot slot) {
        IEntityDao<ErpMfgJobCard> dao = jobCardDao();
        ErpMfgJobCard jc = dao.newEntity();
        jc.setWorkOrderId(wo.getId());
        jc.setWorkcenterId(slot.getWorkcenterId());
        jc.setLineNo(slot.getSequence() != null ? slot.getSequence() : 0);
        jc.setPlannedQuantity(nz(wo.getPlannedQuantity()));
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
        jc.setSourceScheduleId(slot.getOperationOrderId());
        jc.setCode(buildJobCardCode(wo, slot));
        jc.setRemark("APS排程自动生成");
        // JobCard 无 plannedStartT/plannedEndT 字段（仅有 actualStartTime/actualEndTime）；
        // 排程时间经 sourceScheduleId 弱参照溯源至 ErpApsOperationOrder，不重复冗余存储。
        return jc;
    }

    protected void markWorkOrderScheduled(ErpMfgWorkOrder wo, List<ApsLoadSlot> slots) {
        wo.setSourceOrderType(ErpMfgConstants.SOURCE_ORDER_TYPE_APS_SCHEDULE);
        if (wo.getSourceScheduleId() == null && !slots.isEmpty()) {
            Long firstId = slots.get(0).getOperationOrderId();
            if (firstId != null) {
                wo.setSourceScheduleId(firstId);
            }
        }
    }

    protected String buildJobCardCode(ErpMfgWorkOrder wo, ApsLoadSlot slot) {
        String prefix = wo.getCode() != null ? wo.getCode() : ("WO" + wo.getId());
        int seq = slot.getSequence() != null ? slot.getSequence() : 0;
        return prefix + "-OP" + seq;
    }

    // ---------- 查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {
        ErpMfgWorkOrder wo = workOrderDao().getEntityById(workOrderId);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        return wo;
    }

    protected List<ApsLoadSlot> fetchSlots(List<Long> workOrderIds) {
        if (apsLoadSourceProviders == null || apsLoadSourceProviders.isEmpty()) {
            return Collections.emptyList();
        }
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApsLoadSlot> all = new ArrayList<>();
        for (IErpApsLoadSourceProvider provider : apsLoadSourceProviders) {
            List<ApsLoadSlot> slots = provider.findScheduledSlots(workOrderIds, null, null);
            if (slots != null) {
                all.addAll(slots);
            }
        }
        all.removeIf(s -> s.getPlannedStartT() == null || s.getPlannedEndT() == null
                || s.getOperationOrderId() == null);
        return all;
    }

    protected List<ErpMfgJobCard> findJobCardsForWorkOrder(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        return jobCardDao().findAllByQuery(q);
    }

    @SuppressWarnings("unchecked")
    protected List<Long> findWorkOrderIdsWithJobCards(Set<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("workOrderId", new ArrayList<>(workOrderIds)));
        q.setLimit(workOrderIds.size());
        IEntityDao<ErpMfgJobCard> dao = jobCardDao();
        List<ErpMfgJobCard> cards = dao.findAllByQuery(q);
        Set<Long> ids = new HashSet<>();
        for (ErpMfgJobCard jc : cards) {
            if (jc.getWorkOrderId() != null) {
                ids.add(jc.getWorkOrderId());
            }
        }
        return new ArrayList<>(ids);
    }

    protected List<ErpMfgWorkOrder> findCandidateWorkOrders(int limit) {
        QueryBean q = new QueryBean();
        q.addFilter(in("docStatus", new ArrayList<>(ALLOWED_STATES_FOR_GEN)));
        q.addOrderField("id", true);
        q.setLimit(limit);
        return workOrderDao().findAllByQuery(q);
    }

    protected boolean isAutoGenerateEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_JOBCARD_AUTO_GENERATE_ON_SCHEDULE, false);
    }

    protected boolean isIncrementalRebuildEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_JOBCARD_INCREMENTAL_REBUILD, false);
    }

    protected boolean readBoolConfig(String key, boolean defaultValue) {
        try {
            String value = AppConfig.var(key, String.valueOf(defaultValue));
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpMfgJobCard> jobCardDao() {
        return daoProvider.daoFor(ErpMfgJobCard.class);
    }

    protected IEntityDao<ErpMfgWorkOrder> workOrderDao() {
        return daoProvider.daoFor(ErpMfgWorkOrder.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
