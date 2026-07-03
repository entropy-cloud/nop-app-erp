package app.erp.sal.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.DeliveryStockMoveBuilder;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售出库单审批状态机 + 出库审核触发库存移动编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpSalDeliveryBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>跨实体：客户启用校验经 {@link IErpMdPartnerBiz}；发货进度回写经 {@link IErpSalOrderBiz}；
 * 出库移动单经 {@link IErpInvStockMoveBiz}；强制质检经 {@link IErpQaInspectionBiz}。
 */
public class ErpSalDeliveryProcessor {

    @Inject
    IDaoProvider daoProvider;

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

    public ErpSalDelivery submit(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        validateTransitionForSubmit(delivery, context);
        validateBusinessRulesForSubmit(delivery, context);
        doSubmit(delivery, context);
        return delivery;
    }

    public ErpSalDelivery withdrawSubmit(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        validateNotCancelled(delivery, context);
        validateTransitionForWithdraw(delivery, context);
        doWithdrawSubmit(delivery, context);
        return delivery;
    }

    public ErpSalDelivery approve(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        // 幂等：已审核单据再次审核为空操作（state-machine §4），出库移动单已存在，不重复触发。
        if (isAlreadyApproved(delivery)) {
            return delivery;
        }
        validateNotCancelled(delivery, context);
        validateTransitionForApprove(delivery, context);
        validateBusinessRulesForApprove(delivery, context);
        enforceInspectionGate(delivery, context);

        ErpInvStockMove move = triggerOutgoingMove(delivery, context);
        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        delivery = deliveryDao().getEntityById(deliveryId);
        doApprove(delivery, move, context);
        postProcessApprove(delivery, context);
        return delivery;
    }

    public ErpSalDelivery reject(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        validateNotCancelled(delivery, context);
        validateTransitionForReject(delivery, context);
        doReject(delivery, context);
        return delivery;
    }

    public ErpSalDelivery reverseApprove(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (isAlreadyRejected(delivery)) {
            return delivery;
        }
        validateTransitionForReverseApprove(delivery, context);
        ensureReversed(delivery, context);
        delivery = deliveryDao().getEntityById(deliveryId);
        doReverseApprove(delivery, context);
        return delivery;
    }

    public ErpSalDelivery cancel(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        validateTransitionForCancel(delivery, context);
        if (isApproved(delivery)) {
            ensureReversed(delivery, context);
            delivery = deliveryDao().getEntityById(deliveryId);
        }
        doCancel(delivery, context);
        return delivery;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpSalDelivery delivery, IServiceContext context) {
        validateNotCancelled(delivery, context);
        Integer status = delivery.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(delivery, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalDelivery delivery, IServiceContext context) {
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalDelivery delivery, IServiceContext context) {
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalDelivery delivery, IServiceContext context) {
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalDelivery delivery, IServiceContext context) {
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(delivery, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalDelivery delivery, IServiceContext context) {
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalDelivery delivery, IServiceContext context) {
        requireLinesNonEmpty(delivery, context);
        requireCustomerActive(delivery, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalDelivery delivery, IServiceContext context) {
        requireCustomerActive(delivery, context);
    }

    // ---------- step：执行 ----------

    protected void doSubmit(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doWithdrawSubmit(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doApprove(ErpSalDelivery delivery, ErpInvStockMove move, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setApprovedBy(currentUserId());
        delivery.setApprovedAt(CoreMetrics.currentDateTime());
        applyPostingResult(delivery, move);
        deliveryDao().updateEntity(delivery);
    }

    protected void doReject(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doReverseApprove(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doCancel(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        deliveryDao().updateEntity(delivery);
    }

    protected void postProcessApprove(ErpSalDelivery currentDelivery, IServiceContext context) {
        rollupOrderDeliveryStatus(currentDelivery, context);
    }

    // ---------- 出库触发 + 过账接线 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(OUTGOING) 调 {@link IErpInvStockMoveBiz#generateMove}。
     * 出库类在库存域 CONFIRM 校验可用量，不足抛 {@link NopException} 致整个出库单审核回滚。
     */
    protected ErpInvStockMove triggerOutgoingMove(ErpSalDelivery delivery, IServiceContext context) {
        List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());
        StockMoveRequest request = stockMoveBuilder.build(delivery, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 将移动单过账结果接线到出库单：{@code delivery.posted = move.posted}、{@code postedAt}/{@code postedBy} 落地。
     */
    protected void applyPostingResult(ErpSalDelivery delivery, ErpInvStockMove move) {
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
    protected void ensureReversed(ErpSalDelivery delivery, IServiceContext context) {
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
    protected void rollupOrderDeliveryStatus(ErpSalDelivery currentDelivery, IServiceContext context) {
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
        for (ErpSalDelivery d : findApprovedDeliveries(orderId)) {
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

    /**
     * 强制质检门控（config-gated，默认空=不强制）。属强制类型时按出库单行物料逐行触发：首次生成 PENDING 质检单并阻塞，
     * 质检合格/让步后再次审核放行。billType=ERP_SAL_DELIVERY，inspectionType=OUTGOING。
     */
    protected void enforceInspectionGate(ErpSalDelivery delivery, IServiceContext context) {
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

    // ---------- 校验/查询辅助 ----------

    protected ErpSalDelivery requireDelivery(Long deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = deliveryDao().getEntityById(deliveryId);
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                    .param(ErpSalErrors.ARG_DELIVERY_ID, deliveryId);
        }
        return delivery;
    }

    protected void validateNotCancelled(ErpSalDelivery delivery, IServiceContext context) {
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalDelivery delivery) {
        Integer status = delivery.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    protected boolean isApproved(ErpSalDelivery delivery) {
        return isAlreadyApproved(delivery);
    }

    protected boolean isAlreadyRejected(ErpSalDelivery delivery) {
        Integer status = delivery.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED;
    }

    protected void requireLinesNonEmpty(ErpSalDelivery delivery, IServiceContext context) {
        if (loadLines(delivery.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalDelivery delivery, IServiceContext context) {
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

    protected List<ErpSalDeliveryLine> loadLines(Long deliveryId) {
        // D2 边界场景：同聚合子表加载，父实体已授权，子行无独立权限规则。
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("deliveryId", deliveryId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected List<ErpSalOrderLine> loadOrderLines(Long orderId) {
        // D2 边界场景：跨聚合只读加载销售订单行（进度回写用），订单聚合经 orderBiz 跨聚合写时已校验存在性。
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected List<ErpSalDelivery> findApprovedDeliveries(Long orderId) {
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("orderId", orderId), eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED)));
        return new ArrayList<>(deliveryDao().findAllByQuery(rq));
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

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalDelivery> deliveryDao() {
        return daoProvider.daoFor(ErpSalDelivery.class);
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

    protected NopException illegalTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
