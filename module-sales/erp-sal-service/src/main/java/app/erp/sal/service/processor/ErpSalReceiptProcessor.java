package app.erp.sal.service.processor;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.ReceiptSettler;
import app.erp.sal.service.posting.SalReceiptPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 收款单审批状态机 + RECEIPT 过账 + 域级核销编排 Processor
 * （{@code processor-extension-pattern.md} Facade + Processor）。Facade {@code ErpSalReceiptBizModel}
 * 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>跨实体：客户启用校验经 {@link IErpMdPartnerBiz}；过账经 {@link SalReceiptPostingDispatcher} →凭证聚合根 Facade；
 * 核销经 {@link ReceiptSettler}。
 */
public class ErpSalReceiptProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    SalReceiptPostingDispatcher postingDispatcher;

    @Inject
    ReceiptSettler receiptSettler;

    public ErpSalReceipt submit(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        validateTransitionForSubmit(receipt, context);
        validateBusinessRulesForSubmit(receipt, context);
        doSubmit(receipt, context);
        return receipt;
    }

    public ErpSalReceipt withdrawSubmit(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        validateNotCancelled(receipt, context);
        validateTransitionForWithdraw(receipt, context);
        doWithdrawSubmit(receipt, context);
        return receipt;
    }

    public ErpSalReceipt approve(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        // 幂等：已审核单据再次审核为空操作
        if (isAlreadyApproved(receipt)) {
            return receipt;
        }
        validateNotCancelled(receipt, context);
        validateTransitionForApprove(receipt, context);
        validateBusinessRulesForApprove(receipt, context);

        boolean posted = doPosting(receipt, context);
        // 跨域 post 调用扰动会话脏跟踪，重新加载后置 posted 标志并显式持久化。
        receipt = receiptDao().getEntityById(receiptId);
        doApprove(receipt, posted, context);
        return receipt;
    }

    public ErpSalReceipt reject(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        validateNotCancelled(receipt, context);
        validateTransitionForReject(receipt, context);
        doReject(receipt, context);
        return receipt;
    }

    public ErpSalReceipt reverseApprove(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        if (isAlreadyRejected(receipt)) {
            return receipt;
        }
        validateTransitionForReverseApprove(receipt, context);
        if (Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = receiptDao().getEntityById(receiptId);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        doReverseApprove(receipt, context);
        return receipt;
    }

    public ErpSalReceipt cancel(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        validateTransitionForCancel(receipt, context);
        Integer approveStatus = receipt.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = receiptDao().getEntityById(receiptId);
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        doCancel(receipt, context);
        return receipt;
    }

    public ErpSalReceipt settle(Long receiptId, List<SettlementAllocation> allocations, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.settle(receipt, allocations);
    }

    public ErpSalReceipt reverseSettlement(Long receiptId, Long invoiceId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.reverseSettlement(receipt, invoiceId);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpSalReceipt receipt, IServiceContext context) {
        validateNotCancelled(receipt, context);
        Integer status = receipt.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(receipt, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalReceipt receipt, IServiceContext context) {
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalReceipt receipt, IServiceContext context) {
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalReceipt receipt, IServiceContext context) {
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalReceipt receipt, IServiceContext context) {
        Integer status = receipt.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(receipt, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalReceipt receipt, IServiceContext context) {
        Integer docStatus = receipt.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receipt, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalReceipt receipt, IServiceContext context) {
        requireCustomerActive(receipt, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalReceipt receipt, IServiceContext context) {
        requireCustomerActive(receipt, context);
    }

    // ---------- step：过账/执行 ----------

    protected boolean doPosting(ErpSalReceipt receipt, IServiceContext context) {
        return postingDispatcher.tryPost(receipt);
    }

    protected void doSubmit(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doWithdrawSubmit(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doApprove(ErpSalReceipt receipt, boolean posted, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setApprovedBy(currentUserId());
        receipt.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            receipt.setPosted(true);
            receipt.setPostedAt(CoreMetrics.currentDateTime());
            receipt.setPostedBy(currentUserId());
        }
        receiptDao().updateEntity(receipt);
    }

    protected void doReject(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doReverseApprove(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doCancel(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        receiptDao().updateEntity(receipt);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpSalReceipt requireReceipt(Long receiptId, IServiceContext context) {
        ErpSalReceipt receipt = receiptDao().getEntityById(receiptId);
        if (receipt == null) {
            throw new NopException(ErpSalErrors.ERR_RECEIPT_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RECEIPT_ID, receiptId);
        }
        return receipt;
    }

    protected void validateNotCancelled(ErpSalReceipt receipt, IServiceContext context) {
        Integer docStatus = receipt.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receipt, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalReceipt receipt) {
        Integer status = receipt.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    protected boolean isAlreadyRejected(ErpSalReceipt receipt) {
        Integer status = receipt.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED;
    }

    protected void requireCustomerActive(ErpSalReceipt receipt, IServiceContext context) {
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

    protected IEntityDao<ErpSalReceipt> receiptDao() {
        return daoProvider.daoFor(ErpSalReceipt.class);
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

    protected NopException illegalTransition(ErpSalReceipt receipt, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalReceipt receipt, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
