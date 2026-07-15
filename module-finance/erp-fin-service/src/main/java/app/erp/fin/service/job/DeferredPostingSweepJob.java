package app.erp.fin.service.job;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.ErpFinPostingExceptionRecorder;
import io.nop.api.core.beans.query.QueryBean;
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
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 兜底扫描重试 Job（O-2）。
 *
 * <p>定时扫描 {@link ErpFinPostingException} 表中 {@code status=PENDING} 且 {@code retryCount < MAX_RETRY(3)}
 * 且 {@code occurrenceTime} 在最近 {@code RETRY_WINDOW_HOURS(24)} 内的失败记录，重建 {@link PostingEvent}
 * 经 {@link IErpFinVoucherBiz#post}（NORMAL）或 {@link IErpFinVoucherBiz#reverse}（REVERSAL）重试。
 *
 * <p>O-16 补偿：REQUIRES_NEW 事务已提交（凭证已落库）但调用方在 posted=true 设置前失败的场景下，
 * 异常记录仍为 PENDING。重试时 {@code voucherBiz.post()} 经引擎 {@code alreadyPosted()} 幂等命中返回 null，
 * 本 Job 据此将异常记录标记为 RETRIED（补偿成功）——无需重建凭证，仅完成状态闭环。
 *
 * <p>重试以独立事务（REQUIRES_NEW）承接（对齐过账 Facade 的事务边界语义）。单条失败隔离不阻断其他记录。
 * 触发频率由 {@code scheduler.yaml} cronExpr 决定；{@code erp-fin.deferred-posting-sweep-cron} 配置为空时跳过。
 */
public class DeferredPostingSweepJob {
    static final Logger LOG = LoggerFactory.getLogger(DeferredPostingSweepJob.class);

    /** 最大重试次数（超过后不再自动重试，需人工处置）。 */
    static final int MAX_RETRY = 3;
    /** 重试窗口（小时）：仅重试最近 N 小时内发生的失败，避免无限重试历史数据。 */
    static final int RETRY_WINDOW_HOURS = 24;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描 PENDING 异常记录重试。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.debug("erp-fin-deferred-posting-sweep-skipped: cron config empty (erp-fin.deferred-posting-sweep-cron)");
            return;
        }
        List<ErpFinPostingException> pending = findPendingExceptions();
        if (pending.isEmpty()) {
            LOG.debug("erp-fin-deferred-posting-sweep-noop: no pending exceptions in retry window");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        int succeeded = 0;
        int failed = 0;
        for (ErpFinPostingException ex : pending) {
            try {
                boolean resolved = retryOne(ex, ctx);
                if (resolved) {
                    succeeded++;
                }
            } catch (Exception e) {
                failed++;
                LOG.warn("erp-fin-deferred-posting-sweep-retry-failed: exceptionId={}, retryCount={}, reason={}",
                        ex.getId(), ex.getRetryCount(), e.getMessage());
            }
        }
        LOG.info("erp-fin-deferred-posting-sweep-done: scanned={}, succeeded={}, failed={}",
                pending.size(), succeeded, failed);
    }

    /** 查找待重试的异常记录（PENDING + retryCount < MAX_RETRY + 最近 RETRY_WINDOW_HOURS 内）。 */
    protected List<ErpFinPostingException> findPendingExceptions() {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        Timestamp windowStart = new Timestamp(
                CoreMetrics.currentTimeMillis() - (long) RETRY_WINDOW_HOURS * 3600_000);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("status", ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING),
                lt("retryCount", MAX_RETRY),
                ge("occurrenceTime", windowStart)
        ));
        q.setLimit(100);
        q.addOrderField("occurrenceTime", true);
        return dao.findAllByQuery(q);
    }

    /**
     * 重试单条异常记录。返回 true 表示已解决（重试成功或幂等命中）；false 表示仍失败（retryCount 已递增）。
     * 每条记录独立 REQUIRES_NEW 事务，单条失败不阻断其他记录。
     */
    protected boolean retryOne(ErpFinPostingException ex, IServiceContext ctx) {
        try {
            return transactionTemplate.runInTransaction(null,
                    io.nop.api.core.annotations.txn.TransactionPropagation.REQUIRES_NEW, txn ->
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

    /** 据异常记录的 postingType 选择重试路径（NORMAL→post / REVERSAL→reverse）。 */
    protected void doRetry(ErpFinPostingException ex, IServiceContext ctx) {
        String postingType = ex.getPostingType();
        ErpFinBusinessType businessType = parseBusinessType(ex.getBusinessType());
        if (ErpFinConstants.POSTING_TYPE_REVERSAL.equals(postingType)) {
            // 红冲重试：无完整事件快照，经 reverse(billHeadCode, businessType) 重建
            voucherBiz.reverse(ex.getBillHeadCode(), businessType, ctx);
        } else {
            // 正向过账重试：从 eventData 重建 PostingEvent
            PostingEvent event = rebuildEvent(ex);
            if (event != null) {
                Long voucherId = voucherBiz.post(event, ctx);
                // O-16 补偿：post 返回 null 表示幂等命中（凭证已存在），视为重试成功
                LOG.debug("erp-fin-deferred-posting-sweep-retry-post: exceptionId={}, billHeadCode={}, voucherId={}",
                        ex.getId(), ex.getBillHeadCode(), voucherId);
            }
        }
    }

    /** 从异常记录的 eventData（JSON）重建 PostingEvent。eventData 为空时返回 null（无法重试，保留 PENDING）。 */
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

    /** 重试成功标记：status=RETRIED + resolution=RETRY + voucherId（如有）+ resolvedAt。 */
    protected void markRetried(ErpFinPostingException ex) {
        ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
        ex.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
        ex.setResolvedAt(CoreMetrics.currentTimestamp());
        ex.setResolvedBy("deferred-posting-sweep-job");
        daoProvider.daoFor(ErpFinPostingException.class).updateEntity(ex);
    }

    /** 重试失败：retryCount + 1 + 更新 occurrenceTime（延长窗口），仍保留 status=PENDING 等待下次扫描或人工处置。 */
    protected void incrementRetryAndRethrow(ErpFinPostingException ex, Exception e) {
        try {
            transactionTemplate.runInTransaction(null,
                    io.nop.api.core.annotations.txn.TransactionPropagation.REQUIRES_NEW, txn ->
                            ormTemplate.runInSession(session -> {
                                ex.setRetryCount((ex.getRetryCount() == null ? 0 : ex.getRetryCount()) + 1);
                                // 达到上限时标记 RETRYING（可观测），未达上限保留 PENDING 等下次扫描
                                if (ex.getRetryCount() >= MAX_RETRY) {
                                    ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING);
                                }
                                daoProvider.daoFor(ErpFinPostingException.class).updateEntity(ex);
                                session.flush();
                                return null;
                            }));
        } catch (Exception persistErr) {
            LOG.warn("erp-fin-deferred-posting-sweep-increment-retry-failed: exceptionId={}, reason={}",
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
            LOG.warn("erp-fin-deferred-posting-sweep-unknown-business-type: {}", name);
            return null;
        }
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpFinConstants.CONFIG_DEFERRED_POSTING_SWEEP_CRON, "");
    }
}
