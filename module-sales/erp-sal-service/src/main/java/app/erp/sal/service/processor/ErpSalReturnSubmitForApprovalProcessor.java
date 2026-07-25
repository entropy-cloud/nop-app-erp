package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReturn submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpSalReturnProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpSalReturnSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpSalReturn> {

    @Inject
    ErpSalReturnProcessor processor;

    public ErpSalReturnSubmitForApprovalProcessor() {
        super("ErpSalReturn");
    }

    @Override
    public ErpSalReturn submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpSalReturn> dao() {
        return daoProvider.daoFor(ErpSalReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpSalReturn entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpSalReturn entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpSalReturn entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpSalConstants.APPROVE_STATUS_REJECTED;
    }
}
