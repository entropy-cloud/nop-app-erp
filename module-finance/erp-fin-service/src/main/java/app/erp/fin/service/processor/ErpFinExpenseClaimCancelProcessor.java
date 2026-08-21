package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinExpenseClaim cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpFinExpenseClaimProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinExpenseClaimCancelProcessor extends AbstractCancelProcessor<ErpFinExpenseClaim> {

    @Inject
    ErpFinExpenseClaimProcessor processor;

    @Override
    public ErpFinExpenseClaim cancel(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = processor.requireClaim(id, context);
        processor.validateTransitionForCancel(claim, context);
        return processor.doCancel(id, claim, context);
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
    protected String getDocStatus(ErpFinExpenseClaim entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpFinExpenseClaim entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpFinConstants.DOC_STATUS_CANCELLED;
    }
}
