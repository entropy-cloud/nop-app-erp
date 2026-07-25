package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinExpenseClaim withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractWithdrawApprovalProcessor to activate the abstract base class; delegates to ErpFinExpenseClaimProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinExpenseClaimWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpFinExpenseClaim> {

    @Inject
    ErpFinExpenseClaimProcessor processor;

    @Override
    public ErpFinExpenseClaim withdrawApproval(String id, IServiceContext context) {
        return processor.withdrawApproval(id, context);
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
}
