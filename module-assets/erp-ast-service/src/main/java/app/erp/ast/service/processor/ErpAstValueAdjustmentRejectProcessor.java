package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstValueAdjustment reject per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → set REJECTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstValueAdjustmentRejectProcessor extends AbstractRejectProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    @Override
    public ErpAstValueAdjustment reject(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = processor.requireAdjustment(id, context);
        processor.validateNotCancelled(adjustment, context);
        processor.validateTransitionForReject(adjustment, context);
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        processor.adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    @Override
    protected IEntityDao<ErpAstValueAdjustment> dao() {
        return daoProvider.daoFor(ErpAstValueAdjustment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstValueAdjustment entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstValueAdjustment entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstValueAdjustment entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstValueAdjustment entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstValueAdjustment entity) {
        return entity.isRejected();
    }

    @Override
    protected boolean isCancelled(ErpAstValueAdjustment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpAstConstants.APPROVE_STATUS_REJECTED;
    }
}
