package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurInvoiceApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurInvoice submitForApproval per-mutation Processor（审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.16，skeleton 路径）。
 *
 * <p>运行 {@link AbstractSubmitForApprovalProcessor} 骨架；固定来源态/目标态判断委托
 * {@link ErpPurInvoiceApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）。
 */
public class ErpPurInvoiceSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Inject
    ErpPurInvoiceApprovalStateMachine stateMachine;

    public ErpPurInvoiceSubmitForApprovalProcessor() {
        super("ErpPurInvoice");
    }

    @Override
    protected IEntityDao<ErpPurInvoice> dao() {
        return daoProvider.daoFor(ErpPurInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_INVOICE_NOT_FOUND)
                .param(ErpPurErrors.ARG_INVOICE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurInvoice entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurInvoice entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpPurInvoice entity, IServiceContext context) {
        processor.requireLinesNonEmpty(entity, context);
        processor.requireSupplierActive(entity, context);
    }

    @Override
    protected void validateTransitionForSubmit(ErpPurInvoice entity, IServiceContext context) {
        try {
            stateMachine.assertCanSubmit(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity),
                    ErpPurConstants.APPROVE_STATUS_UNSUBMITTED + " / " + ErpPurConstants.APPROVE_STATUS_REJECTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurInvoice entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpPurInvoice entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return ErpPurConstants.APPROVE_STATUS_REJECTED;
    }
}
