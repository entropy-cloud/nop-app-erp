package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstSplit cancel per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateTransitionForCancel → set CANCELLED → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.cancel，不经 xbiz 委托链）。
 */
public class ErpAstSplitCancelProcessor extends AbstractCancelProcessor<ErpAstSplit> {

    @Inject
    ErpAstSplitProcessor processor;

    @Override
    public ErpAstSplit cancel(String id, IServiceContext context) {
        ErpAstSplit split = processor.requireSplit(id, context);
        processor.validateTransitionForCancel(split, context);
        split.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
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
    protected String getDocStatus(ErpAstSplit entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpAstSplit entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }
}
