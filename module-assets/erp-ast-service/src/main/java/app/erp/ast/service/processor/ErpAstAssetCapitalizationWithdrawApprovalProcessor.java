package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstAssetCapitalization withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → set UNSUBMITTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstAssetCapitalizationWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpAstAssetCapitalization> {

    @Inject
    ErpAstAssetCapitalizationProcessor processor;

    @Override
    public ErpAstAssetCapitalization withdrawApproval(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = processor.requireCap(id, context);
        processor.validateNotCancelled(cap, context);
        processor.validateTransitionForWithdraw(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        processor.capDao().updateEntity(cap);
        return cap;
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
    protected boolean isCancelled(ErpAstAssetCapitalization entity) {
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
