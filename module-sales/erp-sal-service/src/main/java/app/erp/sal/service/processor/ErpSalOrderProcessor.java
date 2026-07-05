package app.erp.sal.service.processor;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.CreditLimitChecker;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
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
 * 销售订单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpSalOrderProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    CreditLimitChecker creditLimitChecker;

    public ErpSalOrder submitForApproval(String id, IServiceContext context) {
        ErpSalOrder order = requireOrder(id, context);
        validateNotCancelled(order, context);
        validateTransitionForSubmit(order, context);
        validateBusinessRulesForSubmit(order, context);
        doSubmit(order, context);
        return order;
    }

    public ErpSalOrder withdrawApproval(String id, IServiceContext context) {
        ErpSalOrder order = requireOrder(id, context);
        validateNotCancelled(order, context);
        validateTransitionForWithdraw(order, context);
        doWithdrawSubmit(order, context);
        return order;
    }

    public ErpSalOrder approve(String id, IServiceContext context) {
        ErpSalOrder order = requireOrder(id, context);
        if (isAlreadyApproved(order)) {
            return order;
        }
        validateNotCancelled(order, context);
        validateTransitionForApprove(order, context);
        validateBusinessRulesForApprove(order, context);
        doApprove(order, context);
        return order;
    }

    public ErpSalOrder reject(String id, IServiceContext context) {
        ErpSalOrder order = requireOrder(id, context);
        validateNotCancelled(order, context);
        validateTransitionForReject(order, context);
        doReject(order, context);
        return order;
    }

    public ErpSalOrder reverseApprove(String id, IServiceContext context) {
        ErpSalOrder order = requireOrder(id, context);
        if (isAlreadyRejected(order)) {
            return order;
        }
        validateTransitionForReverseApprove(order, context);
        doReverseApprove(order, context);
        return order;
    }

    public ErpSalOrder cancel(String orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId, context);
        validateTransitionForCancel(order, context);
        doCancel(order, context);
        return order;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(order, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalOrder order, IServiceContext context) {
        String docStatus = order.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalOrder order, IServiceContext context) {
        requireLinesNonEmpty(order, context);
        requireCustomerActive(order, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalOrder order, IServiceContext context) {
        requireCustomerActive(order, context);
        creditLimitChecker.check(order.getCustomerId(), order.getTotalAmountWithTax(), order.getExchangeRate(), context);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doWithdrawSubmit(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doApprove(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentDateTime());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        order.setApprovedBy(null);
        order.setApprovedAt(null);
        orderDao().updateEntity(order);
    }

    protected void doCancel(ErpSalOrder order, IServiceContext context) {
        order.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        orderDao().updateEntity(order);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalOrder requireOrder(String id, IServiceContext context) {
        ErpSalOrder order = orderDao().getEntityById(id);
        if (order == null) {
            throw new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpSalErrors.ARG_ORDER_CODE, id);
        }
        return order;
    }

    protected void validateNotCancelled(ErpSalOrder order, IServiceContext context) {
        String docStatus = order.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalOrder order) {
        String status = order.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpSalOrder order) {
        String status = order.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpSalOrder order, IServiceContext context) {
        if (loadLines(order.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_ORDER_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalOrder order, IServiceContext context) {
        if (order.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(order.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, order.getCustomerId());
        }
    }

    protected List<ErpSalOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalOrder> orderDao() {
        return daoProvider.daoFor(ErpSalOrder.class);
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

    protected NopException illegalTransition(ErpSalOrder order, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalOrder order, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
