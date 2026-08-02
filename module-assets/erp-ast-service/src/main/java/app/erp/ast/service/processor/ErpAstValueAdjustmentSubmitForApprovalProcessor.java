package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstValueAdjustment submitForApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → validateForApproval
 * → auto-approve fast path（isApprovalRequired()=false 时 doAutoApprove）→ set SUBMITTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstValueAdjustmentSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    public ErpAstValueAdjustmentSubmitForApprovalProcessor() {
        super("ErpAstValueAdjustment");
    }

    @Override
    public ErpAstValueAdjustment submitForApproval(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = processor.requireAdjustment(id, context);
        processor.validateNotCancelled(adjustment, context);
        processor.validateTransitionForSubmit(adjustment, context);
        processor.validateForApproval(adjustment, context);
        if (!processor.isApprovalRequired()) {
            return processor.doAutoApprove(id, adjustment, context);
        }
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
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
    protected boolean isCancelled(ErpAstValueAdjustment entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
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
