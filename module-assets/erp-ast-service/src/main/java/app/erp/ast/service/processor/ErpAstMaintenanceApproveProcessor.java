package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMaintenance approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpAstMaintenanceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstMaintenanceApproveProcessor extends AbstractApproveProcessor<ErpAstMaintenance> {

    @Inject
    ErpAstMaintenanceProcessor processor;

    @Override
    public ErpAstMaintenance approve(String id, IServiceContext context) {
        return processor.approve(Long.valueOf(id), context);
    }

    @Override
    protected IEntityDao<ErpAstMaintenance> dao() {
        return daoProvider.daoFor(ErpAstMaintenance.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstMaintenance entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpAstMaintenance entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpAstMaintenance entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpAstMaintenance entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isApproved(ErpAstMaintenance entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstMaintenance entity) {
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
