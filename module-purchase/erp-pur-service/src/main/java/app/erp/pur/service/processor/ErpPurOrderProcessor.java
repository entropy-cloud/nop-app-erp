package app.erp.pur.service.processor;

import app.erp.fin.biz.IErpFinBudgetControlBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
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

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 采购订单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpPurOrderProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpFinBudgetControlBiz budgetControlBiz;

    public ErpPurOrder submitForApproval(String id, IServiceContext context) {
        ErpPurOrder order = requireOrder(id, context);
        validateTransitionForSubmit(order, context);
        validateBusinessRulesForSubmit(order, context);
        doSubmit(order, context);
        return order;
    }

    public ErpPurOrder withdrawApproval(String id, IServiceContext context) {
        ErpPurOrder order = requireOrder(id, context);
        validateNotCancelled(order, context);
        validateTransitionForWithdraw(order, context);
        doWithdrawSubmit(order, context);
        return order;
    }

    public ErpPurOrder approve(String id, IServiceContext context) {
        ErpPurOrder order = requireOrder(id, context);
        if (order.isApproved()) {
            return order;
        }
        validateNotCancelled(order, context);
        validateTransitionForApprove(order, context);
        validateBusinessRulesForApprove(order, context);
        doApprove(order, context);
        return order;
    }

    public ErpPurOrder reject(String id, IServiceContext context) {
        ErpPurOrder order = requireOrder(id, context);
        validateNotCancelled(order, context);
        validateTransitionForReject(order, context);
        doReject(order, context);
        return order;
    }

    public ErpPurOrder reverseApprove(String id, IServiceContext context) {
        ErpPurOrder order = requireOrder(id, context);
        if (order.isRejected()) {
            return order;
        }
        validateTransitionForReverseApprove(order, context);
        doReverseApprove(order, context);
        return order;
    }

    public ErpPurOrder cancel(String orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        validateTransitionForCancel(order, context);
        doCancel(order, context);
        return order;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpPurOrder order, IServiceContext context) {
        validateNotCancelled(order, context);
        String status = order.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(order, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurOrder order, IServiceContext context) {
        String docStatus = order.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurOrder order, IServiceContext context) {
        requireLinesNonEmpty(order, context);
        requireSupplierActive(order, context);
    }

    protected void validateBusinessRulesForApprove(ErpPurOrder order, IServiceContext context) {
        requireSupplierActive(order, context);
        runBudgetCheckHook(order, context);
    }

    /**
     * 预算控制钩子（budget.md §业务规则2/8）。经 {@code erp-fin.budget-check-enabled} 门控（默认 false，向后兼容）。
     * 采购订单无科目维度，按 {@code erp-fin.budget-purchase-expense-subject-code} 配置的默认采购费用科目、
     * 按订单业务日期解析的会计期间，对订单含税合计做预算余量校验。科目/期间未配置时静默跳过。
     */
    protected void runBudgetCheckHook(ErpPurOrder order, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CHECK_ENABLED, Boolean.FALSE))) {
            return;
        }
        Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_PURCHASE_EXPENSE_SUBJECT_CODE);
        if (subjectId == null) {
            return;
        }
        Long periodId = resolvePeriodId(order.getBusinessDate());
        BigDecimal amount = order.getTotalAmountWithTax() != null
                ? order.getTotalAmountWithTax() : BigDecimal.ZERO;
        budgetControlBiz.check(subjectId, null, periodId, amount, "PURCHASE_ORDER", order.getCode(), context);
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

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doWithdrawSubmit(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doApprove(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentTimestamp());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        order.setApprovedBy(null);
        order.setApprovedAt(null);
        orderDao().updateEntity(order);
    }

    protected void doCancel(ErpPurOrder order, IServiceContext context) {
        order.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        orderDao().updateEntity(order);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpPurOrder requireOrder(String id, IServiceContext context) {
        ErpPurOrder order = orderDao().getEntityById(id);
        if (order == null) {
            throw new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpPurErrors.ARG_ORDER_CODE, id);
        }
        return order;
    }

    protected void validateNotCancelled(ErpPurOrder order, IServiceContext context) {
        if (order.isCancelled()) {
            throw illegalDocTransition(order, order.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpPurOrder order, IServiceContext context) {
        if (order.getLines().isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_ORDER_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode());
        }
    }

    protected void requireSupplierActive(ErpPurOrder order, IServiceContext context) {
        if (order.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(order.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpPurConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, order.getSupplierId());
        }
    }

    /**
     * 通过 ORM to-many 关系 {@code ErpPurOrder.lines} 加载行（懒加载，复用主实体 session）。
     * 取代 {@code daoFor(ErpPurOrderLine.class).findAllByQuery(eq("orderId", ...))}——
     * 关系已在 {@code app-erp-purchase.orm.xml} 声明，无需重复查询。
     */
    protected List<ErpPurOrderLine> loadLines(ErpPurOrder order) {
        return new ArrayList<>(order.getLines());
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpPurOrder> orderDao() {
        return daoProvider.daoFor(ErpPurOrder.class);
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

    protected NopException illegalTransition(ErpPurOrder order, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurOrder order, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
