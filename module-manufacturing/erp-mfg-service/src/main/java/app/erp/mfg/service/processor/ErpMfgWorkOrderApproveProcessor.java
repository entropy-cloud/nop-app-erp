package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpMfgWorkOrder approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpMfgWorkOrderProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpMfgWorkOrderApproveProcessor extends AbstractApproveProcessor<ErpMfgWorkOrder> {

    @Inject
    ErpMfgWorkOrderProcessor processor;

    @Override
    public ErpMfgWorkOrder approve(String id, IServiceContext context) {
        return processor.approve(id, context);
    }

    @Override
    protected IEntityDao<ErpMfgWorkOrder> dao() {
        return daoProvider.daoFor(ErpMfgWorkOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpMfgWorkOrder entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpMfgWorkOrder entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpMfgWorkOrder entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpMfgWorkOrder entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isApproved(ErpMfgWorkOrder entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpMfgWorkOrder entity) {
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
