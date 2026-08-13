package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalInvoiceApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalInvoice approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.24)。
 * approve 触发 AR 发票过账 + applyPosted + commitment-release-on-invoice-approve hook（facade approve 流程），
 * 需 custom public override（过账后实体引用变更）。固定来源态/目标态判断委托
 * {@link ErpSalInvoiceApprovalStateMachine}；过账编排/commitment-release/SoD 保留原位。
 */
public class ErpSalInvoiceApproveProcessor extends AbstractApproveProcessor<ErpSalInvoice> {

    @Inject
    ErpSalInvoiceProcessor processor;

    @Inject
    ErpSalInvoiceApprovalStateMachine stateMachine;

    @Override
    public ErpSalInvoice approve(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireEntity(id);
        if (isApproved(invoice)) {
            return invoice;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(invoice), currentUserId(), sodErrorCode());
        processor.validateNotCancelled(invoice, context);
        validateTransitionForApprove(invoice, context);
        processor.validateBusinessRulesForApprove(invoice, context);

        boolean posted = processor.doPosting(invoice, context);
        invoice = dao().getEntityById(id);
        setApproveStatus(invoice, approvedStatus());
        setApprovedBy(invoice, currentUserId());
        setApprovedAt(invoice, now());
        if (posted) {
            invoice.setPosted(true);
            invoice.setPostedAt(now());
            invoice.setPostedBy(currentUserId());
        }
        dao().updateEntity(invoice);

        processor.runCommitmentReleaseOnInvoiceApproveHook(invoice, context);
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
    protected void validateTransitionForApprove(ErpSalInvoice entity, IServiceContext context) {
        try {
            stateMachine.assertCanApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
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
    protected boolean isApproved(ErpSalInvoice entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpSalInvoice entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR;
    }
}
