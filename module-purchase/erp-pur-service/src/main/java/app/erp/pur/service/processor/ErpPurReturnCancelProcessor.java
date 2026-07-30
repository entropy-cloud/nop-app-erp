package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReturn cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Overrides the public cancel method to replicate the facade flow (posting reversal + stock move reversal if approved + doc status),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReturnCancelProcessor extends AbstractCancelProcessor<ErpPurReturn> {

    @Inject
    ErpPurReturnProcessor processor;

    @Override
    public ErpPurReturn cancel(String id, IServiceContext context) {
        ErpPurReturn returnOrder = requireEntity(id);
        validateTransitionForCancel(returnOrder, context);
        if (returnOrder.isApproved()) {
            processor.ensureReversed(returnOrder, context);
            returnOrder = dao().getEntityById(id);
        }
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    protected IEntityDao<ErpPurReturn> dao() {
        return daoProvider.daoFor(ErpPurReturn.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RETURN_NOT_FOUND)
                .param(ErpPurErrors.ARG_RETURN_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReturn entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpPurReturn entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurReturn entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
