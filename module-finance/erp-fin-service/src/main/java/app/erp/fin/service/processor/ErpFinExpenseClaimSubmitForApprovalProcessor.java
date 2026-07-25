package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinExpenseClaim submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpFinExpenseClaimProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinExpenseClaimSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpFinExpenseClaim> {

    @Inject
    ErpFinExpenseClaimProcessor processor;

    public ErpFinExpenseClaimSubmitForApprovalProcessor() {
        super("ErpFinExpenseClaim");
    }

    @Override
    public ErpFinExpenseClaim submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpFinExpenseClaim> dao() {
        return daoProvider.daoFor(ErpFinExpenseClaim.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpFinExpenseClaim entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpFinExpenseClaim entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpFinExpenseClaim entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpFinConstants.APPROVE_STATUS_REJECTED;
    }
}
