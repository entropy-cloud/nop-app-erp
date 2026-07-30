package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReceipt reject per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * Runs the AbstractRejectProcessor skeleton; delegates domain-specific hooks to ErpSalReceiptProcessor.
 * doReject override 仅设 REJECTED（对齐 facade 语义，纠正抽象骨架误设 approvedBy/approvedAt）。
 */
public class ErpSalReceiptRejectProcessor extends AbstractRejectProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Override
    protected IEntityDao<ErpSalReceipt> dao() {
        return daoProvider.daoFor(ErpSalReceipt.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_NOT_FOUND)
                .param(ErpSalErrors.ARG_RECEIPT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalReceipt entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpSalReceipt entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpSalReceipt entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalReceipt entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalReceipt entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalReceipt entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpSalReceipt entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpSalReceipt entity) {
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
    protected void doReject(ErpSalReceipt entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
    }
}
