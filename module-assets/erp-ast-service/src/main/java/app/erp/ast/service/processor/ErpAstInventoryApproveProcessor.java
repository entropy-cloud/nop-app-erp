package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstInventory approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpAstInventoryProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstInventoryApproveProcessor extends AbstractApproveProcessor<ErpAstInventory> {

    @Inject
    ErpAstInventoryProcessor processor;

    @Override
    public ErpAstInventory approve(String id, IServiceContext context) {
        return processor.approve(Long.valueOf(id), context);
    }

    @Override
    protected IEntityDao<ErpAstInventory> dao() {
        return daoProvider.daoFor(ErpAstInventory.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstInventory entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpAstInventory entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpAstInventory entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpAstInventory entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isApproved(ErpAstInventory entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstInventory entity) {
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
