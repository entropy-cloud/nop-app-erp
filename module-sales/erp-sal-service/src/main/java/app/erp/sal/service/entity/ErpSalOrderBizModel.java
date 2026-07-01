
package app.erp.sal.service.entity;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.annotations.core.Name;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售订单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/state-machine.md} §2「销售订单｜仅状态推进」，不触发库存/凭证）+ 客户启用校验 +
 * 客户信用额度校验（对齐 {@code docs/design/sales/README.md} §信用额度控制）。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>单据轴：任意非终态→docStatus=CANCELLED（订单审核未触发库存/凭证，故作废无冲销前置）。</li>
 *   <li>{@link #approve} 仅状态推进——落地 {@code approvedBy}/{@code approvedAt} + 客户启用校验 +
 *       {@link CreditLimitChecker#check}（按 {@code erp-sal.credit-check-level} 三级策略）。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpSalOrder")
public class ErpSalOrderBizModel extends CrudBizModel<ErpSalOrder> implements IErpSalOrderBiz {

    @Inject
    CreditLimitChecker creditLimitChecker;

    public ErpSalOrderBizModel() {
        setEntityName(ErpSalOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalOrder submit(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(order);
        requireCustomerActive(order);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpSalOrder withdrawSubmit(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpSalOrder approve(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        Integer status = order.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4）。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return order;
        }
        requireNotCancelled(order);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        requireCustomerActive(order);
        // 信用额度校验（本单此时仍为 SUBMITTED，不在 outstanding 内，不会被重复计算）。
        creditLimitChecker.check(order.getCustomerId(), order.getTotalAmountWithTax());
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentDateTime());
        // 订单审核 = 纯状态推进（state-machine §2），不触发库存/凭证。
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpSalOrder reject(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpSalOrder reverseApprove(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        Integer status = order.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return order;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(order, status, "APPROVED");
        }
        // 订单审核未触发库存/凭证，反审核 = 纯状态迁移（无冲销前置，与出库单不同）。
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpSalOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {
        ErpSalOrder order = requireOrder(orderId);
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
        // 订单作废无冲销前置（订单审核未触发库存；已有下游出库单的订单作废影响属 1.7/1.10 范畴）。
        order.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(order);
        return order;
    }

    // ---------- validation helpers ----------

    private ErpSalOrder requireOrder(Long orderId) {
        ErpSalOrder order = dao().getEntityById(orderId);
        if (order == null) {
            throw new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpSalErrors.ARG_ORDER_ID, orderId);
        }
        return order;
    }

    private void requireNotCancelled(ErpSalOrder order) {
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalOrder order) {
        if (loadLines(order.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_ORDER_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode());
        }
    }

    private void requireCustomerActive(ErpSalOrder order) {
        if (order.getCustomerId() == null) {
            return;
        }
        // 客户启用校验为纯实体读（master-data 机制 B），故用 daoFor 直接加载。
        ErpMdPartner partner = daoFor(ErpMdPartner.class).getEntityById(order.getCustomerId());
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, order.getCustomerId());
        }
    }

    // ---------- query helpers ----------

    List<ErpSalOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpSalOrderLine> dao = daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
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

    private NopException illegalTransition(ErpSalOrder order, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalOrder order, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
