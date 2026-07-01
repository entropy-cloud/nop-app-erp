
package app.erp.pur.service.entity;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购订单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机
 * （对齐 {@code docs/design/purchase/state-machine.md} §2「采购订单｜仅状态推进」）+ 供应商启用校验。
 *
 * <p><b>订单审核 = 纯状态推进，不触发库存/凭证</b>（订单是意向，下游单据才触发）——与入库单审核触发
 * {@code generateMove} 实质性不同，故 {@link #approve} 不含跨域调用。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>单据轴：任意非终态→docStatus=CANCELLED。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 * {@code @SingleSession}+{@code @Transactional} 提供 ORM Session 与事务边界（同入库单基线）。
 */
@BizModel("ErpPurOrder")
public class ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> implements IErpPurOrderBiz {

    @Inject
    IOrmTemplate ormTemplate;

    public ErpPurOrderBizModel() {
        setEntityName(ErpPurOrder.class.getName());
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder submit(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(order);
        requireSupplierActive(order);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder withdrawSubmit(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder approve(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        Integer status = order.getApproveStatus();
        // 幂等：已审核订单再次审核为空操作（state-machine §4，订单无库存触发故无副作用可重复）。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return order;
        }
        requireNotCancelled(order);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        requireSupplierActive(order);
        // 订单审核 = 纯状态推进（state-machine §2），不触发库存/凭证（下游入库单才触发）。
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentDateTime());
        dao().updateEntity(order);
        return order;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder reject(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(order);
        return order;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder reverseApprove(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        Integer status = order.getApproveStatus();
        // 幂等：已 REJECTED 无更多可反审核，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return order;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(order, status, "APPROVED");
        }
        // 订单审核未触发库存，反审核仅状态迁移（无库存冲销前置——与入库单不同）。
        // 若订单已有下游入库单，作废/反审核订单的下游影响属 1.4/1.6 范畴，本计划不处理。
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(order);
        return order;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpPurOrder cancel(Long orderId) {
        ErpPurOrder order = requireOrder(orderId);
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
        // 订单作废仅状态迁移（无库存冲销前置——订单审核未触发库存）。下游入库单影响属 1.4/1.6。
        order.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(order);
        return order;
    }

    // ---------- validation helpers ----------

    ErpPurOrder requireOrder(Long orderId) {
        ErpPurOrder order = dao().getEntityById(orderId);
        if (order == null) {
            throw new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpPurErrors.ARG_ORDER_CODE, String.valueOf(orderId));
        }
        return order;
    }

    void requireNotCancelled(ErpPurOrder order) {
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    void requireLinesNonEmpty(ErpPurOrder order) {
        if (loadLines(order.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_ORDER_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode());
        }
    }

    void requireSupplierActive(ErpPurOrder order) {
        if (order.getSupplierId() == null) {
            return;
        }
        // 供应商启用校验为纯实体读（master-data 机制 B，无跨域 I*Biz 业务逻辑），故用 daoFor 直接加载。
        ErpMdPartner partner = daoFor(ErpMdPartner.class).getEntityById(order.getSupplierId());
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, order.getSupplierId());
        }
    }

    // ---------- query helpers ----------

    List<ErpPurOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoFor(ErpPurOrderLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
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

    private NopException illegalTransition(ErpPurOrder order, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurOrder order, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
