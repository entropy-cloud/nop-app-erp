package app.erp.pur.service.processor;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.entity.ThreeWayMatcher;
import app.erp.pur.service.posting.PurInvoicePostingDispatcher;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购发票审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpPurInvoiceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    ThreeWayMatcher threeWayMatcher;

    @Inject
    PurInvoicePostingDispatcher postingDispatcher;

    @Inject
    IErpFinBudgetCommitmentBiz budgetCommitmentBiz;

    public ErpPurInvoice submitForApproval(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        validateTransitionForSubmit(invoice, context);
        validateBusinessRulesForSubmit(invoice, context);
        doSubmit(invoice, context);
        return invoice;
    }

    public ErpPurInvoice withdrawApproval(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        validateNotCancelled(invoice, context);
        validateTransitionForWithdraw(invoice, context);
        doWithdrawSubmit(invoice, context);
        return invoice;
    }

    public ErpPurInvoice approve(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        if (invoice.isApproved()) {
            return invoice;
        }
        validateNotCancelled(invoice, context);
        validateTransitionForApprove(invoice, context);
        validateBusinessRulesForApprove(invoice, context);

        boolean posted = doPosting(invoice, context);
        invoice = invoiceDao().getEntityById(id);
        doApprove(invoice, posted, context);
        // A2 承付 release-on-invoice-approve hook（plan 2026-07-21-1206-2，budget.md §承付会计 §3 接入点 #3）：
        // AP 发票过账 = 实际占用产生 = 释放承付。按关联订单 code 反查 COMMITMENT 凭证红冲。
        // config-gated（erp-fin.budget-commitment-enabled 默认 false）；严格对齐 budget.md:78 "被发票接收时红冲"。
        // **reject release-receive-complete**（ErpPurReceive 入库路径）——入库是库存移动不产生 AP ACTUAL 占用。
        runCommitmentReleaseOnInvoiceApproveHook(invoice, context);
        return invoice;
    }

    public ErpPurInvoice reject(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        validateNotCancelled(invoice, context);
        validateTransitionForReject(invoice, context);
        doReject(invoice, context);
        return invoice;
    }

    public ErpPurInvoice reverseApprove(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        if (invoice.isRejected()) {
            return invoice;
        }
        validateTransitionForReverseApprove(invoice, context);
        if (Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = invoiceDao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        doReverseApprove(invoice, context);
        return invoice;
    }

    public ErpPurInvoice cancel(String id, IServiceContext context) {
        ErpPurInvoice invoice = requireInvoice(id, context);
        validateTransitionForCancel(invoice, context);
        String approveStatus = invoice.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(invoice.getPosted())) {
            postingDispatcher.reverse(invoice);
            invoice = invoiceDao().getEntityById(id);
            invoice.setPosted(false);
            invoice.setPostedAt(null);
            invoice.setPostedBy(null);
        }
        doCancel(invoice, context);
        return invoice;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpPurInvoice invoice, IServiceContext context) {
        validateNotCancelled(invoice, context);
        String status = invoice.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(invoice, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(invoice, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurInvoice invoice, IServiceContext context) {
        String status = invoice.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(invoice, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurInvoice invoice, IServiceContext context) {
        String docStatus = invoice.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(invoice, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurInvoice invoice, IServiceContext context) {
        requireLinesNonEmpty(invoice, context);
        requireSupplierActive(invoice, context);
    }

    protected void validateBusinessRulesForApprove(ErpPurInvoice invoice, IServiceContext context) {
        requireSupplierActive(invoice, context);
        List<ErpPurInvoiceLine> lines = loadLines(invoice);
        threeWayMatcher.match(invoice.getCode(), lines, null);
    }

    // ---------- step：过账/执行 ----------

    protected boolean doPosting(ErpPurInvoice invoice, IServiceContext context) {
        return postingDispatcher.tryPost(invoice);
    }

    protected void doSubmit(ErpPurInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doWithdrawSubmit(ErpPurInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doApprove(ErpPurInvoice invoice, boolean posted, IServiceContext context) {
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        invoice.setApprovedBy(currentUserId());
        invoice.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            invoice.setPosted(true);
            invoice.setPostedAt(CoreMetrics.currentTimestamp());
            invoice.setPostedBy(currentUserId());
        }
        invoiceDao().updateEntity(invoice);
    }

    protected void doReject(ErpPurInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        invoiceDao().updateEntity(invoice);
    }

    protected void doReverseApprove(ErpPurInvoice invoice, IServiceContext context) {
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        invoice.setApprovedBy(null);
        invoice.setApprovedAt(null);
        invoiceDao().updateEntity(invoice);
    }

    protected void doCancel(ErpPurInvoice invoice, IServiceContext context) {
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        invoiceDao().updateEntity(invoice);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurInvoice requireInvoice(String id, IServiceContext context) {
        ErpPurInvoice invoice = invoiceDao().getEntityById(id);
        if (invoice == null) {
            throw new NopException(ErpPurErrors.ERR_INVOICE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_INVOICE_ID, id);
        }
        return invoice;
    }

    protected void validateNotCancelled(ErpPurInvoice invoice, IServiceContext context) {
        if (invoice.isCancelled()) {
            throw illegalDocTransition(invoice, invoice.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpPurInvoice invoice, IServiceContext context) {
        if (invoice.getLines().isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_INVOICE_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode());
        }
    }

    protected void requireSupplierActive(ErpPurInvoice invoice, IServiceContext context) {
        if (invoice.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(invoice.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpPurConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, invoice.getSupplierId());
        }
    }

    /**
     * 通过 ORM to-many 关系 {@code ErpPurInvoice.lines} 加载行（懒加载，复用主实体 session）。
     * 关系已在 {@code app-erp-purchase.orm.xml} 声明。
     */
    protected List<ErpPurInvoiceLine> loadLines(ErpPurInvoice invoice) {
        return new ArrayList<>(invoice.getLines());
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpPurInvoice> invoiceDao() {
        return daoProvider.daoFor(ErpPurInvoice.class);
    }

    /**
     * A2 承付 release-on-invoice-approve hook（plan 2026-07-21-1206-2，budget.md §承付会计 §3 接入点 #3）。
     * AP 发票过账 = 实际占用产生 = 释放承付（红冲原 COMMITMENT 凭证）。
     *
     * <p>实现路径：发票 approve 后置 → 经 invoiceLine.receiveLineId 反查 receive → receive.orderId → order.code
     * → 对每个唯一 order.code 调用 {@link IErpFinBudgetCommitmentBiz#release}（容错：无原凭证静默跳过）。
     *
     * <p>config-gated（{@code erp-fin.budget-commitment-enabled} 默认 false）。
     * <b>reject release-receive-complete</b>（ErpPurReceive 入库路径）——入库是库存移动不产生 AP ACTUAL 占用。
     */
    protected void runCommitmentReleaseOnInvoiceApproveHook(ErpPurInvoice invoice, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))) {
            return;
        }
        Set<String> orderCodes = resolveLinkedOrderCodes(invoice);
        if (orderCodes.isEmpty()) {
            return;
        }
        for (String orderCode : orderCodes) {
            try {
                budgetCommitmentBiz.release(
                        ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, orderCode, context);
            } catch (NopException e) {
                // 容错：无原凭证（ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）静默跳过；其他异常重新抛出
                if (!isCommitmentAlreadyReleased(e)) {
                    throw e;
                }
            }
        }
    }

    /** 判断异常是否为"无原承付凭证可红冲"（invoice-approve 路径容错：无原凭证静默跳过）。 */
    protected boolean isCommitmentAlreadyReleased(NopException e) {
        return app.erp.fin.service.ErpFinErrors.ERR_BUDGET_COMMITMENT_ALREADY_RELEASED.getErrorCode()
                .equals(e.getErrorCode());
    }

    /** 经 invoiceLine.receiveLineId → receiveLine.receiveId → receive.orderId → order.code 反查关联订单编码集合。 */
    protected Set<String> resolveLinkedOrderCodes(ErpPurInvoice invoice) {
        Set<String> codes = new HashSet<>();
        List<ErpPurInvoiceLine> lines = loadLines(invoice);
        Set<Long> receiveLineIds = new HashSet<>();
        for (ErpPurInvoiceLine il : lines) {
            if (il.getReceiveLineId() != null) {
                receiveLineIds.add(il.getReceiveLineId());
            }
        }
        if (receiveLineIds.isEmpty()) {
            return codes;
        }
        IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        Set<Long> receiveIds = new HashSet<>();
        for (ErpPurReceiveLine rl : rlDao.findAllByQuery(inQuery("id", receiveLineIds))) {
            if (rl.getReceiveId() != null) {
                receiveIds.add(rl.getReceiveId());
            }
        }
        if (receiveIds.isEmpty()) {
            return codes;
        }
        IEntityDao<ErpPurReceive> rDao = daoProvider.daoFor(ErpPurReceive.class);
        Set<Long> orderIds = new HashSet<>();
        for (ErpPurReceive r : rDao.findAllByQuery(inQuery("id", receiveIds))) {
            if (r.getOrderId() != null) {
                orderIds.add(r.getOrderId());
            }
        }
        if (orderIds.isEmpty()) {
            return codes;
        }
        IEntityDao<ErpPurOrder> oDao = daoProvider.daoFor(ErpPurOrder.class);
        for (ErpPurOrder o : oDao.findAllByQuery(inQuery("id", orderIds))) {
            if (o.getCode() != null) {
                codes.add(o.getCode());
            }
        }
        return codes;
    }

    private static io.nop.api.core.beans.query.QueryBean inQuery(String field, Set<Long> values) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.in(field, new ArrayList<>(values)));
        return q;
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

    protected NopException illegalTransition(ErpPurInvoice invoice, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurInvoice invoice, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_INVOICE_CODE, invoice.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
