package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
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
                       LocalDate voucherDate, Long orgId, Long acctSchemaId,
                       Long currencyId, BigDecimal exchangeRate, String eventData) {
        Long exceptionId = null;
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
                        entity.setFailedStage(failedStage);
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
            LOG.warn("过账异常记录写入失败（降级，原过账异常仍向上传播）：traceId={}, errorCode={}, stage={}, reason={}",
                    traceId, errorCode, failedStage, e.getMessage());
            return;
        }
        // 异常记录已独立事务提交成功，调 notify 派发告警通知（config-gated + 独立事务隔离）
        dispatchNotify(exceptionId, billHeadCode, businessType, postingType, errorCode, errorMessage,
                failedStage, voucherDate, eventData);
    }

    /**
     * 派发过账异常告警通知（config-gated by {@code erp-fin.posting-exception-notify-enabled}）。
     *
     * <p>在独立 REQUIRES_NEW 事务内执行 notify：避免外层正在回滚的主过账事务吞掉通知落库。
     * 通知失败降级（warn）不阻断主异常传播。
     */
    private void dispatchNotify(Long exceptionId, String billHeadCode, String businessType, String postingType,
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
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        notificationBiz.notify(ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION, ctx, serviceCtx);
                        return null;
                    }));
        } catch (Exception e) {
            // 通知派发失败不阻断主异常传播：仅告警
            LOG.warn("过账异常告警通知派发失败（降级）：exceptionId={}, billHeadCode={}, reason={}",
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
}
