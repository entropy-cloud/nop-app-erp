package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinExpenseClaim approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpFinExpenseClaimProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinExpenseClaimApproveProcessor extends AbstractApproveProcessor<ErpFinExpenseClaim> {

    @Inject
    ErpFinExpenseClaimProcessor processor;

    @Override
    public ErpFinExpenseClaim approve(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = processor.requireClaim(id, context);
        if (claim.isApproved()) {
            return claim;
        }
        processor.validateNotCancelled(claim, context);
        processor.validateTransitionForApprove(claim, context);
        processor.validateForApproval(claim, context);
        processor.runBudgetCheckHook(claim, context);
        return processor.doApprove(id, claim, context);
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
    protected void setApprovedBy(ErpFinExpenseClaim entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpFinExpenseClaim entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpFinExpenseClaim entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpFinExpenseClaim entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }
}
