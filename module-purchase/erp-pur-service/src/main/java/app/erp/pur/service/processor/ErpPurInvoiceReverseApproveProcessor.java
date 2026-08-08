package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import app.erp.pur.service.posting.PurInvoicePostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

public class ErpPurInvoiceReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Inject
    PurInvoicePostingDispatcher postingDispatcher;

    @Override
    public ErpPurInvoice reverseApprove(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireEntity(id);
        if (invoice.isRejected()) {
            return invoice;
        }
        processor.validateTransitionForReverseApprove(invoice, context);
        if (Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = dao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        processor.doReverseApprove(invoice, context);
        processor.runCommitmentRestoreOnInvoiceReverseHook(invoice, true, context);
        return invoice;
    }

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
    protected String getApproveStatus(ErpPurInvoice entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurInvoice entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurInvoice entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurInvoice entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurInvoice entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpPurConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }
}
