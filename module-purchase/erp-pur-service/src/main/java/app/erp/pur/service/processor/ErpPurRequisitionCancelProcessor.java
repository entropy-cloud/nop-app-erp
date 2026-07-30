package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurRequisition cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Runs the AbstractCancelProcessor skeleton; delegates domain-specific hooks to ErpPurRequisitionProcessor.
 */
public class ErpPurRequisitionCancelProcessor extends AbstractCancelProcessor<ErpPurRequisition> {

    @Inject
    ErpPurRequisitionProcessor processor;

    @Override
    protected IEntityDao<ErpPurRequisition> dao() {
        return daoProvider.daoFor(ErpPurRequisition.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                .param(ErpPurErrors.ARG_REQUISITION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurRequisition entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpPurRequisition entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurRequisition entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
