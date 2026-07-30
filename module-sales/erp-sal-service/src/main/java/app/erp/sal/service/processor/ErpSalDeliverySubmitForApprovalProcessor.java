package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery submitForApproval per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractSubmitForApprovalProcessor skeleton; delegates domain-specific hooks to ErpSalDeliveryProcessor.
 */
public class ErpSalDeliverySubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

    public ErpSalDeliverySubmitForApprovalProcessor() {
        super("ErpSalDelivery");
    }

    @Override
    protected IEntityDao<ErpSalDelivery> dao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                .param(ErpSalErrors.ARG_DELIVERY_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalDelivery entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalDelivery entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpSalDelivery entity, IServiceContext context) {
        processor.requireLinesNonEmpty(entity, context);
        processor.requireCustomerActive(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpSalDelivery entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalDelivery entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalDelivery entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }
}
