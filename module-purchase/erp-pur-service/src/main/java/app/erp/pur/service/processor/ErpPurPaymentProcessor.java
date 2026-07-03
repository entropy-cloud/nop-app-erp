package app.erp.pur.service.processor;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.entity.PaymentSettler;
import app.erp.pur.service.posting.PurPaymentPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.List;

/**
 * 付款单审批状态机 + PAYMENT 过账 + 域级核销编排 Processor
 * （{@code processor-extension-pattern.md} Facade + Processor）。Facade {@code ErpPurPaymentBizModel}
 * 仅负责入口/事务/委托，编排委托本类。
 */
public class ErpPurPaymentProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    PurPaymentPostingDispatcher postingDispatcher;

    @Inject
    PaymentSettler paymentSettler;

    public ErpPurPayment submit(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        validateTransitionForSubmit(payment, context);
        validateBusinessRulesForSubmit(payment, context);
        doSubmit(payment, context);
        return payment;
    }

    public ErpPurPayment withdrawSubmit(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        validateNotCancelled(payment, context);
        validateTransitionForWithdraw(payment, context);
        doWithdrawSubmit(payment, context);
        return payment;
    }

    public ErpPurPayment approve(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        if (isAlreadyApproved(payment)) {
            return payment;
        }
        validateNotCancelled(payment, context);
        validateTransitionForApprove(payment, context);
        validateBusinessRulesForApprove(payment, context);

        boolean posted = doPosting(payment, context);
        // 跨域 post 调用扰动会话脏跟踪，重新加载后置 posted 标志并显式持久化。
        payment = paymentDao().getEntityById(paymentId);
        doApprove(payment, posted, context);
        return payment;
    }

    public ErpPurPayment reject(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        validateNotCancelled(payment, context);
        validateTransitionForReject(payment, context);
        doReject(payment, context);
        return payment;
    }

    public ErpPurPayment reverseApprove(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        if (isAlreadyRejected(payment)) {
            return payment;
        }
        validateTransitionForReverseApprove(payment, context);
        if (Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = paymentDao().getEntityById(paymentId);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        doReverseApprove(payment, context);
        return payment;
    }

    public ErpPurPayment cancel(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        validateTransitionForCancel(payment, context);
        String approveStatus = payment.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = paymentDao().getEntityById(paymentId);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        doCancel(payment, context);
        return payment;
    }

    public ErpPurPayment settle(Long paymentId, List<SettlementAllocation> allocations, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        return paymentSettler.settle(payment, allocations);
    }

    public ErpPurPayment reverseSettlement(Long paymentId, Long invoiceId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        return paymentSettler.reverseSettlement(payment, invoiceId);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpPurPayment payment, IServiceContext context) {
        validateNotCancelled(payment, context);
        String status = payment.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(payment, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurPayment payment, IServiceContext context) {
        String status = payment.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurPayment payment, IServiceContext context) {
        String status = payment.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurPayment payment, IServiceContext context) {
        String status = payment.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurPayment payment, IServiceContext context) {
        String status = payment.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(payment, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurPayment payment, IServiceContext context) {
        String docStatus = payment.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(payment, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurPayment payment, IServiceContext context) {
        requireSupplierActive(payment, context);
    }

    protected void validateBusinessRulesForApprove(ErpPurPayment payment, IServiceContext context) {
        requireSupplierActive(payment, context);
    }

    // ---------- step：过账/执行 ----------

    protected boolean doPosting(ErpPurPayment payment, IServiceContext context) {
        return postingDispatcher.tryPost(payment);
    }

    protected void doSubmit(ErpPurPayment payment, IServiceContext context) {
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        paymentDao().updateEntity(payment);
    }

    protected void doWithdrawSubmit(ErpPurPayment payment, IServiceContext context) {
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        paymentDao().updateEntity(payment);
    }

    protected void doApprove(ErpPurPayment payment, boolean posted, IServiceContext context) {
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        payment.setApprovedBy(currentUserId());
        payment.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            payment.setPosted(true);
            payment.setPostedAt(CoreMetrics.currentDateTime());
            payment.setPostedBy(currentUserId());
        }
        paymentDao().updateEntity(payment);
    }

    protected void doReject(ErpPurPayment payment, IServiceContext context) {
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        paymentDao().updateEntity(payment);
    }

    protected void doReverseApprove(ErpPurPayment payment, IServiceContext context) {
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        paymentDao().updateEntity(payment);
    }

    protected void doCancel(ErpPurPayment payment, IServiceContext context) {
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        paymentDao().updateEntity(payment);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurPayment requirePayment(Long paymentId, IServiceContext context) {
        ErpPurPayment payment = paymentDao().getEntityById(paymentId);
        if (payment == null) {
            throw new NopException(ErpPurErrors.ERR_PAYMENT_NOT_FOUND)
                    .param(ErpPurErrors.ARG_PAYMENT_ID, paymentId);
        }
        return payment;
    }

    protected void validateNotCancelled(ErpPurPayment payment, IServiceContext context) {
        String docStatus = payment.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(payment, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpPurPayment payment) {
        String status = payment.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpPurPayment payment) {
        String status = payment.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireSupplierActive(ErpPurPayment payment, IServiceContext context) {
        if (payment.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(payment.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpPurConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, payment.getSupplierId());
        }
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpPurPayment> paymentDao() {
        return daoProvider.daoFor(ErpPurPayment.class);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpPurPayment payment, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurPayment payment, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
