package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import app.erp.pur.service.posting.PurPaymentPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

public class ErpPurPaymentReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpPurPayment> {

    @Inject
    ErpPurPaymentProcessor processor;

    @Inject
    PurPaymentPostingDispatcher postingDispatcher;

    @Override
    public ErpPurPayment reverseApprove(String id, IServiceContext context) {
        ErpPurPayment payment = requireEntity(id);
        if (payment.isRejected()) {
            return payment;
        }
        processor.validateTransitionForReverseApprove(payment, context);
        if (Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = dao().getEntityById(id);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        processor.doReverseApprove(payment, context);
        return payment;
    }

    @Override
    protected IEntityDao<ErpPurPayment> dao() {
        return daoProvider.daoFor(ErpPurPayment.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_NOT_FOUND)
                .param(ErpPurErrors.ARG_PAYMENT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurPayment entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpPurPayment entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurPayment entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurPayment entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurPayment entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpPurPayment entity) {
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
