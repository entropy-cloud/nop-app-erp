package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import io.nop.core.lang.json.JsonTool;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinPostingException）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 过账异常记录器。在 {@code ErpFinPostingProcessor.process()}/{@code reverseProcess()} 抛
 * {@link NopException} 时，由编排层 catch 块调用本组件，以**独立事务（REQUIRES_NEW）**写入
 * {@link ErpFinPostingException}，确保失败记录不随主过账事务回滚丢失。
 *
 * <p>事务隔离依据：{@code txn().afterCommit} 仅在事务提交成功时触发，回滚路径不执行
 * （见 {@code posting-log.md §裁决2}、{@code ITransactionTemplate.java:87-94}），故失败记录持久化
 * 不可依赖 {@code afterCommit}，必须用独立 session/REQUIRES_NEW。
 *
 * <p>写入失败不阻断主异常传播：本方法吞掉自身持久化异常（仅日志告警），原过账异常照常向上抛出。
 *
 * <p><b>通知派发（plan 2026-07-06-0642-1 §Phase 1）</b>：异常记录以 REQUIRES_NEW 提交后，
 * 在**第二个独立 REQUIRES_NEW 事务**内调 {@link IErpSysNotificationBiz#notify}（{@code fin.posting-exception}）。
 * 双 REQUIRES_NEW 隔离确保：(1) 异常记录不随主过账回滚；(2) 通知落库（ErpSysNotification）独立于
 * 主过账事务与异常记录事务，避免外层正在回滚的事务吞掉通知。通知失败降级（warn）不阻断主异常传播。
 */
public class ErpFinPostingExceptionRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinPostingExceptionRecorder.class);

    /** P2-CK-fin-014：合并路径 MAX_RETRY 对齐 RetryProcessor/RetryHelper（≥3 升级 MANUAL）。 */
    private static final int MAX_RETRY_FOR_MERGE = 3;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setTransactionTemplate(ITransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 以 REQUIRES_NEW 事务写入一条 PENDING 状态的过账异常记录。
     *
     * @param traceId       端到端追踪 ID（来自 PostingRun）
     * @param billHeadCode  业务单据号
     * @param businessType  业务类型
     * @param postingType   过账类型（NORMAL/REVERSAL）
     * @param errorCode     失败 ErrorCode
     * @param errorMessage  错误信息
     * @param failedStage   失败阶段（resolveProvider/generateFacts/...）
     * @param voucherDate   凭证日期
     * @param orgId         核算组织
     * @param acctSchemaId  账套
     * @param currencyId    币种（重试重建事件用）
     * @param exchangeRate  汇率（重试重建事件用）
     * @param eventData     原始事件 billData（JSON），重试重建事件用；可为 null
     */
    public void record(String traceId, String billHeadCode, String businessType, String postingType,
                       String errorCode, String errorMessage, String failedStage,
                       LocalDate voucherDate, String orgId, String acctSchemaId,
                       String currencyId, BigDecimal exchangeRate, String eventData) {
        // P2-CK-fin-014：同 (businessType, billHeadCode, postingType, 粗粒度通道) 已有 PENDING 时合并
        // （刷新最新失败证据 + 递增既有 retryCount），不新增重复行——修复手动重试失败每次增生
        // PENDING 扩大 sweep 并发面。粗粒度通道归一：run.currentStage 为细粒度管道阶段名
        // （resolveProvider/generateFacts/persistVoucher_{schemaId}…），归一为 post/reverse 通道值；
        // listener 失败（FAILED_STAGE_NOTIFY_*）自成通道不与引擎管道失败合并。
        if (mergeIntoExistingPending(billHeadCode, businessType, postingType, errorCode, errorMessage,
                failedStage, voucherDate, orgId, acctSchemaId, currencyId, exchangeRate, eventData)) {
            return;
        }
        String exceptionId = null;
        try {
            exceptionId = transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
                        ErpFinPostingException entity = dao.newEntity();
                        entity.setTraceId(traceId);
                        entity.setBillHeadCode(billHeadCode);
                        entity.setBusinessType(businessType);
                        entity.setPostingType(postingType);
                        entity.setErrorCode(errorCode);
                        entity.setErrorMessage(truncate(errorMessage, 500));
                        // P2-CK-fin-014：落库即存粗粒度通道值（首行/合并行口径一致，否则合并查询不命中）
                        entity.setFailedStage(coarseChannel(postingType, failedStage));
                        entity.setVoucherDate(voucherDate);
                        entity.setOrgId(orgId);
                        entity.setAcctSchemaId(acctSchemaId);
                        entity.setCurrencyId(currencyId);
                        entity.setExchangeRate(exchangeRate);
                        entity.setEventData(truncate(eventData, 4000));
                        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING);
                        entity.setRetryCount(0);
                        entity.setOccurrenceTime(CoreMetrics.currentTimestamp());
                        dao.saveEntity(entity);
                        session.flush();
                        return entity.getId();
                    }));
        } catch (Exception e) {
            // 持久化失败不阻断主异常传播：仅告警，原过账异常照常向上抛出（失败不静默，但记录器自身失败降级）。
            LOG.warn("posting exception record write failed (degraded; original posting exception still propagates): traceId={}, errorCode={}, stage={}, reason={}",
                    traceId, errorCode, failedStage, e.getMessage());
            return;
        }
        // 异常记录已独立事务提交成功，调 notify 派发告警通知（config-gated + 独立事务隔离）
        dispatchNotify(exceptionId, billHeadCode, businessType, postingType, errorCode, errorMessage,
                failedStage, voucherDate, eventData);
    }

    /** P2-CK-fin-014：粗粒度通道归一——引擎管道失败统一 post/reverse 通道值。 */
    public static String coarseChannel(String postingType, String failedStage) {
        if (failedStage != null && (failedStage.startsWith("NOTIFY_")
                || failedStage.startsWith("notify-"))) {
            return failedStage;
        }
        return ErpFinConstants.POSTING_TYPE_REVERSAL.equals(postingType) ? "reverse" : "post";
    }

    /** 合并进既有 PENDING（同 bill+type+postingType+粗粒度通道）；命中返回 true。 */
    private boolean mergeIntoExistingPending(String billHeadCode, String businessType, String postingType,
                                             String errorCode, String errorMessage, String failedStage,
                                             LocalDate voucherDate, String orgId, String acctSchemaId,
                                             String currencyId, BigDecimal exchangeRate, String eventData) {
        final String[] existingIdHolder = new String[1];
        final boolean[] manualEscalated = new boolean[1];
        try {
            Boolean merged = transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
                        QueryBean q = new QueryBean();
                        q.addFilter(and(eq("billHeadCode", billHeadCode), eq("businessType", businessType),
                                eq("postingType", postingType),
                                eq("failedStage", coarseChannel(postingType, failedStage)),
                                eq("status", ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING)));
                        ErpFinPostingException existing = dao.findAllByQuery(q).stream().findFirst().orElse(null);
                        if (existing == null) {
                            return false;
                        }
                        existing.setErrorCode(errorCode);
                        existing.setErrorMessage(truncate(errorMessage, 500));
                        existing.setFailedStage(coarseChannel(postingType, failedStage));
                        existing.setEventData(truncate(eventData, 4000));
                        existing.setOccurrenceTime(CoreMetrics.currentTimestamp());
                        existing.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1);
                        if (existing.getRetryCount() >= MAX_RETRY_FOR_MERGE) {
                            existing.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL);
                        }
                        if (ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL.equals(existing.getStatus())) {
                            existingIdHolder[0] = existing.getId();
                            manualEscalated[0] = true;
                        }
                        dao.updateEntity(existing);
                        session.flush();
                        return true;
                    }));
            if (Boolean.TRUE.equals(merged)) {
                LOG.warn("erp-fin-posting-exception-merged-into-existing-pending: billHeadCode={}, businessType={}, channel={}",
                        billHeadCode, businessType, coarseChannel(postingType, failedStage));
                // P2-CK-fin-014（结束审计整改 #3）：G2 MAX_RETRY 升级 MANUAL 派发告警（posting-log.md
                // 明文契约，对齐 sweep dispatchMaxRetryAlert 先例；复用 config-gated dispatchNotify 通道）。
                if (manualEscalated[0]) {
                    dispatchNotify(existingIdHolder[0], billHeadCode, businessType, postingType,
                            errorCode, errorMessage, coarseChannel(postingType, failedStage), voucherDate, eventData);
                }
            }
            return Boolean.TRUE.equals(merged);
        } catch (Exception e) {
            // 合并探查失败降级为新增行（不阻断原过账异常传播）
            LOG.warn("erp-fin-posting-exception-merge-probe-failed (degraded): billHeadCode={}, reason={}",
                    billHeadCode, e.getMessage());
            return false;
        }
    }

    /** 派发过账异常告警通知（config-gated by {@code erp-fin.posting-exception-notify-enabled}）。
     *
     * <p>在独立 REQUIRES_NEW 事务内执行 notify：避免外层正在回滚的主过账事务吞掉通知落库。
     * 通知失败降级（warn）不阻断主异常传播。
     */
    private void dispatchNotify(String exceptionId, String billHeadCode, String businessType, String postingType,
                                String errorCode, String errorMessage, String failedStage,
                                LocalDate voucherDate, String eventData) {
        if (!isNotifyEnabled() || notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("exceptionId", exceptionId);
        ctx.put("billHeadCode", billHeadCode);
        ctx.put("businessType", businessType);
        ctx.put("postingType", postingType);
        ctx.put("errorCode", errorCode);
        ctx.put("errorMessage", truncate(errorMessage, 200));
        ctx.put("failedStage", failedStage);
        ctx.put("voucherDate", voucherDate);
        // 模板可用字段：postingNo（取业务单据号）+ amount（如有，从 eventData JSON 派生，无则不渲染）
        ctx.put("postingNo", billHeadCode);
        BigDecimal amount = deriveAmountFromEventData(eventData);
        if (amount != null) {
            ctx.put("amount", amount);
        }
        IServiceContext serviceCtx = serviceContext();
        try {
            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        notificationBiz.notify(ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION, ctx, serviceCtx);
                        return null;
                    }));
        } catch (Exception e) {
            // 通知派发失败不阻断主异常传播：仅告警
            LOG.warn("posting exception alert notification dispatch failed (degraded): exceptionId={}, billHeadCode={}, reason={}",
                    exceptionId, billHeadCode, e.getMessage());
        }
    }

    private boolean isNotifyEnabled() {
        return AppConfig.var(ErpFinConstants.CONFIG_POSTING_EXCEPTION_NOTIFY_ENABLED, true);
    }

    /** 从原始 eventData JSON 中尝试派生金额（TOTAL/AMOUNT 优先），无则返回 null（模板不渲染该字段）。 */
    private BigDecimal deriveAmountFromEventData(String eventData) {
        if (StringHelper.isBlank(eventData)) {
            return null;
        }
        try {
            Map<String, Object> data = JsonTool.parseBeanFromText(eventData, Map.class);
            if (data == null) {
                return null;
            }
            Object total = data.get("TOTAL");
            if (total instanceof BigDecimal) {
                return (BigDecimal) total;
            }
            if (total instanceof Number) {
                return new BigDecimal(total.toString());
            }
            Object amount = data.get("AMOUNT");
            if (amount instanceof BigDecimal) {
                return (BigDecimal) amount;
            }
            if (amount instanceof Number) {
                return new BigDecimal(amount.toString());
            }
        } catch (Exception ignore) {
            // 解析失败：金额字段留空，模板不渲染
        }
        return null;
    }

    /** 将原始 billData 序列化为 JSON（供 record() 的 eventData 参数与重试重建事件用）。 */
    public static String serializeEventData(Map<String, Object> billData) {
        if (billData == null || billData.isEmpty()) {
            return null;
        }
        return JsonTool.serialize(billData, false);
    }

    /** 反序列化 eventData JSON 为 Map（重试重建事件用）。 */
    public static Map<String, Object> deserializeEventData(String eventData) {
        if (StringHelper.isBlank(eventData)) {
            return null;
        }
        return JsonTool.parseBeanFromText(eventData, Map.class);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
