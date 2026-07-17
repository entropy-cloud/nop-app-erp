package app.erp.sal.service.processor;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.CreditLimitChecker;
import app.erp.sal.service.posting.SalInvoicePostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售发票审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 *
 * <p>跨实体：客户启用校验经 {@link IErpMdPartnerBiz}；过账经 {@link SalInvoicePostingDispatcher} →凭证聚合根 Facade。
 */
public class ErpSalInvoiceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    SalInvoicePostingDispatcher postingDispatcher;

    @Inject
    CreditLimitChecker creditLimitChecker;

    public ErpSalInvoice submitForApproval(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(id, context);
        validateNotCancelled(invoice, context);
        validateTransitionForSubmit(invoice, context);
        validateBusinessRulesForSubmit(invoice, context);
        doSubmit(invoice, context);
        return invoice;
    }

    public ErpSalInvoice withdrawApproval(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(id, context);
        validateNotCancelled(invoice, context);
        validateTransitionForWithdraw(invoice, context);
        doWithdrawSubmit(invoice, context);
        return invoice;
    }

    public ErpSalInvoice approve(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(id, context);
        if (invoice.isApproved()) {
            return invoice;
        }
        validateNotCancelled(invoice, context);
        validateTransitionForApprove(invoice, context);
        validateBusinessRulesForApprove(invoice, context);

        boolean posted = doPosting(invoice, context);
        invoice = invoiceDao().getEntityById(id);
        doApprove(invoice, posted, context);
        return invoice;
    }

    public ErpSalInvoice reject(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(id, context);
        validateNotCancelled(invoice, context);
        validateTransitionForReject(invoice, context);
        doReject(invoice, context);
        return invoice;
    }

    public ErpSalInvoice reverseApprove(String id, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(id, context);
        if (invoice.isRejected()) {
            return invoice;
        }
        validateTransitionForReverseApprove(invoice, context);
        doReverseApprove(invoice, context);
        return invoice;
    }

    public ErpSalInvoice cancel(String invoiceId, IServiceContext context) {
        ErpSalInvoice invoice = requireInvoice(invoiceId, context);
        validateTransitionForCancel(invoice, context);
        String approveStatus = invoice.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = invoiceDao().getEntityById(invoiceId);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        doCancel(invoice, context);
        return invoice;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(invoice, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(invoice, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalInvoice invoice, IServiceContext context) {
        String docStatus = invoice.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalInvoice invoice, IServiceContext context) {
        requireLinesNonEmpty(invoice, context);
        requireCustomerActive(invoice, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalInvoice invoice, IServiceContext context) {
        requireCustomerActive(invoice, context);
        enforceCreditHold(invoice, context);
    }

    /**
     * 发票审核信用冻结检查（config-gated by {@code erp-sal.credit-check-on-invoice}，默认 false 向后兼容）。
     *
     * <p>检查客户当前信用状况是否已超额（订单审核时额度已被占用，此处为 point-in-time hold）。
     * 详见 {@link CreditLimitChecker#checkCreditHold}。
     */
    protected void enforceCreditHold(ErpSalInvoice invoice, IServiceContext context) {
        if (!AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_ON_INVOICE,
                ErpSalConstants.CREDIT_CHECK_ON_INVOICE_DEFAULT)) {
            return;
        }
        creditLimitChecker.checkCreditHold(invoice.getCustomerId(), invoice.getCode(),
                ErpSalConstants.BILL_TYPE_INVOICE, context);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doWithdrawSubmit(ErpSalInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doApprove(ErpSalInvoice invoice, boolean posted, IServiceContext context) {
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        invoice.setApprovedBy(currentUserId());
        invoice.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            invoice.setPosted(true);
            invoice.setPostedAt(CoreMetrics.currentTimestamp());
            invoice.setPostedBy(currentUserId());
        }
        invoiceDao().updateEntity(invoice);
    }

    protected void doReject(ErpSalInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doReverseApprove(ErpSalInvoice invoice, IServiceContext context) {
        if (Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = invoiceDao().getEntityById(invoice.getId());
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        invoice.setApprovedBy(null);
        invoice.setApprovedAt(null);
        invoiceDao().updateEntity(invoice);
    }

    protected void doCancel(ErpSalInvoice invoice, IServiceContext context) {
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        invoiceDao().updateEntity(invoice);
    }

    // ---------- 过账 ----------

    protected boolean doPosting(ErpSalInvoice invoice, IServiceContext context) {
        return postingDispatcher.tryPost(invoice);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalInvoice requireInvoice(String id, IServiceContext context) {
        ErpSalInvoice invoice = invoiceDao().getEntityById(id);
        if (invoice == null) {
            throw new NopException(ErpSalErrors.ERR_INVOICE_NOT_FOUND)
                    .param(ErpSalErrors.ARG_INVOICE_CODE, id);
        }
        return invoice;
    }

    protected void validateNotCancelled(ErpSalInvoice invoice, IServiceContext context) {
        if (invoice.isCancelled()) {
            throw illegalDocTransition(invoice, invoice.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpSalInvoice invoice, IServiceContext context) {
        if (loadLines(invoice.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_INVOICE_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalInvoice invoice, IServiceContext context) {
        if (invoice.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(invoice.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, invoice.getCustomerId());
        }
    }

    protected List<ErpSalInvoiceLine> loadLines(Long invoiceId) {
        IEntityDao<ErpSalInvoiceLine> dao = daoProvider.daoFor(ErpSalInvoiceLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("invoiceId", invoiceId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalInvoice> invoiceDao() {
        return daoProvider.daoFor(ErpSalInvoice.class);
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

    protected NopException illegalTransition(ErpSalInvoice invoice, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalInvoice invoice, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
