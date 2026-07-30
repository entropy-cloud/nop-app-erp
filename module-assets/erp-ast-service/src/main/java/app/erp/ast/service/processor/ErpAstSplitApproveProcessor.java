package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.dao.entity.ErpAstSplitLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.List;

/**
 * ErpAstSplit approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → idempotency → validateNotCancelled → validateTransition → validateSourceAsset → validateLines → executeApprove.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstSplitApproveProcessor extends AbstractApproveProcessor<ErpAstSplit> {

    @Inject
    ErpAstSplitProcessor processor;

    @Override
    public ErpAstSplit approve(String id, IServiceContext context) {
        ErpAstSplit split = processor.requireSplit(id, context);
        if (split.isApproved()) {
            return split;
        }
        processor.validateNotCancelled(split, context);
        processor.validateTransitionForApprove(split, context);
        ErpAstAsset source = processor.loadSourceAsset(split);
        processor.validateSourceAsset(split, source, context);
        List<ErpAstSplitLine> lines = processor.loadLines(split);
        processor.validateLines(split, source, context);
        return processor.executeApprove(id, split, source, lines, context);
    }

    @Override
    protected IEntityDao<ErpAstSplit> dao() {
        return daoProvider.daoFor(ErpAstSplit.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstSplit entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstSplit entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstSplit entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstSplit entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpAstSplit entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpAstSplit entity) {
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
