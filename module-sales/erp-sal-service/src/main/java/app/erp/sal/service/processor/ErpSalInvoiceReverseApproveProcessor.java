package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalInvoicePostingDispatcher;
import app.erp.sal.service.statemachine.ErpSalInvoiceApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalInvoice reverseApprove per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.24)。
 *
 * <p>运行 {@link AbstractReverseApproveProcessor} 骨架；固定来源态判断委托
 * {@link ErpSalInvoiceApprovalStateMachine}。reverseApprove 在已过账时红冲 AR 发票过账（postingDispatcher.reverse）
 * 后 reload 设 REJECTED + 清空审计字段，需 custom public override（红冲后实体引用变更）。
 * reverseApprove 目标态=REJECTED 委托 Bean。
 */
public class ErpSalInvoiceReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpSalInvoice> {

    @Inject
    ErpSalInvoiceProcessor processor;

    @Inject
    SalInvoicePostingDispatcher postingDispatcher;

    @Inject
    ErpSalInvoiceApprovalStateMachine stateMachine;

    @Override
    public ErpSalInvoice reverseApprove(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireEntity(id);
        if (isRejected(invoice)) {
            return invoice;
        }
        validateTransitionForReverseApprove(invoice, context);
        if (Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = dao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        setApproveStatus(invoice, stateMachine.reverseApproveTargetStatus());
        setApprovedBy(invoice, null);
        setApprovedAt(invoice, null);
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    protected IEntityDao<ErpSalInvoice> dao() {
        return daoProvider.daoFor(ErpSalInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_INVOICE_NOT_FOUND)
                .param(ErpSalErrors.ARG_INVOICE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalInvoice entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForReverseApprove(ErpSalInvoice entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
    }

    @Override
    protected String getApproveStatus(ErpSalInvoice entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalInvoice entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalInvoice entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpSalInvoice entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
