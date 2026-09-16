package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalReceiptPostingDispatcher;
import app.erp.sal.service.statemachine.ErpSalReceiptApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSalReceipt）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpSalReceipt reverseApprove per-mutation Processor (plan 2026-07-30-1433-2 R5.2；审批轴 Bean 接线 plan 2026-08-13-1950-2 M4.26)。
 *
 * <p>运行 {@link AbstractReverseApproveProcessor} 骨架；固定来源态判断委托
 * {@link ErpSalReceiptApprovalStateMachine}。reverseApprove 在已过账时红冲收款过账（postingDispatcher.reverse）
 * 后 reload 设 REJECTED + 清空审计字段，需 custom public override（红冲后实体引用变更）。
 * reverseApprove 目标态=REJECTED 委托 Bean。
 */
public class ErpSalReceiptReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Inject
    SalReceiptPostingDispatcher postingDispatcher;

    @Inject
    ErpSalReceiptApprovalStateMachine stateMachine;

    @Inject
    app.erp.sal.service.entity.ReceiptSettler receiptSettler;

    @Override
    public ErpSalReceipt reverseApprove(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireEntity(id);
        if (isRejected(receipt)) {
            return receipt;
        }
        validateTransitionForReverseApprove(receipt, context);
        // P2-CK-sal-012：反审核前反向核销全部核销行（发票 receivedStatus 随 recompute 回落）。
        receiptSettler.reverseAllSettlements(receipt);
        if (Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = dao().getEntityById(id);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        setApproveStatus(receipt, stateMachine.reverseApproveTargetStatus());
        setApprovedBy(receipt, null);
        setApprovedAt(receipt, null);
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
    protected void validateTransitionForReverseApprove(ErpSalReceipt entity, IServiceContext context) {
        try {
            stateMachine.assertCanReverseApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw e.param(ErpSalErrors.ARG_RECEIPT_CODE, entity.getCode());
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
    protected boolean isRejected(ErpSalReceipt entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpSalConstants.APPROVE_STATUS_SUBMITTED;
    }
}
