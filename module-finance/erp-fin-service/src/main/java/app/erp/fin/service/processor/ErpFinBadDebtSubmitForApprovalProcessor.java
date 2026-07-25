package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBadDebt submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpFinBadDebtProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBadDebtSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpFinBadDebt> {

    @Inject
    ErpFinBadDebtProcessor processor;

    public ErpFinBadDebtSubmitForApprovalProcessor() {
        super("ErpFinBadDebt");
    }

    @Override
    public ErpFinBadDebt submitForApproval(String id, IServiceContext context) {
        return processor.submit(Long.valueOf(id), context);
    }

    @Override
    protected IEntityDao<ErpFinBadDebt> dao() {
        return daoProvider.daoFor(ErpFinBadDebt.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpFinBadDebt entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpFinBadDebt entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isCancelled(ErpFinBadDebt entity) {
        return false;
    }

    @Override
    protected String unsubmittedStatus() {
        return null;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String rejectedStatus() {
        return null;
    }
}
