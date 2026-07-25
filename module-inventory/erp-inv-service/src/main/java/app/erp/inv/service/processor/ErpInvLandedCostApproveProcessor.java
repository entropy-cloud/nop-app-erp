package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvLandedCost approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpInvLandedCostProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpInvLandedCostApproveProcessor extends AbstractApproveProcessor<ErpInvLandedCost> {

    @Inject
    ErpInvLandedCostProcessor processor;

    @Override
    public ErpInvLandedCost approve(String id, IServiceContext context) {
        return processor.approve(Long.valueOf(id), context);
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
    protected boolean isApproved(ErpInvLandedCost entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpInvLandedCost entity) {
        return false;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String approvedStatus() {
        return null;
    }
}
