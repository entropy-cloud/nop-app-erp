package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

public class ErpPurInvoiceWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Override
    protected IEntityDao<ErpPurInvoice> dao() {
        return daoProvider.daoFor(ErpPurInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_INVOICE_NOT_FOUND)
                .param(ErpPurErrors.ARG_INVOICE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurInvoice entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurInvoice entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected String getApproveStatus(ErpPurInvoice entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpPurInvoice entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }
}
