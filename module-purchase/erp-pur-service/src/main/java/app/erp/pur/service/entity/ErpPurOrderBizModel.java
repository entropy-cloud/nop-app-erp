
package app.erp.pur.service.entity;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 采购订单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机
 * （对齐 {@code docs/design/purchase/state-machine.md} §2「采购订单｜仅状态推进」）+ 供应商启用校验。
 *
 * <p><b>订单审核 = 纯状态推进，不触发库存/凭证</b>（订单是意向，下游单据才触发）——与入库单审核触发
 * {@code generateMove} 实质性不同，故 {@link #approve} 不含跨域调用。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：供应商启用校验经 {@link IErpMdPartnerBiz}；
 * 请购→订单转化、收货进度回写由本接口的 {@link #createFromRequisition}/{@link #updateReceiveStatus}
 * 承接（操作自身聚合 {@link ErpPurOrder}/行）。
 */
@BizModel("ErpPurOrder")
public class ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> implements IErpPurOrderBiz {

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    RequisitionToOrderConverter converter;

    public ErpPurOrderBizModel() {
        setEntityName(ErpPurOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurOrder submit(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
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
        requireSupplierActive(order, context);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpPurOrder withdrawSubmit(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpPurOrder approve(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        Integer status = order.getApproveStatus();
        // 幂等：已审核订单再次审核为空操作（state-machine §4，订单无库存触发故无副作用可重复）。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return order;
        }
        requireNotCancelled(order);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        requireSupplierActive(order, context);
        // 订单审核 = 纯状态推进（state-machine §2），不触发库存/凭证（下游入库单才触发）。
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentDateTime());
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpPurOrder reject(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        requireNotCancelled(order);
        Integer status = order.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(order);
        return order;
    }

    @Override
    @BizMutation
    public ErpPurOrder reverseApprove(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
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

    @Override
    @BizMutation
    public ErpPurOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {
        ErpPurOrder order = requireOrder(orderId, context);
        Integer docStatus = order.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
        // 订单作废仅状态迁移（无库存冲销前置——订单审核未触发库存）。下游入库单影响属 1.4/1.6。
        order.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(order);
        return order;
    }

    // ---------- 跨聚合写契约（请购→订单转化、收货进度回写） ----------

    @Override
    @BizAction
    public ErpPurOrder createFromRequisition(@Name("requisition") ErpPurRequisition requisition,
                                             @Name("lines") List<ErpPurRequisitionLine> lines,
                                             @Name("supplierId") Long supplierId,
                                             @Name("request") ConvertToOrderRequest request,
                                             IServiceContext context) {
        ErpPurOrder order = converter.build(requisition, lines, supplierId, request);
        dao().saveEntity(order);
        // flush 使订单 ID 落地，再保存行（行 orderId 依赖头 ID）
        ormTemplate.flushSession();
        for (ErpPurOrderLine orderLine : converter.buildLines(order, lines, request)) {
            daoFor(ErpPurOrderLine.class).saveEntity(orderLine);
        }
        return order;
    }

    @Override
    @BizAction
    public boolean existsActiveByRequisition(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        if (requisitionId == null) {
            return false;
        }
        ormTemplate.flushSession();
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("requisitionId", requisitionId),
                ne("docStatus", ErpPurConstants.DOC_STATUS_CANCELLED)));
        return !dao().findAllByQuery(q).isEmpty();
    }

    @Override
    @BizAction
    public void updateReceiveStatus(@Name("orderId") Long orderId,
                                    @Name("receiveStatus") Integer receiveStatus,
                                    IServiceContext context) {
        if (orderId == null) {
            return;
        }
        ErpPurOrder order = dao().getEntityById(orderId);
        if (order == null) {
            return;
        }
        order.setReceiveStatus(receiveStatus);
        dao().updateEntity(order);
    }

    // ---------- validation helpers ----------

    ErpPurOrder requireOrder(Long orderId, IServiceContext context) {
        return requireEntity(String.valueOf(orderId), null, context);
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

    void requireSupplierActive(ErpPurOrder order, IServiceContext context) {
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

    // ---------- query helpers ----------

    List<ErpPurOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoFor(ErpPurOrderLine.class);
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
