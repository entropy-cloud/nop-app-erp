package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinPostingException）=同域实体批量聚合，批量读写，写路径经编排层 Facade 事务边界承接。
/**
 * ErpFinPostingException ignore per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含显式忽略编排：reason 必填守卫 + 翻 IGNORED + 放弃态告警派发（G2 错误传播分级）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinPostingExceptionIgnoreProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpFinPostingException ignore(String exceptionId, String resolutionNote, IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        if (resolutionNote == null || resolutionNote.trim().isEmpty()) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_IGNORE_REASON_REQUIRED)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId);
        }
        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_IGNORED);
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_IGNORE);
        entity.setResolutionNote(resolutionNote);
        entity.setResolvedBy(currentUserId());
        entity.setResolvedAt(CoreMetrics.currentTimestamp());
        daoProvider.daoFor(ErpFinPostingException.class).updateEntity(entity);
        // P1-MA2-032（G2 显式放弃态）：IGNORED 补告警——首次记录已有 dispatchNotify，
        // 此处补放弃态告警使运营感知「异常被显式忽略」决策（posting-log.md §错误传播分级策略 G2）。
        dispatchAbandonmentAlert(entity, resolutionNote);
        return entity;
    }

    /**
     * IGNORED 放弃态告警派发（G2 错误传播分级策略；plan 2026-07-30-0341-2 P1-MA2-032）。
     * 通知失败降级（warn）不阻断处置动作。
     */
    protected void dispatchAbandonmentAlert(ErpFinPostingException entity, String resolutionNote) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("exceptionId", entity.getId());
        ctx.put("billHeadCode", entity.getBillHeadCode());
        ctx.put("businessType", entity.getBusinessType());
        ctx.put("errorCode", entity.getErrorCode());
        ctx.put("errorMessage", entity.getErrorMessage());
        ctx.put("resolutionNote", resolutionNote);
        ctx.put("postingNo", entity.getBillHeadCode());
        try {
            notificationBiz.notify(ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION, ctx, serviceContext());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ErpFinPostingExceptionIgnoreProcessor.class)
                    .warn("erp-fin-posting-exception-ignored-alert-failed: exceptionId={}, reason={}",
                            entity.getId(), e.getMessage());
        }
    }

    protected ErpFinPostingException requirePending(String exceptionId) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
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

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            LOG.warn("currentUserId resolution failed (degraded): {}", e.getMessage());            return null;
        }
    }

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
