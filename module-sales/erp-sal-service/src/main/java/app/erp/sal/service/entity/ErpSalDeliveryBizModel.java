
package app.erp.sal.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售出库单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/state-machine.md}，与采购域镜像对称）+ 出库审核触发库存移动（对齐
 * {@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约 + 销售独有可用量校验）。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：客户启用校验经 {@link IErpMdPartnerBiz}；
 * 发货进度回写源订单经 {@link IErpSalOrderBiz}；冲销时定位出库移动单经 {@link IErpInvStockMoveBiz}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpSalDelivery")
public class ErpSalDeliveryBizModel extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    DeliveryStockMoveBuilder stockMoveBuilder;

    @Inject
    IErpSalOrderBiz orderBiz;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpQaInspectionBiz inspectionBiz;

    public ErpSalDeliveryBizModel() {
        setEntityName(ErpSalDelivery.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalDelivery submit(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(delivery, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(delivery);
        requireCustomerActive(delivery, context);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @Override
    @BizMutation
    public ErpSalDelivery withdrawSubmit(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @Override
    @BizMutation
    public ErpSalDelivery approve(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        Integer status = delivery.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），出库移动单已存在，不重复触发。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return delivery;
        }
        requireNotCancelled(delivery);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        requireCustomerActive(delivery, context);

        // 强制质检门控（plan 2026-07-02-2237-3 Phase 2）：erp-qua.mandatory-inspection-bill-types 配置门控，
        // 默认空=不强制。属强制类型时：首次审核生成 PENDING 质检单并阻塞，质检合格/让步后再次审核放行。
        enforceInspectionGate(delivery, context);

        ErpInvStockMove move = triggerOutgoingMove(delivery, context);
        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        delivery = requireEntity(String.valueOf(deliveryId), null, context);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setApprovedBy(currentUserId());
        delivery.setApprovedAt(CoreMetrics.currentDateTime());
        applyPostingResult(delivery, move);
        dao().updateEntity(delivery);
        rollupOrderDeliveryStatus(delivery, context);
        return delivery;
    }

    @Override
    @BizMutation
    public ErpSalDelivery reject(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @Override
    @BizMutation
    public ErpSalDelivery reverseApprove(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        Integer status = delivery.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return delivery;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(delivery, status, "APPROVED");
        }
        ensureReversed(delivery, context);
        delivery = requireEntity(String.valueOf(deliveryId), null, context);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @Override
    @BizMutation
    public ErpSalDelivery cancel(@Name("deliveryId") Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
        Integer approveStatus = delivery.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            ensureReversed(delivery, context);
            delivery = requireEntity(String.valueOf(deliveryId), null, context);
        }
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(delivery);
        return delivery;
    }

    // ---------- Phase 2: 出库触发 + 过账接线 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(OUTGOING) 调 {@link IErpInvStockMoveBiz#generateMove}。
     * 出库类在库存域 CONFIRM 校验可用量（{@code availableQuantity ≥ quantity}），不足抛 {@link NopException}
     * 致整个出库单审核回滚（对齐 state-machine §4 销售独有 + cross-domain「不足拒绝+审核回滚」）。
     */
    ErpInvStockMove triggerOutgoingMove(ErpSalDelivery delivery, IServiceContext context) {
        List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());
        StockMoveRequest request = stockMoveBuilder.build(delivery, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 将移动单过账结果接线到出库单：{@code delivery.posted = move.posted}、{@code postedAt}/{@code postedBy} 落地。
     */
    private void applyPostingResult(ErpSalDelivery delivery, ErpInvStockMove move) {
        delivery.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(delivery.getPosted())) {
            delivery.setPostedAt(CoreMetrics.currentDateTime());
            delivery.setPostedBy(currentUserId());
        }
    }

    /**
     * 反审核/作废前的内部冲销（Design A）：经 {@link IErpInvStockMoveBiz} 定位原出库移动单与既有冲销单，
     * 不存在冲销单则调 {@link IErpInvStockMoveBiz#reverse}。
     */
    void ensureReversed(ErpSalDelivery delivery, IServiceContext context) {
        ErpInvStockMove original = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode(), context);
        if (original == null) {
            throw new NopException(ErpSalErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
        }
        ErpInvStockMove existingReversal = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode(), context);
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    /**
     * 回写源订单 {@code deliveryStatus}：按「累计已发 / 订单数量」按行比较，全发清→DELIVERED、
     * 部分发→PARTIAL、未发→UNDELIVERED。进度回写经 {@link IErpSalOrderBiz}（跨聚合写）。
     */
    void rollupOrderDeliveryStatus(ErpSalDelivery currentDelivery, IServiceContext context) {
        Long orderId = currentDelivery.getOrderId();
        if (orderId == null) {
            return;
        }
        List<ErpSalOrderLine> orderLines = loadOrderLines(orderId);
        if (orderLines.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> deliveredByOrderLine = new HashMap<>();
        addLineQuantities(deliveredByOrderLine, loadLines(currentDelivery.getId()));
        for (ErpSalDelivery d : findApprovedDeliveries(orderId, context)) {
            if (d.getId().equals(currentDelivery.getId())) {
                continue;
            }
            addLineQuantities(deliveredByOrderLine, loadLines(d.getId()));
        }

        boolean anyDelivered = false;
        boolean allFullyDelivered = true;
        for (ErpSalOrderLine ol : orderLines) {
            BigDecimal ordered = ol.getQuantity() == null ? BigDecimal.ZERO : ol.getQuantity();
            BigDecimal delivered = deliveredByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);
            if (delivered.signum() > 0) {
                anyDelivered = true;
            }
            if (delivered.compareTo(ordered) < 0) {
                allFullyDelivered = false;
            }
        }
        int rolled;
        if (allFullyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_DELIVERED;
        } else if (anyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_PARTIAL;
        } else {
            rolled = ErpSalConstants.DELIVERY_STATUS_UNDELIVERED;
        }
        orderBiz.updateDeliveryStatus(orderId, rolled, context);
    }

    private void addLineQuantities(Map<Long, BigDecimal> map, List<ErpSalDeliveryLine> lines) {
        for (ErpSalDeliveryLine dl : lines) {
            if (dl.getOrderLineId() == null) {
                continue;
            }
            BigDecimal qty = dl.getQuantity() == null ? BigDecimal.ZERO : dl.getQuantity();
            map.merge(dl.getOrderLineId(), qty, BigDecimal::add);
        }
    }

    private List<ErpSalDelivery> findApprovedDeliveries(Long orderId, IServiceContext context) {
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("orderId", orderId), eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED)));
        return findList(rq, null, context);
    }

    // ---------- validation helpers ----------

    private ErpSalDelivery requireDelivery(Long deliveryId, IServiceContext context) {
        return requireEntity(String.valueOf(deliveryId), null, context);
    }

    private void requireNotCancelled(ErpSalDelivery delivery) {
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalDelivery delivery) {
        if (loadLines(delivery.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
        }
    }

    private void requireCustomerActive(ErpSalDelivery delivery, IServiceContext context) {
        if (delivery.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(delivery.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, delivery.getCustomerId());
        }
    }

    // ---------- query helpers ----------

    /**
     * 强制质检门控（config-gated，默认空=不强制）。属强制类型时按出库单行物料逐行触发：首次生成 PENDING 质检单并阻塞，
     * 质检合格/让步后再次审核放行。billType=ERP_SAL_DELIVERY，inspectionType=OUTGOING。
     */
    private void enforceInspectionGate(ErpSalDelivery delivery, IServiceContext context) {
        String billType = ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY;
        if (!InspectionTrigger.isMandatoryBillType(billType)) {
            return;
        }
        for (ErpSalDeliveryLine line : loadLines(delivery.getId())) {
            if (line.getMaterialId() == null) {
                continue;
            }
            int gate = InspectionTrigger.enforceGate(inspectionBiz, billType, delivery.getCode(),
                    line.getMaterialId(), 40 /* erp-qa/inspection-type OUTGOING */,
                    line.getQuantity(), null, delivery.getWarehouseId(), null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpSalErrors.ERR_DELIVERY_INSPECTION_BLOCKED)
                        .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
            }
        }
    }

    List<ErpSalDeliveryLine> loadLines(Long deliveryId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpSalDeliveryLine> dao = daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("deliveryId", deliveryId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private List<ErpSalOrderLine> loadOrderLines(Long orderId) {
        // D2 边界场景：跨聚合只读加载销售订单行（进度回写用），订单聚合经 orderBiz 跨聚合写时已校验存在性。
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

    private NopException illegalTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
