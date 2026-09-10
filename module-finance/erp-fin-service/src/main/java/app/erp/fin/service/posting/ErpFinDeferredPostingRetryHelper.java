package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.txn.TransactionPropagation;
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

import java.util.LinkedHashMap;
import java.util.Map;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinPostingException）=同域实体批量聚合，批量读写，写路径经编排层 Facade 事务边界承接。
/**
 * 过账异常重试帮助类（plan 2026-07-18-1600-1 Phase 3，自原 {@code DeferredPostingSweepJob} 抽取）。
 *
 * <p>供 nop-batch {@code deferred-posting-sweep.batch.xml} 的 processor 按记录调用：
 * 单条 {@link ErpFinPostingException} 经 REQUIRES_NEW 独立事务重建 {@link PostingEvent} 重试过账，
 * 成功标记 RETRIED，失败递增 retryCount。单条失败隔离不阻断 batch 继续处理其他记录（由 batch skipPolicy 兜底）。
 *
 * <p>O-16 补偿：REQUIRES_NEW 已提交但调用方在 posted=true 设置前失败的场景，
 * {@code voucherBiz.post()} 经引擎幂等命中返回既有凭证 id（F1.1 前{@code alreadyPosted()}命中返回 null），本类据此标记 RETRIED（补偿成功，两态皆成功）。
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
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    ErpFinPostedListenerRegistry postedListenerRegistry;
    @Inject
    ErpFinPostingExceptionRecorder exceptionRecorder;

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

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 重试单条异常记录。成功（含幂等命中）返回 true；失败返回 false（retryCount 已递增）。
     * 每条独立 REQUIRES_NEW 事务，单条失败不阻断其他记录。
     */
    public boolean retry(String exceptionId, IServiceContext ctx) {
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
                String voucherId = voucherBiz.post(event, ctx);
                LOG.debug("erp-fin-deferred-posting-retry-post: exceptionId={}, billHeadCode={}, voucherId={}",
                        ex.getId(), ex.getBillHeadCode(), voucherId);
                // F2.1（P1-CK-fin-001）：调用方不在场通道——凭证落账成功（含 F1.1 幂等命中返回既有 id）
                // 后派发 posted 事件，域监听者回写源单 posted（悬挂闭环）。失败落工作台且 eventData 透传
                // （下轮 sweep 幂等命中再派发 → 自愈），原异常记录仍 markRetried（凭证法律效力不回滚）。
                if (voucherId != null) {
                    dispatchPostedEvent(ex, event, voucherId, ctx);
                }
            }
        }
    }

    /** F2.1：派发 posted 事件；监听者失败经 recorder 落工作台（eventData 透传自愈）。 */
    protected void dispatchPostedEvent(ErpFinPostingException ex, PostingEvent event, String voucherId,
                                       IServiceContext ctx) {
        VoucherPostedEvent posted = new VoucherPostedEvent();
        posted.setVoucherId(voucherId);
        posted.setBillHeadCode(event.getBillHeadCode());
        posted.setBusinessType(ex.getBusinessType());
        posted.setBillType(ex.getBusinessType());
        posted.setTraceId(ex.getTraceId());
        java.util.List<ErpFinPostedListenerRegistry.ListenerFailure> failures =
                postedListenerRegistry.dispatch(posted, ctx);
        for (ErpFinPostedListenerRegistry.ListenerFailure failure : failures) {
            exceptionRecorder.record(ex.getTraceId(), ex.getBillHeadCode(), ex.getBusinessType(),
                    ex.getPostingType(),
                    ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED.getErrorCode(),
                    "posted-listener " + failure.getListenerName() + " failed: " + failure.getErrorMessage(),
                    ErpFinConstants.FAILED_STAGE_NOTIFY_POSTED_LISTENER,
                    ex.getVoucherDate(), ex.getOrgId(), ex.getAcctSchemaId(),
                    ex.getCurrencyId(), ex.getExchangeRate(), ex.getEventData());
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
        boolean escalatedToManual = false;
        try {
            escalatedToManual = transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        // 在 REQUIRES_NEW session 内重新加载受管实体，避免外层（已回滚/挂起）session 的游离实体更新丢失。
                        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
                        ErpFinPostingException managed = dao.getEntityById(ex.getId());
                        if (managed == null) {
                            return false;
                        }
                        int newCount = (managed.getRetryCount() == null ? 0 : managed.getRetryCount()) + 1;
                        managed.setRetryCount(newCount);
                        // G2 永久性失败：retryCount≥MAX_RETRY 升级 MANUAL 终态（非 RETRYING 死状态——
                        // sweep loader filter status=PENDING，RETRYING 永不被重新选中；MANUAL 标记需人工处置）。
                        if (newCount >= MAX_RETRY) {
                            managed.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL);
                        }
                        dao.updateEntity(managed);
                        session.flush();
                        return newCount >= MAX_RETRY;
                    }));
        } catch (Exception persistErr) {
            LOG.warn("erp-fin-deferred-posting-increment-retry-failed: exceptionId={}, reason={}",
                    ex.getId(), persistErr.getMessage());
        }
        // 升级 MANUAL 后派发告警（G2 错误传播分级策略；独立 try 降级不阻断主异常传播）。
        if (escalatedToManual) {
            dispatchMaxRetryAlert(ex, e);
        }
    }

    /**
     * MAX_RETRY 耗尽升级 MANUAL 时派发告警（G2 永久性失败处置；plan 2026-07-30-0341-2 P1-MA4-001）。
     * 通知失败降级（warn）不阻断主异常传播。
     */
    protected void dispatchMaxRetryAlert(ErpFinPostingException ex, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("exceptionId", ex.getId());
        ctx.put("billHeadCode", ex.getBillHeadCode());
        ctx.put("businessType", ex.getBusinessType());
        ctx.put("errorCode", ex.getErrorCode());
        ctx.put("errorMessage", cause != null ? truncate(cause.getMessage(), 200) : null);
        ctx.put("postingNo", ex.getBillHeadCode());
        IServiceContext serviceCtx = serviceContext();
        try {
            notificationBiz.notify(ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("erp-fin-deferred-posting-max-retry-alert-failed: exceptionId={}, reason={}",
                    ex.getId(), notifyErr.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
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

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
