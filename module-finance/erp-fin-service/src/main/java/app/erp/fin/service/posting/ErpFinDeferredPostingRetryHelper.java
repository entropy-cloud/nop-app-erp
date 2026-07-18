package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 过账异常重试帮助类（plan 2026-07-18-1600-1 Phase 3，自原 {@code DeferredPostingSweepJob} 抽取）。
 *
 * <p>供 nop-batch {@code deferred-posting-sweep.batch.xml} 的 processor 按记录调用：
 * 单条 {@link ErpFinPostingException} 经 REQUIRES_NEW 独立事务重建 {@link PostingEvent} 重试过账，
 * 成功标记 RETRIED，失败递增 retryCount。单条失败隔离不阻断 batch 继续处理其他记录（由 batch skipPolicy 兜底）。
 *
 * <p>O-16 补偿：REQUIRES_NEW 已提交但调用方在 posted=true 设置前失败的场景，
 * {@code voucherBiz.post()} 经引擎 {@code alreadyPosted()} 幂等命中返回 null，本类据此标记 RETRIED（补偿成功）。
 */
public class ErpFinDeferredPostingRetryHelper {

    static final Logger LOG = LoggerFactory.getLogger(ErpFinDeferredPostingRetryHelper.class);

    static final int MAX_RETRY = 3;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setTransactionTemplate(ITransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setVoucherBiz(IErpFinVoucherBiz voucherBiz) {
        this.voucherBiz = voucherBiz;
    }

    /**
     * 重试单条异常记录。成功（含幂等命中）返回 true；失败返回 false（retryCount 已递增）。
     * 每条独立 REQUIRES_NEW 事务，单条失败不阻断其他记录。
     */
    public boolean retry(Long exceptionId, IServiceContext ctx) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        ErpFinPostingException ex = dao.getEntityById(exceptionId);
        if (ex == null) {
            return true;
        }
        try {
            return transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        doRetry(ex, ctx);
                        markRetried(ex);
                        session.flush();
                        return true;
                    }));
        } catch (Exception e) {
            incrementRetryAndRethrow(ex, e);
            return false;
        }
    }

    protected void doRetry(ErpFinPostingException ex, IServiceContext ctx) {
        String postingType = ex.getPostingType();
        ErpFinBusinessType businessType = parseBusinessType(ex.getBusinessType());
        if (ErpFinConstants.POSTING_TYPE_REVERSAL.equals(postingType)) {
            voucherBiz.reverse(ex.getBillHeadCode(), businessType, ctx);
        } else {
            PostingEvent event = rebuildEvent(ex);
            if (event != null) {
                Long voucherId = voucherBiz.post(event, ctx);
                LOG.debug("erp-fin-deferred-posting-retry-post: exceptionId={}, billHeadCode={}, voucherId={}",
                        ex.getId(), ex.getBillHeadCode(), voucherId);
            }
        }
    }

    protected PostingEvent rebuildEvent(ErpFinPostingException ex) {
        String eventData = ex.getEventData();
        if (StringHelper.isBlank(eventData)) {
            return null;
        }
        Map<String, Object> billData = ErpFinPostingExceptionRecorder.deserializeEventData(eventData);
        PostingEvent event = new PostingEvent();
        ErpFinBusinessType businessType = parseBusinessType(ex.getBusinessType());
        event.setBusinessType(businessType);
        event.setBillHeadCode(ex.getBillHeadCode());
        event.setOrgId(ex.getOrgId());
        event.setAcctSchemaId(ex.getAcctSchemaId());
        event.setCurrencyId(ex.getCurrencyId());
        event.setExchangeRate(ex.getExchangeRate());
        event.setVoucherDate(ex.getVoucherDate());
        event.setBillData(billData);
        return event;
    }

    protected void markRetried(ErpFinPostingException ex) {
        ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
        ex.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
        ex.setResolvedAt(CoreMetrics.currentTimestamp());
        ex.setResolvedBy("deferred-posting-sweep-job");
        daoProvider.daoFor(ErpFinPostingException.class).updateEntity(ex);
    }

    protected void incrementRetryAndRethrow(ErpFinPostingException ex, Exception e) {
        try {
            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        ex.setRetryCount((ex.getRetryCount() == null ? 0 : ex.getRetryCount()) + 1);
                        if (ex.getRetryCount() >= MAX_RETRY) {
                            ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING);
                        }
                        daoProvider.daoFor(ErpFinPostingException.class).updateEntity(ex);
                        session.flush();
                        return null;
                    }));
        } catch (Exception persistErr) {
            LOG.warn("erp-fin-deferred-posting-increment-retry-failed: exceptionId={}, reason={}",
                    ex.getId(), persistErr.getMessage());
        }
    }

    protected ErpFinBusinessType parseBusinessType(String name) {
        if (StringHelper.isBlank(name)) {
            return null;
        }
        try {
            return ErpFinBusinessType.valueOf(name);
        } catch (IllegalArgumentException e) {
            LOG.warn("erp-fin-deferred-posting-unknown-business-type: {}", name);
            return null;
        }
    }
}
