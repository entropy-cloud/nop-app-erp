package app.erp.pur.service.processor;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购订单审批状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpPurOrderBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个 {@code public} 动作方法只编排步骤顺序（加载→校验迁移→校验业务规则→执行），
 * 各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。客户/行业覆盖单步实现时，
 * 写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpPurOrderProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    public ErpPurOrder submit(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        validateTransitionForSubmit(order, context);
        validateBusinessRulesForSubmit(order, context);
        doSubmit(order, context);
        return order;
    }

    public ErpPurOrder withdrawSubmit(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        validateNotCancelled(order, context);
        validateTransitionForWithdraw(order, context);
        doWithdrawSubmit(order, context);
        return order;
    }

    public ErpPurOrder approve(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        // 幂等：已审核订单再次审核为空操作（state-machine §4，订单无库存触发故无副作用可重复）。
        if (isAlreadyApproved(order)) {
            return order;
        }
        validateNotCancelled(order, context);
        validateTransitionForApprove(order, context);
        validateBusinessRulesForApprove(order, context);
        doApprove(order, context);
        return order;
    }

    public ErpPurOrder reject(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        validateNotCancelled(order, context);
        validateTransitionForReject(order, context);
        doReject(order, context);
        return order;
    }

    public ErpPurOrder reverseApprove(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        // 幂等：已 REJECTED 无更多可反审核，直接返回。
        if (isAlreadyRejected(order)) {
            return order;
        }
        validateTransitionForReverseApprove(order, context);
        doReverseApprove(order, context);
        return order;
    }

    public ErpPurOrder cancel(Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        validateTransitionForCancel(order, context);
        doCancel(order, context);
        return order;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpPurOrder order, IServiceContext context) {
        validateNotCancelled(order, context);
        Integer status = order.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurOrder order, IServiceContext context) {
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurOrder order, IServiceContext context) {
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurOrder order, IServiceContext context) {
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurOrder order, IServiceContext context) {
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(order, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurOrder order, IServiceContext context) {
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
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
        order.setApprovedAt(CoreMetrics.currentDateTime());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpPurOrder order, IServiceContext context) {
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doCancel(ErpPurOrder order, IServiceContext context) {
        order.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        orderDao().updateEntity(order);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpPurOrder requireOrder(Long orderId, IServiceContext context) {
        ErpPurOrder order = orderDao().getEntityById(orderId);
        if (order == null) {
            throw new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpPurErrors.ARG_ORDER_CODE, String.valueOf(orderId));
        }
        return order;
    }

    protected void validateNotCancelled(ErpPurOrder order, IServiceContext context) {
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpPurOrder order) {
        Integer status = order.getApproveStatus();
        return status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED;
    }

    protected boolean isAlreadyRejected(ErpPurOrder order) {
        Integer status = order.getApproveStatus();
        return status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED;
    }

    protected void requireLinesNonEmpty(ErpPurOrder order, IServiceContext context) {
        if (loadLines(order.getId()).isEmpty()) {
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
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, order.getSupplierId());
        }
    }

    protected List<ErpPurOrderLine> loadLines(Long orderId) {
        // D2 边界场景：同聚合子表加载，父实体已授权，子行无独立权限规则。
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
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

    protected NopException illegalTransition(ErpPurOrder order, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurOrder order, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
