package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpMfgSubcontractOrder reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpMfgSubcontractOrderProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpMfgSubcontractOrderRejectProcessor extends AbstractRejectProcessor<ErpMfgSubcontractOrder> {

    @Inject
    ErpMfgSubcontractOrderProcessor processor;

    @Override
    public ErpMfgSubcontractOrder reject(String id, IServiceContext context) {
        return processor.reject(id, context);
    }

    @Override
    protected IEntityDao<ErpMfgSubcontractOrder> dao() {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpMfgSubcontractOrder entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpMfgSubcontractOrder entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpMfgSubcontractOrder entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpMfgSubcontractOrder entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isRejected(ErpMfgSubcontractOrder entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpMfgSubcontractOrder entity) {
        return false;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String rejectedStatus() {
        return null;
    }
}
