
package app.erp.pur.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurPaymentBiz;
import app.erp.pur.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.posting.PurPaymentPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 付款单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/purchase/state-machine.md}）+ 付款审核触发 PAYMENT 过账（{@code posting.md}，借应付/贷银行存款）
 * + 域级核销（{@link PaymentSettler}）。
 *
 * <p>跨实体访问：供应商启用校验经 {@link IErpMdPartnerBiz}；过账经 {@link PurPaymentPostingDispatcher} →
 * 凭证聚合根 Facade {@code IErpFinVoucherBiz}；核销经 {@link PaymentSettler}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpPurPayment")
public class ErpPurPaymentBizModel extends CrudBizModel<ErpPurPayment> implements IErpPurPaymentBiz {

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    PurPaymentPostingDispatcher postingDispatcher;

    @Inject
    PaymentSettler paymentSettler;

    public ErpPurPaymentBizModel() {
        setEntityName(ErpPurPayment.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurPayment submit(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        requireNotCancelled(payment);
        Integer status = payment.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(payment, status, "UNSUBMITTED 或 REJECTED");
        }
        requireSupplierActive(payment, context);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment withdrawSubmit(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        requireNotCancelled(payment);
        Integer status = payment.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment approve(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        Integer status = payment.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return payment;
        }
        requireNotCancelled(payment);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
        requireSupplierActive(payment, context);

        // PAYMENT 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）
        boolean posted = postingDispatcher.tryPost(payment);

        // 跨域 post 调用扰动会话脏跟踪，重新加载后置 posted 标志并显式持久化（对齐 ErpPurInvoiceBizModel）
        payment = requireEntity(String.valueOf(paymentId), null, context);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        payment.setApprovedBy(currentUserId());
        payment.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            payment.setPosted(true);
            payment.setPostedAt(CoreMetrics.currentDateTime());
            payment.setPostedBy(currentUserId());
        }
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment reject(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        requireNotCancelled(payment);
        Integer status = payment.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(payment, status, "SUBMITTED");
        }
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseApprove(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        Integer status = payment.getApproveStatus();
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return payment;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(payment, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = requireEntity(String.valueOf(paymentId), null, context);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        Integer docStatus = payment.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(payment, docStatus, "非已作废");
        }
        Integer approveStatus = payment.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpPurConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = requireEntity(String.valueOf(paymentId), null, context);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(payment);
        return payment;
    }

    @Override
    @BizMutation
    public ErpPurPayment settle(@Name("paymentId") Long paymentId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        return paymentSettler.settle(payment, allocations);
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        ErpPurPayment payment = requirePayment(paymentId, context);
        return paymentSettler.reverseSettlement(payment, invoiceId);
    }

    // ---------- validation helpers ----------

    private ErpPurPayment requirePayment(Long paymentId, IServiceContext context) {
        return requireEntity(String.valueOf(paymentId), null, context);
    }

    private void requireNotCancelled(ErpPurPayment payment) {
        Integer docStatus = payment.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(payment, docStatus, "非已作废");
        }
    }

    private void requireSupplierActive(ErpPurPayment payment, IServiceContext context) {
        if (payment.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(payment.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, payment.getSupplierId());
        }
    }

    // ---------- misc helpers ----------

    private String currentUserId() {
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

    private NopException illegalTransition(ErpPurPayment payment, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurPayment payment, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_PAYMENT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_PAYMENT_CODE, payment.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
