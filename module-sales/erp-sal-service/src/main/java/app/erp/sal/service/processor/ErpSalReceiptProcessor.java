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
import java.util.Objects;

import java.util.List;

/**
 * 收款单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
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

    public ErpSalReceipt submitForApproval(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(id, context);
        validateNotCancelled(receipt, context);
        validateTransitionForSubmit(receipt, context);
        validateBusinessRulesForSubmit(receipt, context);
        doSubmit(receipt, context);
        return receipt;
    }

    public ErpSalReceipt withdrawApproval(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(id, context);
        validateNotCancelled(receipt, context);
        validateTransitionForWithdraw(receipt, context);
        doWithdrawSubmit(receipt, context);
        return receipt;
    }

    public ErpSalReceipt approve(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(id, context);
        if (isAlreadyApproved(receipt)) {
            return receipt;
        }
        validateNotCancelled(receipt, context);
        validateTransitionForApprove(receipt, context);
        validateBusinessRulesForApprove(receipt, context);
        doApprove(receipt, context);
        return receipt;
    }

    public ErpSalReceipt reject(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(id, context);
        validateNotCancelled(receipt, context);
        validateTransitionForReject(receipt, context);
        doReject(receipt, context);
        return receipt;
    }

    public ErpSalReceipt reverseApprove(String id, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(id, context);
        if (isAlreadyRejected(receipt)) {
            return receipt;
        }
        validateTransitionForReverseApprove(receipt, context);
        doReverseApprove(receipt, context);
        return receipt;
    }

    public ErpSalReceipt cancel(String receiptId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        validateTransitionForCancel(receipt, context);
        String approveStatus = receipt.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)
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

    public ErpSalReceipt settle(String receiptId, List<SettlementAllocation> allocations, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.settle(receipt, allocations);
    }

    public ErpSalReceipt reverseSettlement(String receiptId, Long invoiceId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return receiptSettler.reverseSettlement(receipt, invoiceId);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalReceipt receipt, IServiceContext context) {
        String status = receipt.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(receipt, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalReceipt receipt, IServiceContext context) {
        String status = receipt.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalReceipt receipt, IServiceContext context) {
        String status = receipt.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalReceipt receipt, IServiceContext context) {
        String status = receipt.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receipt, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalReceipt receipt, IServiceContext context) {
        String status = receipt.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(receipt, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalReceipt receipt, IServiceContext context) {
        String docStatus = receipt.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
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

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doWithdrawSubmit(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        receiptDao().updateEntity(receipt);
    }

    protected void doApprove(ErpSalReceipt receipt, IServiceContext context) {
        boolean posted = doPosting(receipt, context);
        receipt = receiptDao().getEntityById(receipt.getId());
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
        if (Boolean.TRUE.equals(receipt.getPosted())) {
            postingDispatcher.reverse(receipt);
            receipt = receiptDao().getEntityById(receipt.getId());
            receipt.setPosted(false);
            receipt.setPostedAt(null);
            receipt.setPostedBy(null);
        }
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        receipt.setApprovedBy(null);
        receipt.setApprovedAt(null);
        receiptDao().updateEntity(receipt);
    }

    protected void doCancel(ErpSalReceipt receipt, IServiceContext context) {
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        receiptDao().updateEntity(receipt);
    }

    // ---------- 过账 ----------

    protected boolean doPosting(ErpSalReceipt receipt, IServiceContext context) {
        return postingDispatcher.tryPost(receipt);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalReceipt requireReceipt(String id, IServiceContext context) {
        ErpSalReceipt receipt = receiptDao().getEntityById(id);
        if (receipt == null) {
            throw new NopException(ErpSalErrors.ERR_RECEIPT_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RECEIPT_CODE, id);
        }
        return receipt;
    }

    protected void validateNotCancelled(ErpSalReceipt receipt, IServiceContext context) {
        String docStatus = receipt.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(receipt, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalReceipt receipt) {
        String status = receipt.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpSalReceipt receipt) {
        String status = receipt.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireCustomerActive(ErpSalReceipt receipt, IServiceContext context) {
        if (receipt.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(receipt.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
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

    protected NopException illegalTransition(ErpSalReceipt receipt, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalReceipt receipt, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RECEIPT_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RECEIPT_CODE, receipt.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
