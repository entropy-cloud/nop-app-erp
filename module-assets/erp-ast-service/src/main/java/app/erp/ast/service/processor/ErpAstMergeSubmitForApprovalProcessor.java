package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.List;

/**
 * ErpAstMerge submitForApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → validateSources → set SUBMITTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstMergeSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    public ErpAstMergeSubmitForApprovalProcessor() {
        super("ErpAstMerge");
    }

    @Override
    public ErpAstMerge submitForApproval(String id, IServiceContext context) {
        ErpAstMerge merge = processor.requireMerge(id, context);
        processor.validateNotCancelled(merge, context);
        processor.validateTransitionForSubmit(merge, context);
        List<ErpAstMergeLine> lines = processor.loadLines(merge);
        List<ErpAstAsset> sources = processor.loadSources(lines);
        processor.validateSources(merge, lines, sources, context);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        processor.mergeDao().updateEntity(merge);
        return merge;
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
    protected boolean isCancelled(ErpAstMerge entity) {
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
