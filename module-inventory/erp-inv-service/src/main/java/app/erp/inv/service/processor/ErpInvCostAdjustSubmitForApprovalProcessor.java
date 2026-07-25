package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvCostAdjust submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpInvCostAdjustProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpInvCostAdjustSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpInvCostAdjust> {

    @Inject
    ErpInvCostAdjustProcessor processor;

    public ErpInvCostAdjustSubmitForApprovalProcessor() {
        super("ErpInvCostAdjust");
    }

    @Override
    public ErpInvCostAdjust submitForApproval(String id, IServiceContext context) {
        return processor.submitForApproval(id, context);
    }

    @Override
    protected IEntityDao<ErpInvCostAdjust> dao() {
        return daoProvider.daoFor(ErpInvCostAdjust.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpInvCostAdjust entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpInvCostAdjust entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpInvCostAdjust entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpInvConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpInvConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpInvConstants.APPROVE_STATUS_REJECTED;
    }
}
