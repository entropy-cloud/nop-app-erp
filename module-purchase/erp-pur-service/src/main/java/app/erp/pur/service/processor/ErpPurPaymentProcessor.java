package app.erp.pur.service.processor;

import app.erp.fin.biz.IErpFinBudgetControlBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.entity.PaymentSettler;
import app.erp.pur.service.posting.PurPaymentPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 付款单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
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

    @Inject
    IErpFinBudgetControlBiz budgetControlBiz;

    public ErpPurPayment submitForApproval(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        validateTransitionForSubmit(payment, context);
        validateBusinessRulesForSubmit(payment, context);
        doSubmit(payment, context);
        return payment;
    }

    public ErpPurPayment withdrawApproval(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        validateNotCancelled(payment, context);
        validateTransitionForWithdraw(payment, context);
        doWithdrawSubmit(payment, context);
        return payment;
    }

    public ErpPurPayment approve(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        if (payment.isApproved()) {
            return payment;
        }
        validateNotCancelled(payment, context);
        validateTransitionForApprove(payment, context);
        validateBusinessRulesForApprove(payment, context);

        boolean posted = doPosting(payment, context);
        payment = paymentDao().getEntityById(id);
        doApprove(payment, posted, context);
        return payment;
    }

    public ErpPurPayment reject(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        validateNotCancelled(payment, context);
        validateTransitionForReject(payment, context);
        doReject(payment, context);
        return payment;
    }

    public ErpPurPayment reverseApprove(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        if (payment.isRejected()) {
            return payment;
        }
        validateTransitionForReverseApprove(payment, context);
        if (Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = paymentDao().getEntityById(id);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        doReverseApprove(payment, context);
        return payment;
    }

    public ErpPurPayment cancel(String id, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        validateTransitionForCancel(payment, context);
        String approveStatus = payment.getApproveStatus();
        if (approveStatus != null && Objects.equals(approveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(payment.getPosted())) {
            postingDispatcher.reverse(payment);
            payment = paymentDao().getEntityById(id);
            payment.setPosted(false);
            payment.setPostedAt(null);
            payment.setPostedBy(null);
        }
        doCancel(payment, context);
        return payment;
    }

    public ErpPurPayment settle(String id, List<SettlementAllocation> allocations, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        return paymentSettler.settle(payment, allocations);
    }

    public ErpPurPayment reverseSettlement(String id, Long invoiceId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
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
        runBudgetCheckHook(payment, context);
    }

    /**
     * 预算控制钩子（budget.md §业务规则2/8）。经 {@code erp-fin.budget-check-enabled} 门控（默认 false，向后兼容）。
     * 付款单无科目维度，按 {@code erp-fin.budget-purchase-expense-subject-code} 配置的默认采购费用科目、
     * 按付款业务日期解析的会计期间，对付款本位币金额做预算余量校验。科目/期间未配置时静默跳过。
     */
    protected void runBudgetCheckHook(ErpPurPayment payment, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CHECK_ENABLED, Boolean.FALSE))) {
            return;
        }
        Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_PURCHASE_EXPENSE_SUBJECT_CODE);
        if (subjectId == null) {
            return;
        }
        Long periodId = resolvePeriodId(payment.getBusinessDate());
        BigDecimal amount = payment.getAmountFunctional() != null
                ? payment.getAmountFunctional() : BigDecimal.ZERO;
        budgetControlBiz.check(subjectId, null, periodId, amount, "AP_PAYMENT", payment.getCode(), context);
    }

    protected Long resolveBudgetSubjectId(String configKey) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            return null;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    protected Long resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(le("startDate", businessDate));
        q.addFilter(ge("endDate", businessDate));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
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
        payment.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            payment.setPosted(true);
            payment.setPostedAt(CoreMetrics.currentTimestamp());
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
        payment.setApprovedBy(null);
        payment.setApprovedAt(null);
        paymentDao().updateEntity(payment);
    }

    protected void doCancel(ErpPurPayment payment, IServiceContext context) {
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        paymentDao().updateEntity(payment);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurPayment requirePayment(String id, IServiceContext context) {
        ErpPurPayment payment = paymentDao().getEntityById(id);
        if (payment == null) {
            throw new NopException(ErpPurErrors.ERR_PAYMENT_NOT_FOUND)
                    .param(ErpPurErrors.ARG_PAYMENT_ID, id);
        }
        return payment;
    }

    protected void validateNotCancelled(ErpPurPayment payment, IServiceContext context) {
        if (payment.isCancelled()) {
            throw illegalDocTransition(payment, payment.getDocStatus(), "非已作废");
        }
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
