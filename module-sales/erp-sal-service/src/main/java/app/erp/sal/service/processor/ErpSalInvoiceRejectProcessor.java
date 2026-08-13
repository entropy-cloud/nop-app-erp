package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalInvoiceApprovalStateMachine;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalInvoice reject per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.24)。
 *
 * <p>运行 {@link AbstractRejectProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpSalInvoiceApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 * doReject override 仅设 REJECTED（对齐 facade 语义，纠正抽象骨架误设 approvedBy/approvedAt）。
 */
public class ErpSalInvoiceRejectProcessor extends AbstractRejectProcessor<ErpSalInvoice> {

    @Inject
    ErpSalInvoiceProcessor processor;

    @Inject
    ErpSalInvoiceApprovalStateMachine stateMachine;

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
    protected void validateNotCancelled(ErpSalInvoice entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForReject(ErpSalInvoice entity, IServiceContext context) {
        try {
            stateMachine.assertCanReject(getApproveStatus(entity));
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
    protected boolean isRejected(ErpSalInvoice entity) {
        return entity.isRejected();
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
    protected String rejectedStatus() {
        return stateMachine.rejectTargetStatus();
    }

    @Override
    protected void doReject(ErpSalInvoice entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
