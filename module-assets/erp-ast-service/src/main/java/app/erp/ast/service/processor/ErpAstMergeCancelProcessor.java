package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMerge cancel per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateTransitionForCancel → set CANCELLED → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.cancel，不经 xbiz 委托链）。
 */
public class ErpAstMergeCancelProcessor extends AbstractCancelProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge cancel(String id, IServiceContext context) {
        ErpAstMerge merge = processor.requireMerge(id, context);
        processor.validateTransitionForCancel(merge, context);
        merge.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
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
    protected String getDocStatus(ErpAstMerge entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpAstMerge entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }
}
