package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurOrder cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Runs the AbstractCancelProcessor skeleton; delegates domain-specific hooks to ErpPurOrderProcessor.
 */
public class ErpPurOrderCancelProcessor extends AbstractCancelProcessor<ErpPurOrder> {

    @Inject
    ErpPurOrderProcessor processor;

    @Override
    protected IEntityDao<ErpPurOrder> dao() {
        return daoProvider.daoFor(ErpPurOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpPurErrors.ARG_ORDER_CODE, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurOrder entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void beforeCancel(ErpPurOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected String getDocStatus(ErpPurOrder entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurOrder entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
