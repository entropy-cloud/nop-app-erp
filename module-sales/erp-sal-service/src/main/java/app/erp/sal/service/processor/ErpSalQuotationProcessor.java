package app.erp.sal.service.processor;

import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售报价单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 *
 * <p>跨实体：报价→订单转化经 {@link IErpSalOrderBiz}（createFromQuotation + existsActiveByQuotation 防重）。
 */
public class ErpSalQuotationProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSalOrderBiz orderBiz;

    public ErpSalQuotation submitForApproval(String id, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(id, context);
        validateNotCancelled(quotation, context);
        validateTransitionForSubmit(quotation, context);
        validateBusinessRulesForSubmit(quotation, context);
        doSubmit(quotation, context);
        return quotation;
    }

    public ErpSalQuotation withdrawApproval(String id, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(id, context);
        validateNotCancelled(quotation, context);
        validateTransitionForWithdraw(quotation, context);
        doWithdrawSubmit(quotation, context);
        return quotation;
    }

    public ErpSalQuotation approve(String id, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(id, context);
        if (isAlreadyApproved(quotation)) {
            return quotation;
        }
        validateNotCancelled(quotation, context);
        validateTransitionForApprove(quotation, context);
        doApprove(quotation, context);
        return quotation;
    }

    public ErpSalQuotation reject(String id, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(id, context);
        validateNotCancelled(quotation, context);
        validateTransitionForReject(quotation, context);
        doReject(quotation, context);
        return quotation;
    }

    public ErpSalQuotation reverseApprove(String id, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(id, context);
        if (isAlreadyRejected(quotation)) {
            return quotation;
        }
        validateTransitionForReverseApprove(quotation, context);
        doReverseApprove(quotation, context);
        return quotation;
    }

    public ErpSalQuotation cancel(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateTransitionForCancel(quotation, context);
        doCancel(quotation, context);
        return quotation;
    }

    public ErpSalQuotation confirmCustomerAccepted(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateNotCancelled(quotation, context);
        validateTransitionForConfirm(quotation, context);
        requireNotExpired(quotation, context);
        doConfirmCustomerAccepted(quotation, context);
        return quotation;
    }

    public ErpSalOrder convertToOrder(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateReadyForConvert(quotation, context);
        requireNotExpired(quotation, context);
        validateNotAlreadyConverted(quotation, context);
        ErpSalOrder order = createOrderFromQuotation(quotation, context);
        markQuotationAccepted(quotationId, context);
        return order;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalQuotation quotation, IServiceContext context) {
        validateNotCancelled(quotation, context);
        String status = quotation.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(quotation, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalQuotation quotation, IServiceContext context) {
        String status = quotation.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalQuotation quotation, IServiceContext context) {
        String status = quotation.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalQuotation quotation, IServiceContext context) {
        String status = quotation.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalQuotation quotation, IServiceContext context) {
        String status = quotation.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalQuotation quotation, IServiceContext context) {
        String docStatus = quotation.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
    }

    protected void validateTransitionForConfirm(ErpSalQuotation quotation, IServiceContext context) {
        String status = quotation.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
    }

    protected void validateReadyForConvert(ErpSalQuotation quotation, IServiceContext context) {
        validateNotCancelled(quotation, context);
        String status = quotation.getApproveStatus();
        boolean accepted = Boolean.TRUE.equals(quotation.getIsAccepted());
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED) || !accepted) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_READY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpSalErrors.ARG_IS_ACCEPTED, accepted);
        }
    }

    protected void validateNotAlreadyConverted(ErpSalQuotation quotation, IServiceContext context) {
        if (orderBiz.existsActiveByQuotation(quotation.getId(), context)) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_ALREADY_CONVERTED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }
    }

    protected void requireNotExpired(ErpSalQuotation quotation, IServiceContext context) {
        LocalDate validTo = quotation.getValidTo();
        if (validTo != null && validTo.isBefore(CoreMetrics.currentDate())) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_EXPIRED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_VALID_TO, validTo);
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalQuotation quotation, IServiceContext context) {
        requireLinesNonEmpty(quotation, context);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doWithdrawSubmit(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doApprove(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        quotation.setApprovedBy(currentUserId());
        quotation.setApprovedAt(CoreMetrics.currentTimestamp());
        quotationDao().updateEntity(quotation);
    }

    protected void doReject(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doReverseApprove(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        quotation.setApprovedBy(null);
        quotation.setApprovedAt(null);
        quotationDao().updateEntity(quotation);
    }

    protected void doCancel(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        quotationDao().updateEntity(quotation);
    }

    protected void doConfirmCustomerAccepted(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setIsAccepted(true);
        quotationDao().updateEntity(quotation);
    }

    protected ErpSalOrder createOrderFromQuotation(ErpSalQuotation quotation, IServiceContext context) {
        List<ErpSalQuotationLine> quotationLines = loadLines(quotation.getId());
        return orderBiz.createFromQuotation(quotation, quotationLines, context);
    }

    protected void markQuotationAccepted(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = quotationDao().getEntityById(quotationId);
        quotation.setIsAccepted(true);
        quotationDao().updateEntity(quotation);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalQuotation requireQuotation(String id, IServiceContext context) {
        ErpSalQuotation quotation = quotationDao().getEntityById(id);
        if (quotation == null) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, id);
        }
        return quotation;
    }

    protected void validateNotCancelled(ErpSalQuotation quotation, IServiceContext context) {
        String docStatus = quotation.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalQuotation quotation) {
        String status = quotation.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpSalQuotation quotation) {
        String status = quotation.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpSalQuotation quotation, IServiceContext context) {
        if (loadLines(quotation.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }
    }

    protected List<ErpSalQuotationLine> loadLines(Long quotationId) {
        IEntityDao<ErpSalQuotationLine> dao = daoProvider.daoFor(ErpSalQuotationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("quotationId", quotationId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalQuotation> quotationDao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpSalQuotation quotation, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalQuotation quotation, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
