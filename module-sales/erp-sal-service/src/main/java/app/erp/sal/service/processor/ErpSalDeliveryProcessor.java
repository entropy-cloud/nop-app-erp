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
import app.erp.sal.service.entity.CreditLimitChecker;
import app.erp.sal.service.entity.DeliveryStockMoveBuilder;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售出库单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
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

    @Inject
    CreditLimitChecker creditLimitChecker;

    public ErpSalDelivery submitForApproval(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(id, context);
        validateNotCancelled(delivery, context);
        validateTransitionForSubmit(delivery, context);
        validateBusinessRulesForSubmit(delivery, context);
        doSubmit(delivery, context);
        return delivery;
    }

    public ErpSalDelivery withdrawApproval(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(id, context);
        validateNotCancelled(delivery, context);
        validateTransitionForWithdraw(delivery, context);
        doWithdrawSubmit(delivery, context);
        return delivery;
    }

    public ErpSalDelivery approve(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(id, context);
        if (isAlreadyApproved(delivery)) {
            return delivery;
        }
        validateNotCancelled(delivery, context);
        validateTransitionForApprove(delivery, context);
        validateBusinessRulesForApprove(delivery, context);
        enforceInspectionGate(delivery, context);
        doApprove(delivery, context);
        return delivery;
    }

    public ErpSalDelivery reject(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(id, context);
        validateNotCancelled(delivery, context);
        validateTransitionForReject(delivery, context);
        doReject(delivery, context);
        return delivery;
    }

    public ErpSalDelivery reverseApprove(String id, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(id, context);
        if (isAlreadyRejected(delivery)) {
            return delivery;
        }
        validateTransitionForReverseApprove(delivery, context);
        doReverseApprove(delivery, context);
        return delivery;
    }

    public ErpSalDelivery cancel(String deliveryId, IServiceContext context) {
        ErpSalDelivery delivery = requireDelivery(deliveryId, context);
        validateTransitionForCancel(delivery, context);
        if (isApproved(delivery)) {
            ensureReversed(delivery, context);
            delivery = deliveryDao().getEntityById(deliveryId);
        }
        doCancel(delivery, context);
        return delivery;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalDelivery delivery, IServiceContext context) {
        String status = delivery.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(delivery, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalDelivery delivery, IServiceContext context) {
        String status = delivery.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalDelivery delivery, IServiceContext context) {
        String status = delivery.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalDelivery delivery, IServiceContext context) {
        String status = delivery.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalDelivery delivery, IServiceContext context) {
        String status = delivery.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(delivery, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalDelivery delivery, IServiceContext context) {
        String docStatus = delivery.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
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
        enforceCreditHold(delivery, context);
    }

    /**
     * 出库审核信用冻结检查（config-gated by {@code erp-sal.credit-check-on-delivery}，默认 false 向后兼容）。
     *
     * <p>检查客户当前信用状况是否已超额（订单审核时额度已被占用，此处为 point-in-time hold）。
     * 详见 {@link CreditLimitChecker#checkCreditHold}。
     */
    protected void enforceCreditHold(ErpSalDelivery delivery, IServiceContext context) {
        if (!AppConfig.var(ErpSalConstants.CONFIG_CREDIT_CHECK_ON_DELIVERY,
                ErpSalConstants.CREDIT_CHECK_ON_DELIVERY_DEFAULT)) {
            return;
        }
        creditLimitChecker.checkCreditHold(delivery.getCustomerId(), delivery.getCode(),
                ErpSalConstants.BILL_TYPE_DELIVERY, context);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doWithdrawSubmit(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doApprove(ErpSalDelivery delivery, IServiceContext context) {
        ErpInvStockMove move = triggerOutgoingMove(delivery, context);
        applyPostingResult(delivery, move);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setApprovedBy(currentUserId());
        delivery.setApprovedAt(CoreMetrics.currentDateTime());
        deliveryDao().updateEntity(delivery);
        postProcessApprove(delivery, context);
    }

    protected void doReject(ErpSalDelivery delivery, IServiceContext context) {
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        deliveryDao().updateEntity(delivery);
    }

    protected void doReverseApprove(ErpSalDelivery delivery, IServiceContext context) {
        ensureReversed(delivery, context);
        delivery = deliveryDao().getEntityById(delivery.getId());
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        delivery.setApprovedBy(null);
        delivery.setApprovedAt(null);
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

    protected ErpInvStockMove triggerOutgoingMove(ErpSalDelivery delivery, IServiceContext context) {
        List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());
        StockMoveRequest request = stockMoveBuilder.build(delivery, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    protected void applyPostingResult(ErpSalDelivery delivery, ErpInvStockMove move) {
        delivery.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(delivery.getPosted())) {
            delivery.setPostedAt(CoreMetrics.currentDateTime());
            delivery.setPostedBy(currentUserId());
        }
    }

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
        String rolled;
        if (allFullyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_DELIVERED;
        } else if (anyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_PARTIAL;
        } else {
            rolled = ErpSalConstants.DELIVERY_STATUS_UNDELIVERED;
        }
        orderBiz.updateDeliveryStatus(orderId, rolled, context);
    }

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
                    line.getMaterialId(), "OUTGOING",
                    line.getQuantity(), null, delivery.getWarehouseId(), null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpSalErrors.ERR_DELIVERY_INSPECTION_BLOCKED)
                        .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
            }
        }
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalDelivery requireDelivery(String id, IServiceContext context) {
        ErpSalDelivery delivery = deliveryDao().getEntityById(id);
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, id);
        }
        return delivery;
    }

    protected void validateNotCancelled(ErpSalDelivery delivery, IServiceContext context) {
        String docStatus = delivery.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
    }

    protected boolean isApproved(ErpSalDelivery delivery) {
        String status = delivery.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyApproved(ErpSalDelivery delivery) {
        return isApproved(delivery);
    }

    protected boolean isAlreadyRejected(ErpSalDelivery delivery) {
        String status = delivery.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED);
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
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, delivery.getCustomerId());
        }
    }

    protected List<ErpSalDeliveryLine> loadLines(Long deliveryId) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("deliveryId", deliveryId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected List<ErpSalOrderLine> loadOrderLines(Long orderId) {
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

    protected NopException illegalTransition(ErpSalDelivery delivery, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalDelivery delivery, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
