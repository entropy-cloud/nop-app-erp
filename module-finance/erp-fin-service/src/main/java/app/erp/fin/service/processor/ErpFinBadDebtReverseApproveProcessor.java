package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBadDebt reverseApprove per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractReverseApproveProcessor to activate the abstract base class; delegates to ErpFinBadDebtProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBadDebtReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpFinBadDebt> {

    @Inject
    ErpFinBadDebtProcessor processor;

    @Override
    public ErpFinBadDebt reverseApprove(String id, IServiceContext context) {
        return processor.reverseApprove(Long.valueOf(id), context);
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
    protected String approvedStatus() {
        return null;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }
}
