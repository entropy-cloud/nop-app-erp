package app.erp.aps.service.processor;

import app.erp.aps.biz.WorkOrderOperationCreationResult;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import app.erp.aps.service.scheduling.ErpApsSchedulingEngine;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * UC-APS-01 WorkOrder 下达→OperationOrder 批量创建编排 Processor（plan RC-R1.86 / P1-RC-088）。
 *
 * <p><b>D1 裁决（选项 B：aps 侧拉取，R1.76 拉取消费先例）</b>：触发模型 = aps job 周期拉取扫描已下达工单
 * + 计划员手动触发 mutation（守卫/幂等同源）。否决选项 A（mfg RELEASED 后置推送）：须新增 mfg-service→aps-dao
 * Java 边且联动失败须隔离 mfg 审核主流程；拉取模型复用 aps-service 既有 mfg-dao compile 依赖（ATP/CTP 先例）
 * 零新边，幂等守卫（同 WorkOrder 已有任一 OperationOrder 即跳过整单）天然可重试。
 *
 * <p><b>跨域只读通道（matrix §9.4 永久豁免目标域 daoFor 直访）</b>：读 mfg 域
 * {@code ErpMfgWorkOrder}/{@code ErpMfgRoutingOperation}/{@code ErpMfgWorkcenter} 经 {@link IDaoProvider}
 * 只读查询——对齐 {@code ErpApsAtpCtpServiceImpl} 范式（aps 单模块测试 I*Biz 强注入 NoSuchBeanFailure 先例，
 * 只读聚合无业务写）。
 *
 * <p>每 step 为 protected，下游可逐个覆盖（产品化）。
 */
public class ErpApsWorkOrderToOperationProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpApsWorkOrderToOperationProcessor.class);

    /** 视为「已下达」的 mfg 工单状态（审核 NOT_STARTED 起，至 IN_PROCESS 的未终态执行段；幂等守卫兜底）。 */
    static final List<String> RELEASED_STATUSES = Arrays.asList(
            "NOT_STARTED", "STOCK_RESERVED", "STOCK_PARTIAL", "IN_PROCESS");

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    // ---------- 编排 ----------

    public WorkOrderOperationCreationResult createOperationOrdersFromWorkOrder(String workOrderId,
                                                                                IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId);

        WorkOrderOperationCreationResult result = new WorkOrderOperationCreationResult();
        // A2 桥接（bridge-main-022）：mfg Long workOrderId → aps String 回显，退役 owner M3.1
        result.setWorkOrderId(ConvertHelper.toString(wo.getId()));
        result.setWorkOrderCode(wo.getCode());

        // step 1: 幂等守卫——同 WorkOrder 已有任一 OperationOrder 即跳过整单（重复触发零重复建单）
        if (hasExistingOperationOrders(ConvertHelper.toString(wo.getId()))) {
            result.setAlreadyCreated(true);
            return result;
        }

        // step 2: 读绑定工艺路线工序列表（sequence = lineNo ASC）
        Map<Integer, ErpMfgRoutingOperation> routingOps = loadRoutingOperations(wo);
        if (routingOps.isEmpty()) {
            // L1 异常分支一：工艺路线缺失→整单跳过 + LOG.warn + notify 告警
            LOG.warn("aps.workorder-no-routing: workOrderId={} code={} routingId={} missing or empty",
                    wo.getId(), wo.getCode(), wo.getRoutingId());
            notify(ErpApsConstants.NOTIFY_EVENT_WORKORDER_NO_ROUTING, wo, null, null, context);
            result.setSkippedNoRouting(true);
            return result;
        }

        // step 3: 按 sequence 依次创建 DRAFT 工序工单；工作中心不存在→该工序拒绝创建 + 告警
        for (ErpMfgRoutingOperation rop : routingOps.values()) {
            if (rop.getWorkcenterId() == null || !workcenterExists(rop.getWorkcenterId())) {
                LOG.warn("aps.operation-workcenter-missing: workOrderId={} code={} sequence={} workcenterId={} not found, reject creation",
                        wo.getId(), wo.getCode(), rop.getLineNo(), rop.getWorkcenterId());
                notify(ErpApsConstants.NOTIFY_EVENT_OPERATION_WORKCENTER_MISSING, wo,
                        rop.getWorkcenterId(), rop.getLineNo(), context);
                result.getRejectedSequences().add(rop.getLineNo());
                continue;
            }
            opOrderDao().saveEntity(buildOperationOrder(wo, rop));
            result.setCreatedCount(result.getCreatedCount() + 1);
        }
        return result;
    }

    public Integer scanReleasedWorkOrders(IServiceContext context) {
        int created = 0;
        for (ErpMfgWorkOrder wo : findReleasedWorkOrders()) {
            // A2 桥接（bridge-main-022）：mfg Long → aps String 入参，退役 owner M3.1
            WorkOrderOperationCreationResult r = createOperationOrdersFromWorkOrder(
                    ConvertHelper.toString(wo.getId()), context);
            created += r.getCreatedCount();
        }
        return created;
    }

    // ---------- step：数据加载与校验（protected，下游可覆盖） ----------

    protected ErpMfgWorkOrder requireWorkOrder(String workOrderId) {
        // A2 桥接（bridge-main-022）：aps String 入参 → mfg Long 实体 API，退役 owner M3.1
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(ConvertHelper.toLong(workOrderId));
        if (wo == null) {
            throw new NopException(ErpApsErrors.ERR_APS_WORK_ORDER_NOT_FOUND)
                    .param(ErpApsErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        return wo;
    }

    protected boolean hasExistingOperationOrders(String workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.setLimit(1);
        return !opOrderDao().findAllByQuery(q).isEmpty();
    }

    /** 工艺路线工序（lineNo ASC）。routingId 缺失或无工序行返回空 map（= 工艺路线缺失语义）。
     *  A2 桥接（bridge-main-021）：mfg ErpMfgRoutingOperation routingId Long 查询，退役 owner M3.1。 */
    protected Map<Integer, ErpMfgRoutingOperation> loadRoutingOperations(ErpMfgWorkOrder wo) {
        Map<Integer, ErpMfgRoutingOperation> bySequence = new TreeMap<>();
        if (wo.getRoutingId() == null) {
            return bySequence;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("routingId", wo.getRoutingId()));
        for (ErpMfgRoutingOperation rop : daoProvider.daoFor(ErpMfgRoutingOperation.class).findAllByQuery(q)) {
            bySequence.put(rop.getLineNo(), rop);
        }
        return bySequence;
    }

    protected boolean workcenterExists(Long workcenterId) {
        return daoProvider.daoFor(ErpMfgWorkcenter.class).getEntityById(workcenterId) != null;
    }

    /** 已下达工单（RELEASED_STATUSES 段），创建时间 ASC 保证扫描顺序稳定。 */
    protected List<ErpMfgWorkOrder> findReleasedWorkOrders() {
        QueryBean q = new QueryBean();
        q.addFilter(in("docStatus", new ArrayList<>(RELEASED_STATUSES)));
        q.addOrderField("createTime", false);
        return daoProvider.daoFor(ErpMfgWorkOrder.class).findAllByQuery(q);
    }

    // ---------- step：实体构造 ----------

    protected ErpApsOperationOrder buildOperationOrder(ErpMfgWorkOrder wo, ErpMfgRoutingOperation rop) {
        ErpApsOperationOrder op = opOrderDao().newEntity();
        op.setCode(buildOpCode(wo, rop));
        // A2 桥接（bridge-main-022/023）：mfg Long id/orgId → aps String 列，退役 owner M3.1
        op.setWorkOrderId(ConvertHelper.toString(wo.getId()));
        op.setOperationName(rop.getOperationName() != null ? rop.getOperationName()
                : (rop.getOperationCode() != null ? rop.getOperationCode() : ("OP-" + rop.getLineNo())));
        op.setSequence(rop.getLineNo());
        op.setMachineId(ConvertHelper.toString(rop.getWorkcenterId()));
        op.setSetupTime(rop.getSetupTime());
        op.setRuntimePerUnit(rop.getRunTime());
        op.setQty(wo.getPlannedQuantity());
        op.setPriority(50);
        op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
        op.setOrgId(ConvertHelper.toString(wo.getOrgId()));
        op.setBusinessDate(CoreMetrics.today());
        op.setRemark("WorkOrder下达自动创建");
        // totalDuration = setupTime + runtimePerUnit × qty（与排产引擎同公式单一真相源，CEILING 整分钟）
        op.setTotalDuration(java.math.BigDecimal.valueOf(
                new ErpApsSchedulingEngine(0, null, null).computeDuration(op)));
        return op;
    }

    protected String buildOpCode(ErpMfgWorkOrder wo, ErpMfgRoutingOperation rop) {
        String prefix = wo.getCode() != null ? wo.getCode() : ("WO" + wo.getId());
        return prefix + "-OP" + rop.getLineNo();
    }

    // ---------- notify 告警（R1.4 范式：无 ACTIVE 模板 config-gated 静默跳过，失败降级不阻断） ----------

    protected void notify(String eventType, ErpMfgWorkOrder wo, Long workcenterId, Integer sequence,
                          IServiceContext context) {
        try {
            java.util.Map<String, Object> ctx = new java.util.LinkedHashMap<>();
            ctx.put("workOrderId", wo.getId());
            ctx.put("workOrderCode", wo.getCode());
            if (workcenterId != null) {
                ctx.put("workcenterId", workcenterId);
            }
            if (sequence != null) {
                ctx.put("sequence", sequence);
            }
            notificationBiz.notify(eventType, ctx, context);
        } catch (Exception e) {
            LOG.warn("aps workorder-to-op notify failed (degraded, main flow continues): eventType={}, workOrderId={}, reason={}",
                    eventType, wo.getId(), e.getMessage());
        }
    }

    // ---------- 同域 DAO ----------

    protected IEntityDao<ErpApsOperationOrder> opOrderDao() {
        return daoProvider.daoFor(ErpApsOperationOrder.class);
    }
}
