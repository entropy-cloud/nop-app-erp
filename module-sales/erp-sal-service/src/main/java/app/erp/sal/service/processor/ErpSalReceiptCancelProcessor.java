package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalReceiptPostingDispatcher;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpSalReceipt cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source).
 * cancel 在已审核已过账时红冲收款过账（postingDispatcher.reverse）后 reload setDocStatus(CANCELLED)，
 * 需 custom public override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 */
public class ErpSalReceiptCancelProcessor extends AbstractCancelProcessor<ErpSalReceipt> {

    @Inject
    ErpSalReceiptProcessor processor;

    @Inject
    SalReceiptPostingDispatcher postingDispatcher;

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
    protected void setDocStatus(ErpSalReceipt entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpSalConstants.DOC_STATUS_CANCELLED;
    }
}
