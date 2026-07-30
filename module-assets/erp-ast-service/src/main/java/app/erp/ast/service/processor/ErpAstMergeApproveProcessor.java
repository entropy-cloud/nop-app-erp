package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.List;

/**
 * ErpAstMerge approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → idempotency → validateNotCancelled → validateTransition → validateSources → executeApprove.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstMergeApproveProcessor extends AbstractApproveProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge approve(String id, IServiceContext context) {
        ErpAstMerge merge = processor.requireMerge(id, context);
        if (merge.isApproved()) {
            return merge;
        }
        processor.validateNotCancelled(merge, context);
        processor.validateTransitionForApprove(merge, context);
        List<ErpAstMergeLine> lines = processor.loadLines(merge);
        List<ErpAstAsset> sources = processor.loadSources(lines);
        processor.validateSources(merge, lines, sources, context);
        return processor.executeApprove(id, merge, lines, sources, context);
    }

    @Override
    protected IEntityDao<ErpAstMerge> dao() {
        return daoProvider.daoFor(ErpAstMerge.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstMerge entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstMerge entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstMerge entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstMerge entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpAstMerge entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpAstMerge entity) {
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
