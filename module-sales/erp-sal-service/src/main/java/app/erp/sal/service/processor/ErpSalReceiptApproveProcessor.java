package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReceipt approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2).
 * approve 触发收款过账（facade doPosting）后 reload 设 APPROVED + applyPosted，需 custom public override
 * （过账后实体引用变更）。对齐 R5.1 ErpPurReceiveApproveProcessor 模式 B。
 */
public class ErpSalReceiptApproveProcessor extends AbstractApproveProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Override
    public ErpSalReceipt approve(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireEntity(id);
        if (isApproved(receipt)) {
            return receipt;
        }
        processor.validateNotCancelled(receipt, context);
        validateTransitionForApprove(receipt, context);
        processor.validateBusinessRulesForApprove(receipt, context);

        boolean posted = processor.doPosting(receipt, context);
        receipt = dao().getEntityById(id);
        setApproveStatus(receipt, approvedStatus());
        setApprovedBy(receipt, currentUserId());
        setApprovedAt(receipt, now());
        if (posted) {
            receipt.setPosted(true);
            receipt.setPostedAt(now());
            receipt.setPostedBy(currentUserId());
        }
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    protected IEntityDao<ErpSalReceipt> dao() {
        return daoProvider.daoFor(ErpSalReceipt.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_NOT_FOUND)
                .param(ErpSalErrors.ARG_RECEIPT_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalReceipt entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpSalReceipt entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpSalConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpSalReceipt entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpSalReceipt entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpSalReceipt entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpSalReceipt entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpSalReceipt entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }
}
