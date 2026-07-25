package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpPurReceiveProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReceiveCancelProcessor extends AbstractCancelProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Override
    public ErpPurReceive cancel(String id, IServiceContext context) {
        return processor.cancel(id, context);
    }

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getDocStatus(ErpPurReceive entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurReceive entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
