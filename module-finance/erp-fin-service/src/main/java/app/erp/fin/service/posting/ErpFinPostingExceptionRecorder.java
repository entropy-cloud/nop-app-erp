package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import io.nop.core.lang.json.JsonTool;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 */
public class ErpFinPostingExceptionRecorder {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;

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
        try {
            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
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
                        return null;
                    }));
        } catch (Exception e) {
            // 持久化失败不阻断主异常传播：仅告警，原过账异常照常向上抛出（失败不静默，但记录器自身失败降级）。
            org.slf4j.LoggerFactory.getLogger(ErpFinPostingExceptionRecorder.class)
                    .warn("过账异常记录写入失败（降级，原过账异常仍向上传播）：traceId={}, errorCode={}, stage={}, reason={}",
                            traceId, errorCode, failedStage, e.getMessage());
        }
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
