package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalInvoicePostingDispatcher;
import app.erp.sal.service.statemachine.ErpSalInvoiceDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpSalInvoice cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source;
 * StateMachine 接线 plan 2026-08-13-0810-2 M4.23)。
 * cancel 在已审核已过账时红冲 AR 发票过账（postingDispatcher.reverse）后 reload setDocStatus(CANCELLED)，
 * 需 custom public override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpSalInvoiceDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpSalErrors#ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION}（{@code invoiceCode}/
 * {@code currentDocStatus}/{@code expectedDocStatus} 参数对外不变）。
 */
public class ErpSalInvoiceCancelProcessor extends AbstractCancelProcessor<ErpSalInvoice> {

    @Inject
    ErpSalInvoiceProcessor processor;

    @Inject
    SalInvoicePostingDispatcher postingDispatcher;

    @Inject
    ErpSalInvoiceDocumentStateMachine stateMachine;

    @Override
    public ErpSalInvoice cancel(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireEntity(id);
        validateTransitionForCancel(invoice, context);
        String approveStatus = invoice.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = dao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        setDocStatus(invoice, cancelledDocStatus());
        dao().updateEntity(invoice);
        return invoice;
    }

    @Override
    protected IEntityDao<ErpSalInvoice> dao() {
        return daoProvider.daoFor(ErpSalInvoice.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_INVOICE_NOT_FOUND)
                .param(ErpSalErrors.ARG_INVOICE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalInvoice entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpSalInvoice entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void validateTransitionForCancel(ErpSalInvoice entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected void setDocStatus(ErpSalInvoice entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
