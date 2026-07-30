package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstValueAdjustment cancel per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateTransitionForCancel → set CANCELLED → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.cancel，不经 xbiz 委托链）。
 */
public class ErpAstValueAdjustmentCancelProcessor extends AbstractCancelProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    @Override
    public ErpAstValueAdjustment cancel(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = processor.requireAdjustment(id, context);
        processor.validateTransitionForCancel(adjustment, context);
        adjustment.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
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
    protected String getDocStatus(ErpAstValueAdjustment entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpAstValueAdjustment entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }
}
