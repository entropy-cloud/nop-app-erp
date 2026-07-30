package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalDelivery reject per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractRejectProcessor skeleton; delegates domain-specific hooks to ErpSalDeliveryProcessor.
 * doReject override 仅设 REJECTED（对齐 facade 语义，纠正抽象骨架误设 approvedBy/approvedAt）。
 */
public class ErpSalDeliveryRejectProcessor extends AbstractRejectProcessor<ErpSalDelivery> {

    @Inject
    ErpSalDeliveryProcessor processor;

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
    protected String getApproveStatus(ErpSalDelivery entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalDelivery entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalDelivery entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalDelivery entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpSalDelivery entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpSalDelivery entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }

    @Override
    protected void doReject(ErpSalDelivery entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
