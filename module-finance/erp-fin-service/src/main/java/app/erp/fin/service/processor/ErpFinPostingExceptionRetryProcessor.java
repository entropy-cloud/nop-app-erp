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

/**
 * ErpFinPostingException retry per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含重试编排：翻 RETRYING + 记重试次数 + 经 {@link IErpFinVoucherBiz} 独立事务重新触发过账。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinPostingExceptionRetryProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    public ErpFinPostingException retry(String exceptionId, IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        // 翻 RETRYING 并记重试次数；重新触发过账（独立事务，失败回滚不污染本事务）。
        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING);
        entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
        entity.setResolvedBy(currentUserId());
        entity.setResolvedAt(CoreMetrics.currentTimestamp());
        daoProvider().updateEntity(entity);

        if (!ErpFinConstants.POSTING_TYPE_REVERSAL.equals(entity.getPostingType())) {
            // 正向过账重试：从 eventData 重建 PostingEvent 重新过账。
            PostingEvent event = rebuildEvent(entity);
            String voucherId = voucherBiz.post(event, context);
            if (voucherId != null) {
                entity.setVoucherId(voucherId);
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            } else {
                // 幂等命中（源单已过账）也算重试成功。
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            }
        } else {
            // 红冲重试：按回链重新红冲。
            ErpFinBusinessType businessType = parseBusinessType(entity.getBusinessType());
            String voucherId = voucherBiz.reverse(entity.getBillHeadCode(), businessType, context);
            if (voucherId != null) {
                entity.setVoucherId(voucherId);
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            }
        }
        daoProvider().updateEntity(entity);
        return entity;
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
        event.setExchangeRate(entity.getExchangeRate() != null ? entity.getExchangeRate() : BigDecimal.ONE);
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
            return null;
        }
    }
}
