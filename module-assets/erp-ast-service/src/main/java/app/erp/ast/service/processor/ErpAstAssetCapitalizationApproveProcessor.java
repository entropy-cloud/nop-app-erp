package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstAssetCapitalization approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → idempotency → validateNotCancelled → validateTransition → validateForApproval → executeApprove.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstAssetCapitalizationApproveProcessor extends AbstractApproveProcessor<ErpAstAssetCapitalization> {

    @Inject
    ErpAstAssetCapitalizationProcessor processor;

    @Override
    public ErpAstAssetCapitalization approve(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = processor.requireCap(id, context);
        if (cap.isApproved()) {
            return cap;
        }
        processor.validateNotCancelled(cap, context);
        processor.validateTransitionForApprove(cap, context);
        processor.validateForApproval(cap, context);
        return processor.executeApprove(id, cap, context);
    }

    @Override
    protected IEntityDao<ErpAstAssetCapitalization> dao() {
        return daoProvider.daoFor(ErpAstAssetCapitalization.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstAssetCapitalization entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstAssetCapitalization entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstAssetCapitalization entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstAssetCapitalization entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpAstAssetCapitalization entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpAstAssetCapitalization entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpAstConstants.APPROVE_STATUS_APPROVED;
    }
}
