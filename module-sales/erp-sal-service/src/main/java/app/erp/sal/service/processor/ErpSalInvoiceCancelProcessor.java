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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSalInvoice）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpSalInvoice cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source;
 * StateMachine 接线 plan 2026-08-13-0810-2 M4.23)。
 * cancel 在已审核已过账时红冲 AR 发票过账（postingDispatcher.reverse）后 reload setDocStatus(CANCELLED)，
 * 需 custom public override。经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpSalInvoiceDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边直抛领域码 {@link ErpSalErrors#ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION}（plan 2026-09-07-2200-1），
 * {@link #validateTransitionForCancel} 同码补参 {@code invoiceCode} 实体编号。
 */
public class ErpSalInvoiceCancelProcessor extends AbstractCancelProcessor<ErpSalInvoice> {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ErpSalInvoiceCancelProcessor.class);

    @Inject
    ErpSalInvoiceProcessor processor;

    @Inject
    SalInvoicePostingDispatcher postingDispatcher;

    @Inject
    ErpSalInvoiceDocumentStateMachine stateMachine;

    @Inject
    @jakarta.annotation.Nullable
    app.erp.fin.biz.IErpFinPostingExceptionBiz postingExceptionBiz;

    @Override
    public ErpSalInvoice cancel(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireEntity(id);
        validateTransitionForCancel(invoice, context);
        String approveStatus = invoice.getApproveStatus();
        // P1-CK-sal-003（sales 侧收口）：posted 标志 + 凭证存在性双判（sweep 异步重试成功后 posted 不回写）。
        if (approveStatus != null && Objects.equals(approveStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)
                && (Boolean.TRUE.equals(invoice.getPosted()) || processor.hasActivePosting(invoice.getCode()))) {
            postingDispatcher.reverse(invoice);
            invoice = dao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        } else {
            // P2-CK-sal-019：无红冲出口联动作废 PENDING 过账异常（sweep 仅扫 PENDING，重放通道关闭），
            // 修复「作废后 sweep 24h 窗口内重试成功 → 为已作废单据生成有效凭证且无人红冲」竞态。
            ignorePendingPostingExceptions(invoice, context);
        }
        setDocStatus(invoice, cancelledDocStatus());
        dao().updateEntity(invoice);
        return invoice;
    }

    /** P2-CK-sal-019：失败隔离（对齐 RC-R1.85 容错范式），不阻断作废主流程。 */
    private void ignorePendingPostingExceptions(ErpSalInvoice invoice, IServiceContext context) {
        if (postingExceptionBiz == null) {
            return;
        }
        try {
            int ignored = postingExceptionBiz.ignorePendingByBill(invoice.getCode(),
                    app.erp.fin.dao.ErpFinBusinessType.AR_INVOICE.name(), context);
            if (ignored > 0) {
                LOG.info("erp-sal-invoice-cancel-pending-posting-ignored: invoiceCode={}, ignored={}",
                        invoice.getCode(), ignored);
            }
        } catch (Exception e) {
            LOG.warn("erp-sal-invoice-cancel-pending-posting-ignore-failed (isolated, non-blocking): invoiceCode={}, reason={}",
                    invoice.getCode(), e.getMessage());
        }
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
            throw e.param(ErpSalErrors.ARG_INVOICE_CODE, entity.getCode());
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
