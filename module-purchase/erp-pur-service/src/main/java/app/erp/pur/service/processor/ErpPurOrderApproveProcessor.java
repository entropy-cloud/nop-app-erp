package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurOrder approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Runs the AbstractApproveProcessor skeleton; delegates domain-specific hooks to ErpPurOrderProcessor.
 */
public class ErpPurOrderApproveProcessor extends AbstractApproveProcessor<ErpPurOrder> {

    @Inject
    ErpPurOrderProcessor processor;

    @Override
    protected IEntityDao<ErpPurOrder> dao() {
        return daoProvider.daoFor(ErpPurOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpPurErrors.ARG_ORDER_CODE, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurOrder entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurOrder entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateBusinessRules(ErpPurOrder entity, IServiceContext context) {
        processor.requireSupplierActive(entity, context);
        processor.runBudgetCheckHook(entity, context);
    }

    @Override
    protected void afterStateChange(ErpPurOrder entity, IServiceContext context) {
        processor.runCommitmentCommitHook(entity, context);
        processor.runIntercompanyApproveHook(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpPurOrder entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurOrder entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurOrder entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurOrder entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurOrder entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR;
    }
}
