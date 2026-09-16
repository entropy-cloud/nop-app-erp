package app.erp.fin.service.processor;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import app.erp.fin.service.posting.ErpFinPostingExceptionRecorder;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinPostingException）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpFinPostingException retry per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含重试编排：翻 RETRYING + 记重试次数 + 经 {@link IErpFinVoucherBiz} 独立事务重新触发过账。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinPostingExceptionRetryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinPostingExceptionRetryProcessor.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    app.erp.fin.service.posting.ErpFinPostedListenerRegistry postedListenerRegistry;
    @Inject
    app.erp.fin.service.posting.ErpFinPostingExceptionRecorder exceptionRecorder;

    public ErpFinPostingException retry(String exceptionId, IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        // P2-CK-fin-014（镜像 sweep 语义，杜绝 RETRYING 死状态）：外层不再翻转 RETRYING/递增计数
        // （原实现在外层 @BizMutation 事务内，post 失败一并回滚 → 计数不增、永不升级 MANUAL，且
        // 引擎 recordPostFailure 每次新增 PENDING 扩大同单多记录）。失败路径经
        // {@link #incrementRetryIndependent} 独立事务递增（REQUIRES_NEW 重载受管实体，MAX_RETRY
        // 升级 MANUAL）；成功路径重载实体后置 RETRIED（防独立事务递增后 version 冲突）。
        // 并发双击无状态互锁——与 sweep 同型，引擎 alreadyPosted 幂等兜底（P0-MA2-018 deferred 家族）。
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);

        try {
            if (!ErpFinConstants.POSTING_TYPE_REVERSAL.equals(entity.getPostingType())) {
                // 正向过账重试：从 eventData 重建 PostingEvent 重新过账。
                PostingEvent event = rebuildEvent(entity);
                String voucherId = voucherBiz.post(event, context);
                entity = reloadManaged(entity.getId());
                if (entity == null) {
                    return null;
                }
                // P2-CK-fin-014：成功路径计数在主事务持久（每次尝试计数一次；失败路径由 Recorder
                // 合并递增，两路径均不丢失尝试次数）。
                entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
                entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
                entity.setResolvedBy(currentUserId());
                entity.setResolvedAt(CoreMetrics.currentTimestamp());
                if (voucherId != null) {
                    entity.setVoucherId(voucherId);
                    entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
                    // F2.1（P1-CK-fin-001）：调用方不在场通道——凭证落账后派发 posted 事件
                    //（域监听者回写源单 posted；失败落工作台 eventData 透传自愈，镜像 sweep doRetry）。
                    dispatchPostedEvent(entity, event, voucherId, context);
                } else {
                    // F1.1 后引擎幂等命中返回既有凭证 id（非 null）——null 仅无凭证可派发，不派发（与 doRetry 同语义）。
                    entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
                }
            } else {
                // 红冲重试：按回链重新红冲。
                ErpFinBusinessType businessType = parseBusinessType(entity.getBusinessType());
                String voucherId = voucherBiz.reverse(entity.getBillHeadCode(), businessType, context);
                entity = reloadManaged(entity.getId());
                if (entity == null) {
                    return null;
                }
                if (voucherId != null) {
                    entity.setVoucherId(voucherId);
                    entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
                    entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
                    entity.setResolvedBy(currentUserId());
                    entity.setResolvedAt(CoreMetrics.currentTimestamp());
                }
            }
        } catch (RuntimeException e) {
            // 失败：引擎 recordPostFailure → Recorder 合并递增已持久化计数/刷新证据（保持 PENDING；
            // MAX_RETRY 升级 MANUAL），本处仅重抛原异常（外层 @BizMutation 回滚无害——失败路径
            // 无外层未提交写入）。与 sweep incrementRetryAndRethrow 语义对齐且不双重递增。
            throw e;
        }
        daoProvider().updateEntity(entity);
        return entity;
    }

    /** 独立事务重载受管实体（成功路径防独立递增后 version 冲突；实体已不存在返回 null）。 */
    private ErpFinPostingException reloadManaged(String exceptionId) {
        return daoProvider().getEntityById(exceptionId);
    }


    protected IEntityDao<ErpFinPostingException> daoProvider() {
        return daoProvider.daoFor(ErpFinPostingException.class);
    }

    protected ErpFinPostingException requirePending(String exceptionId) {
        IEntityDao<ErpFinPostingException> dao = daoProvider();
        ErpFinPostingException entity = dao.getEntityById(exceptionId);
        if (entity == null) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId);
        }
        if (!Objects.equals(entity.getStatus(), ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING)) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_NOT_PENDING)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId)
                    .param(ErpFinPostingErrors.ARG_CURRENT_STATUS, entity.getStatus());
        }
        return entity;
    }

    /** 从异常记录重建 PostingEvent（重试用）。 */
    protected PostingEvent rebuildEvent(ErpFinPostingException entity) {
        PostingEvent event = new PostingEvent();
        event.setTraceId(entity.getTraceId());
        event.setBillHeadCode(entity.getBillHeadCode());
        event.setBusinessType(parseBusinessType(entity.getBusinessType()));
        event.setVoucherDate(entity.getVoucherDate());
        event.setOrgId(entity.getOrgId());
        event.setAcctSchemaId(entity.getAcctSchemaId());
        event.setCurrencyId(entity.getCurrencyId());
        // P2-CK-fin-013：缺失汇率透传 null（与 sweep RetryHelper#rebuildEvent L157 对齐），交引擎
        // guardExchangeRate 判定非本位币缺汇率抛 ERR_EXCHANGE_RATE_REQUIRED——修复手动通道预置
        // rate=1 击穿 RC-R1.42 外币汇率守卫、与 sweep 行为分裂。
        event.setExchangeRate(entity.getExchangeRate());
        Map<String, Object> billData = ErpFinPostingExceptionRecorder.deserializeEventData(entity.getEventData());
        if (billData == null) {
            billData = new LinkedHashMap<>();
        }
        event.setBillData(billData);
        return event;
    }

    protected ErpFinBusinessType parseBusinessType(String name) {
        if (name == null) {
            return null;
        }
        return ErpFinBusinessType.valueOf(name);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            LOG.warn("currentUserId resolution failed (degraded): {}", e.getMessage());            return null;
        }
    }

    /** F2.1：派发 posted 事件；监听者失败经 recorder 落工作台（eventData 透传自愈），不阻断 RETRIED。 */
    protected void dispatchPostedEvent(ErpFinPostingException entity, PostingEvent event, String voucherId,
                                       IServiceContext context) {
        app.erp.fin.service.posting.VoucherPostedEvent posted = new app.erp.fin.service.posting.VoucherPostedEvent();
        posted.setVoucherId(voucherId);
        posted.setBillHeadCode(event.getBillHeadCode());
        posted.setBusinessType(entity.getBusinessType());
        posted.setBillType(entity.getBusinessType());
        posted.setTraceId(entity.getTraceId());
        java.util.List<app.erp.fin.service.posting.ErpFinPostedListenerRegistry.ListenerFailure> failures =
                postedListenerRegistry.dispatch(posted, context);
        for (app.erp.fin.service.posting.ErpFinPostedListenerRegistry.ListenerFailure failure : failures) {
            exceptionRecorder.record(entity.getTraceId(), entity.getBillHeadCode(), entity.getBusinessType(),
                    entity.getPostingType(),
                    app.erp.fin.service.posting.ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED.getErrorCode(),
                    "posted-listener " + failure.getListenerName() + " failed: " + failure.getErrorMessage(),
                    ErpFinConstants.FAILED_STAGE_NOTIFY_POSTED_LISTENER,
                    entity.getVoucherDate(), entity.getOrgId(), entity.getAcctSchemaId(),
                    entity.getCurrencyId(), entity.getExchangeRate(), entity.getEventData());
        }
    }
}