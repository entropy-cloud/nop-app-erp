
package app.erp.sal.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalReceiptBiz;
import app.erp.sal.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalReceiptPostingDispatcher;
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
 * 收款单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/state-machine.md}，与采购域付款单镜像对称）+ 收款审核触发 RECEIPT 过账
 * （{@code posting.md}，借银行存款 / 贷应收）+ 域级核销（{@link ReceiptSettler}）。
 *
 * <p>跨实体访问：客户启用校验经 {@link IErpMdPartnerBiz}；过账经 {@link SalReceiptPostingDispatcher} →
 * 凭证聚合根 Facade {@code IErpFinVoucherBiz}；核销经 {@link ReceiptSettler}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpSalReceipt")
public class ErpSalReceiptBizModel extends CrudBizModel<ErpSalReceipt> implements IErpSalReceiptBiz {

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    SalReceiptPostingDispatcher postingDispatcher;

    @Inject
    ReceiptSettler receiptSettler;

    public ErpSalReceiptBizModel() {
        setEntityName(ErpSalReceipt.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReceipt submit(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        requireNotCancelled(receipt);
        Integer status = receipt.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(receipt, status, "UNSUBMITTED 或 REJECTED");
        }
        requireCustomerActive(receipt, context);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt withdrawSubmit(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        requireNotCancelled(receipt);
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt approve(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        Integer status = receipt.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return receipt;
        }
        requireNotCancelled(receipt);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
        requireCustomerActive(receipt, context);

        // RECEIPT 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）
        boolean posted = postingDispatcher.tryPost(receipt);

        // 跨域 post 调用扰动会话脏跟踪，重新加载后置 posted 标志并显式持久化（对齐 ErpSalInvoiceBizModel）
        receipt = dao().getEntityById(receiptId);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setApprovedBy(currentUserId());
        receipt.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            receipt.setPosted(true);
            receipt.setPostedAt(CoreMetrics.currentDateTime());
            receipt.setPostedBy(currentUserId());
        }
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt reject(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        requireNotCancelled(receipt);
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseApprove(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        Integer status = receipt.getApproveStatus();
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return receipt;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(receipt, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = dao().getEntityById(receiptId);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        Integer docStatus = receipt.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receipt, docStatus, "非已作废");
        }
        Integer approveStatus = receipt.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = dao().getEntityById(receiptId);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(receipt);
        return receipt;
    }

    @Override
    @BizMutation
    public ErpSalReceipt settle(@Name("receiptId") Long receiptId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.settle(receipt, allocations);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.reverseSettlement(receipt, invoiceId);
    }

    // ---------- validation helpers ----------

    private ErpSalReceipt requireReceipt(Long receiptId, IServiceContext context) {
        return requireEntity(String.valueOf(receiptId), null, context);
    }

    private void requireNotCancelled(ErpSalReceipt receipt) {
        Integer docStatus = receipt.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receipt, docStatus, "非已作废");
        }
    }

    private void requireCustomerActive(ErpSalReceipt receipt, IServiceContext context) {
        if (receipt.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(receipt.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, receipt.getCustomerId());
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

    private NopException illegalTransition(ErpSalReceipt receipt, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalReceipt receipt, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
