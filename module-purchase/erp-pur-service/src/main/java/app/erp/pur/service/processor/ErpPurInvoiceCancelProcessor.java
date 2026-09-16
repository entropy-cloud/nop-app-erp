package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.AbstractCancelProcessor;
import app.erp.pur.service.entity.PaymentSettler;
import app.erp.pur.service.posting.PurInvoicePostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPurInvoice）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。

public class ErpPurInvoiceCancelProcessor extends AbstractCancelProcessor<ErpPurInvoice> {

    @Inject
    ErpPurInvoiceProcessor processor;

    @Inject
    PurInvoicePostingDispatcher postingDispatcher;

    @Inject
    PaymentSettler settler;

    @Override
    public ErpPurInvoice cancel(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireEntity(id);
        processor.validateTransitionForCancel(invoice, context);
        // P2-CK-pur-005：存在净核销拒绝作废——已部分/全额付款的发票作废将使应付派生态失真；
        // 须先通过对应付款单 reverseSettlement（returns.md §异常处理「需先撤回核销」拒绝语义）。
        if (settler.sumNetSettledForInvoice(id).signum() != 0) {
            throw new NopException(ErpPurErrors.ERR_INVOICE_SETTLED_EXISTS)
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
        String approveStatus = invoice.getApproveStatus();
        boolean wasApproved = approveStatus != null
                && Objects.equals(approveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED);
        if (wasApproved && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = dao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        processor.doCancel(invoice, context);
        processor.runCommitmentRestoreOnInvoiceReverseHook(invoice, wasApproved, context);
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
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpPurInvoice entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurInvoice entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpPurConstants.DOC_STATUS_CANCELLED;
    }
}
