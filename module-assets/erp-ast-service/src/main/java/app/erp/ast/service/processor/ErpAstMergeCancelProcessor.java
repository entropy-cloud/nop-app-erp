package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMerge cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpAstMergeProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstMergeCancelProcessor extends AbstractCancelProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge cancel(String id, IServiceContext context) {
        return processor.cancel(Long.valueOf(id), context);
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
        return null;
    }

    @Override
    protected void setDocStatus(ErpAstMerge entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected String cancelledDocStatus() {
        return null;
    }
}
