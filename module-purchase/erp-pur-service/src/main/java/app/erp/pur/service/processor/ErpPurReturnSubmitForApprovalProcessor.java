package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReturn submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpPurReturnProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReturnSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    public ErpPurReturnSubmitForApprovalProcessor() {
        super("ErpPurReturn");
    }

    @Override
    public ErpPurReturn submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpPurReturn> dao() {
        return daoProvider.daoFor(ErpPurReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPurReturn entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpPurReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpPurReturn entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpPurConstants.APPROVE_STATUS_REJECTED;
    }
}
