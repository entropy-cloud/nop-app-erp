package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBadDebt reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpFinBadDebtProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBadDebtRejectProcessor extends AbstractRejectProcessor<ErpFinBadDebt> {

    @Inject
    ErpFinBadDebtProcessor processor;

    @Override
    public ErpFinBadDebt reject(String id, IServiceContext context) {
        ErpFinBadDebt debt = processor.requireBadDebt(Long.valueOf(id));
        processor.validateTransitionForReject(debt);
        processor.doReject(debt);
        return debt;
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
    protected void setApprovedBy(ErpFinBadDebt entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpFinBadDebt entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isRejected(ErpFinBadDebt entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpFinBadDebt entity) {
        return false;
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
