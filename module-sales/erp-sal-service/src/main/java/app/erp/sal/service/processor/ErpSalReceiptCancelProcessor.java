package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalReceiptPostingDispatcher;
import app.erp.sal.service.statemachine.ErpSalReceiptDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpSalReceipt cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source;
 * StateMachine 接线 plan 2026-08-13-0810-2 M4.25)。
 * cancel 在已审核已过账时红冲收款过账（postingDispatcher.reverse）后 reload setDocStatus(CANCELLED)，
 * 需 custom public override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpSalReceiptDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * writtenOffStatus（核销轴）/ settle 编排不受 docStatus 轴影响。非法边映射：Bean 抛 common 层
 * {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/{@code fromStatus} 元数据）作 cause，
 * {@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpSalErrors#ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION}（{@code receiptCode}/
 * {@code currentDocStatus}/{@code expectedDocStatus} 参数对外不变）。
 */
public class ErpSalReceiptCancelProcessor extends AbstractCancelProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Inject
    SalReceiptPostingDispatcher postingDispatcher;

    @Inject
    ErpSalReceiptDocumentStateMachine stateMachine;

    @Override
    public ErpSalReceipt cancel(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireEntity(id);
        validateTransitionForCancel(receipt, context);
        String approveStatus = receipt.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = dao().getEntityById(id);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        setDocStatus(receipt, cancelledDocStatus());
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
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpSalReceipt entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void validateTransitionForCancel(ErpSalReceipt entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected void setDocStatus(ErpSalReceipt entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
