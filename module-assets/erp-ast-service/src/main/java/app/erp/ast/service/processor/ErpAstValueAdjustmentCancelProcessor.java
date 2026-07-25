package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstValueAdjustment cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpAstValueAdjustmentProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpAstValueAdjustmentCancelProcessor extends AbstractCancelProcessor<ErpAstValueAdjustment> {

    @Inject
    ErpAstValueAdjustmentProcessor processor;

    @Override
    public ErpAstValueAdjustment cancel(String id, IServiceContext context) {
        return processor.cancel(Long.valueOf(id), context);
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
