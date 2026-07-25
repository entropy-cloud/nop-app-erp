package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinEmployeeAdvance approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpFinEmployeeAdvanceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinEmployeeAdvanceApproveProcessor extends AbstractApproveProcessor<ErpFinEmployeeAdvance> {

    @Inject
    ErpFinEmployeeAdvanceProcessor processor;

    @Override
    public ErpFinEmployeeAdvance approve(String id, IServiceContext context) {
        return processor.approve(id, context);
    }

    @Override
    protected IEntityDao<ErpFinEmployeeAdvance> dao() {
        return daoProvider.daoFor(ErpFinEmployeeAdvance.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpFinEmployeeAdvance entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpFinEmployeeAdvance entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpFinEmployeeAdvance entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpFinEmployeeAdvance entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpFinEmployeeAdvance entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpFinEmployeeAdvance entity) {
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
