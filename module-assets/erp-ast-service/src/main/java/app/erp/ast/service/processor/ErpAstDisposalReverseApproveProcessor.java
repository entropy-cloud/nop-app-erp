package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstDisposal reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → idempotency → validateTransition → executeReverseApprove.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstDisposalReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpAstDisposal> {

    @Inject
    ErpAstDisposalProcessor processor;

    @Override
    public ErpAstDisposal reverseApprove(String id, IServiceContext context) {
        ErpAstDisposal disposal = processor.requireDisposal(id, context);
        if (disposal.isRejected()) {
            return disposal;
        }
        processor.validateTransitionForReverseApprove(disposal, context);
        return processor.executeReverseApprove(id, disposal, context);
    }

    @Override
    protected IEntityDao<ErpAstDisposal> dao() {
        return daoProvider.daoFor(ErpAstDisposal.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstDisposal entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstDisposal entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstDisposal entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstDisposal entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstDisposal entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpAstConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }
}
