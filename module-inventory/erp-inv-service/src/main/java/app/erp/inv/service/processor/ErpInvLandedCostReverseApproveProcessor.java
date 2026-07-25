package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvLandedCost reverseApprove per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractReverseApproveProcessor to activate the abstract base class; delegates to ErpInvLandedCostProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpInvLandedCostReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpInvLandedCost> {

    @Inject
    ErpInvLandedCostProcessor processor;

    @Override
    public ErpInvLandedCost reverseApprove(String id, IServiceContext context) {
        return processor.reverseApprove(Long.valueOf(id), context);
    }

    @Override
    protected IEntityDao<ErpInvLandedCost> dao() {
        return daoProvider.daoFor(ErpInvLandedCost.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpInvLandedCost entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpInvLandedCost entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpInvLandedCost entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpInvLandedCost entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isRejected(ErpInvLandedCost entity) {
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
