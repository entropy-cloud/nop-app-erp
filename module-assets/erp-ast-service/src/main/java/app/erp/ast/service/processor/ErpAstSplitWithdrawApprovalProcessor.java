package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstSplit withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → set UNSUBMITTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstSplitWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpAstSplit> {

    @Inject
    ErpAstSplitProcessor processor;

    @Override
    public ErpAstSplit withdrawApproval(String id, IServiceContext context) {
        ErpAstSplit split = processor.requireSplit(id, context);
        processor.validateNotCancelled(split, context);
        processor.validateTransitionForWithdraw(split, context);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        processor.splitDao().updateEntity(split);
        return split;
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
    protected boolean isCancelled(ErpAstSplit entity) {
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
}
