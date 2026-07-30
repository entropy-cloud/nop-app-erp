package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinEmployeeAdvance reverseApprove per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractReverseApproveProcessor to activate the abstract base class; delegates to ErpFinEmployeeAdvanceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinEmployeeAdvanceReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpFinEmployeeAdvance> {

    @Inject
    ErpFinEmployeeAdvanceProcessor processor;

    @Override
    public ErpFinEmployeeAdvance reverseApprove(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = processor.requireAdvance(id, context);
        if (advance.isRejected()) {
            return advance;
        }
        processor.validateTransitionForReverseApprove(advance, context);
        return processor.doReverseApprove(id, advance, context);
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
    protected boolean isRejected(ErpFinEmployeeAdvance entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }
}
