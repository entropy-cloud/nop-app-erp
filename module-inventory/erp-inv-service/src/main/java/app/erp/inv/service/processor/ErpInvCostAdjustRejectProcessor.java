package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.ErpInvConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpInvCostAdjust reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpInvCostAdjustProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpInvCostAdjustRejectProcessor extends AbstractRejectProcessor<ErpInvCostAdjust> {

    @Inject
    ErpInvCostAdjustProcessor processor;

    @Override
    public ErpInvCostAdjust reject(String id, IServiceContext context) {
        return processor.reject(id, context);
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
    protected void setApprovedBy(ErpInvCostAdjust entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpInvCostAdjust entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpInvCostAdjust entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpInvCostAdjust entity) {
        return entity.isCancelled();
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
