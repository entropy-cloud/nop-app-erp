package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvCostAdjust withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractWithdrawApprovalProcessor to activate the abstract base class; delegates to ErpInvCostAdjustProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpInvCostAdjustWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpInvCostAdjust> {

    @Inject
    ErpInvCostAdjustProcessor processor;

    @Override
    public ErpInvCostAdjust withdrawApproval(String id, IServiceContext context) {
        return processor.withdrawApproval(id, context);
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
}
