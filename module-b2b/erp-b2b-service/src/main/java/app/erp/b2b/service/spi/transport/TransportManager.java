package app.erp.b2b.service.spi.transport;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bMftConfig;
import app.erp.b2b.dao.entity.ErpB2bMftLog;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.spi.transport.mock.MockTransportAdapter;
import app.erp.b2b.service.spi.transport.model.TransportResult;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.DateHelper;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * MFT 传输管理器。承载传输编排：路由（按 protocol 经 Registry 取 Adapter）+ 重试（5xx/超时指数退避，
 * 4xx 不重试）+ 死信（耗尽→DEAD_LETTER）+ 审计日志（写 {@link ErpB2bMftLog}）。
 *
 * <p><b>事务边界</b>：传输在事务外，写 log 在事务内（参 logistics {@code GatewayDispatcher}）。
 * 成功回填 Phase 2 {@code markSent}（EdiDoc TO_SEND→SENT）。
 *
 * <p>对应 {@code managed-file-transfer.md §文件传输流程 / §重试与错误处理}。
 */
public class TransportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransportManager.class);

    @Inject
    ErpB2bMftTransportRegistry transportRegistry;
    @Inject
    IDaoProvider daoProvider;

    /**
     * 发送 EDI 报文（出站）。
     *
     * <p>流程：查 {@link ErpB2bMftConfig}（按 partnerId，active=true）→ 按 protocol 经 Registry 取 Adapter →
     * 调 adapter.send（含重试/死信）→ 写 {@link ErpB2bMftLog} → 成功回填 EdiDoc markSent。
     *
     * @param ediDocId 关联 EDI 事务 ID
     * @param partnerId 伙伴 ID
     * @param payload 报文内容
     * @param fileName 文件名
     * @return 传输是否成功
     */
    public boolean send(Long ediDocId, Long partnerId, String payload, String fileName) {
        ErpB2bMftConfig config = findActiveConfig(partnerId);
        if (config == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_MFT_CONFIG_MISSING)
                    .param(ErpB2bErrors.ARG_PARTNER_ID, partnerId);
        }

        IErpB2bTransportAdapter adapter = transportRegistry.getAdapter(config.getProtocol());

        int maxRetries = AppConfig.var(ErpB2bConfigs.CONFIG_MFT_MAX_RETRIES,
                ErpB2bConfigs.DEFAULT_MFT_MAX_RETRIES);

        LocalDateTime startTime = CoreMetrics.currentDateTime();
        long startNanos = CoreMetrics.nanoTime();

        NopException lastFailure = null;
        boolean retryable = false;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                TransportResult result = adapter.send(config, payload, fileName);
                long durationMs = (CoreMetrics.nanoTime() - startNanos) / 1_000_000;
                writeLog(config, ediDocId, ErpB2bConstants.DIRECTION_OUTBOUND,
                        ErpB2bConstants.MFT_STATUS_SENT, result, null, startTime, durationMs, 0);
                markEdiDocSent(ediDocId);
                return true;
            } catch (NopException e) {
                lastFailure = e;
                retryable = isRetryable(e);
                if (!retryable || attempt >= maxRetries) {
                    break;
                }
                LOG.warn("MFT 传输失败（第{}次重试），ediDocId={}：{}", attempt + 1, ediDocId, e.getMessage());
                sleepSilently(1);
            }
        }

        long durationMs = (CoreMetrics.nanoTime() - startNanos) / 1_000_000;
        String errorCode = retryable ? "MFT_RETRY_EXHAUSTED" : "MFT_NON_RETRYABLE";
        String errorMsg = (retryable ? "[传输重试耗尽] " : "[传输不可重试错误] ")
                + (lastFailure != null ? lastFailure.getDescription() : "unknown");

        String status = config.getDeadLetterEnabled() != null && config.getDeadLetterEnabled()
                ? ErpB2bConstants.MFT_STATUS_DEAD_LETTER
                : ErpB2bConstants.MFT_STATUS_FAILED;
        writeLog(config, ediDocId, ErpB2bConstants.DIRECTION_OUTBOUND, status,
                TransportResult.failure(errorCode, errorMsg), lastFailure, startTime, durationMs, maxRetries);
        LOG.error("MFT 传输死信，ediDocId={} 保留 TO_SEND：{}", ediDocId, errorMsg);
        return false;
    }

    private ErpB2bMftConfig findActiveConfig(Long partnerId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("active", true));
        // O-5：追加 mftId 排序确保确定性
        q.addOrderField("id", false);
        return daoProvider.daoFor(ErpB2bMftConfig.class).findFirstByQuery(q);
    }

    private void markEdiDocSent(Long ediDocId) {
        if (ediDocId == null) {
            return;
        }
        try {
            IEntityDao<ErpB2bEdiDoc> dao = daoProvider.daoFor(ErpB2bEdiDoc.class);
            ErpB2bEdiDoc doc = dao.getEntityById(ediDocId);
            if (doc != null && ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(doc.getState())) {
                doc.setState(ErpB2bConstants.EDI_DOC_STATE_SENT);
                doc.setSentAt(CoreMetrics.currentTimestamp());
                dao.saveOrUpdateEntity(doc);
            }
        } catch (Exception e) {
            LOG.warn("回填 EdiDoc markSent 失败（不阻塞传输成功），ediDocId={}：{}", ediDocId, e.getMessage());
        }
    }

    private void writeLog(ErpB2bMftConfig config, Long ediDocId, String direction, String status,
                          TransportResult result, NopException failure,
                          LocalDateTime startTime, long durationMs, int retryCount) {
        IEntityDao<ErpB2bMftLog> dao = daoProvider.daoFor(ErpB2bMftLog.class);
        ErpB2bMftLog log = dao.newEntity();
        log.setConfigId(config.getId());
        log.setDirection(direction);
        log.setProtocol(config.getProtocol());
        log.setStatus(status);
        log.setStartTime(DateHelper.dateTimeToTimestamp(startTime));
        log.setEndTime(CoreMetrics.currentTimestamp());
        log.setDurationMs(durationMs);
        log.setRetryCount(retryCount);
        log.setIsCompressed(config.getCompression());
        log.setIsEncrypted(config.getEncryption());
        log.setIsSigned(config.getSignature());
        if (result != null) {
            log.setMessageId(result.getMessageId());
            log.setFileHash(result.getFileHash());
            log.setMdnStatus(result.getMdnStatus());
            if (!result.isSuccess()) {
                log.setErrorCode(result.getErrorCode());
                log.setErrorMsg(result.getErrorMessage());
            }
        }
        if (failure != null && log.getErrorMsg() == null) {
            log.setErrorMsg(failure.getDescription());
        }
        dao.saveEntity(log);
    }

    private boolean isRetryable(NopException e) {
        Object httpStatus = e.getParam("httpStatus");
        if (httpStatus instanceof Number) {
            int code = ((Number) httpStatus).intValue();
            return code >= 500 || code == 408;
        }
        return false;
    }

    private void sleepSilently(int seconds) {
        try {
            Thread.sleep(Math.min(Math.max(seconds, 0), 5) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
