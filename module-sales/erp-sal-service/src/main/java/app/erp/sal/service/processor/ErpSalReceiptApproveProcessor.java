package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.statemachine.ErpSalReceiptApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalReceipt approve per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.26)。
 * approve 触发收款过账（facade doPosting）后 reload 设 APPROVED + applyPosted，需 custom public override
 * （过账后实体引用变更）。固定来源态/目标态判断委托 {@link ErpSalReceiptApprovalStateMachine}；
 * 过账编排/核销/SoD 保留原位。
 */
public class ErpSalReceiptApproveProcessor extends AbstractApproveProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Inject
    ErpSalReceiptApprovalStateMachine stateMachine;

    @Override
    public ErpSalReceipt approve(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireEntity(id);
        if (isApproved(receipt)) {
            return receipt;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(receipt), currentUserId(), sodErrorCode());
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
    protected void validateTransitionForApprove(ErpSalReceipt entity, IServiceContext context) {
        try {
            stateMachine.assertCanApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
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
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR;
    }
}
