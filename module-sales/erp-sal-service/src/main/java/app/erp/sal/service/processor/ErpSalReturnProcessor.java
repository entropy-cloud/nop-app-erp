package app.erp.sal.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.SoDGuard;
import app.erp.sal.service.entity.ReturnQtyValidator;
import app.erp.sal.service.entity.ReturnRefundOrchestrator;
import app.erp.sal.service.entity.ReturnStockMoveBuilder;
import app.erp.sal.service.posting.SalReturnPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 销售退货单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 *
 * <p>跨实体：客户启用校验经 {@link IErpMdPartnerBiz}；源出库单经退货单 {@code delivery} 关系 getter；
 * 库存反向入库经 {@link IErpInvStockMoveBiz}；过账经 {@link SalReturnPostingDispatcher} →凭证聚合根 Facade；
 * 退款编排经 {@link ReturnRefundOrchestrator}。
 */
public class ErpSalReturnProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ReturnStockMoveBuilder stockMoveBuilder;

    @Inject
    ReturnQtyValidator returnQtyValidator;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    SalReturnPostingDispatcher postingDispatcher;

    @Inject
    ReturnRefundOrchestrator refundOrchestrator;

    @Inject
    ErpSalReturnSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpSalReturnApproveProcessor approveProcessor;

    @Inject
    ErpSalReturnRejectProcessor rejectProcessor;

    @Inject
    ErpSalReturnReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpSalReturnWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpSalReturnCancelProcessor cancelProcessor;

    public ErpSalReturn submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpSalReturn withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpSalReturn approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpSalReturn reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpSalReturn reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpSalReturn cancel(String returnId, IServiceContext context) {
        return cancelProcessor.cancel(returnId, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(returnOrder, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(returnOrder, status, ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void validateTransitionForCancel(ErpSalReturn returnOrder, IServiceContext context) {
        String docStatus = returnOrder.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        requireLinesNonEmpty(returnOrder, context);
        requireCustomerActive(returnOrder, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalReturn returnOrder, IServiceContext context) {
        requireCustomerActive(returnOrder, context);
        requireSourceDeliveryApproved(returnOrder, context);
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        requireReasonIfConfigured(returnOrder, lines, context);
        returnQtyValidator.validate(returnOrder, lines);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doWithdrawSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doApprove(ErpSalReturn returnOrder, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(returnOrder.getCreatedBy(), currentUserId(), ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR);
        triggerIncomingMove(returnOrder, context);
        ormTemplate.flushSession();

        boolean posted = triggerPosting(returnOrder, context);
        refundOrchestrator.orchestrateRefund(returnOrder);

        returnOrder = returnDao().getEntityById(returnOrder.getId());
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentTimestamp());
        applyPosted(returnOrder, posted);
        updateUndeliveredQuantity(returnOrder, context);
        returnDao().updateEntity(returnOrder);
    }

    protected void doReject(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doReverseApprove(ErpSalReturn returnOrder, IServiceContext context) {
        ensureReversed(returnOrder, context);
        returnOrder = returnDao().getEntityById(returnOrder.getId());
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        returnOrder.setApprovedBy(null);
        returnOrder.setApprovedAt(null);
        returnDao().updateEntity(returnOrder);
    }

    protected void doCancel(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        returnDao().updateEntity(returnOrder);
    }

    // ---------- 业务规则校验 ----------

    protected void requireSourceDeliveryApproved(ErpSalReturn returnOrder, IServiceContext context) {
        ErpSalDelivery delivery = returnOrder.getDelivery();
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, null);
        }
        String deliveryStatus = delivery.getApproveStatus();
        if (deliveryStatus == null || !Objects.equals(deliveryStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, deliveryStatus);
        }
    }

    protected void requireReasonIfConfigured(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines,
                                             IServiceContext context) {
        if (!isReasonRequired()) {
            return;
        }
        for (ErpSalReturnLine line : lines) {
            if (line.getReason() == null || line.getReason().trim().isEmpty()) {
                throw new NopException(ErpSalErrors.ERR_RETURN_REASON_REQUIRED)
                        .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                        .param(ErpSalErrors.ARG_LINE_NO, line.getLineNo());
            }
        }
    }

    protected boolean isReasonRequired() {
        return readBoolConfig(ErpSalConstants.CONFIG_RETURN_REASON_REQUIRED, true);
    }

    protected boolean readBoolConfig(String key, boolean defaultValue) {
        try {
            String value = AppConfig.var(key, String.valueOf(defaultValue));
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ---------- 过账接线 ----------

    protected void applyPosted(ErpSalReturn returnOrder, boolean posted) {
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentTimestamp());
            returnOrder.setPostedBy(currentUserId());
        }
    }

    // ---------- 库存触发 + 过账 + 冲销 ----------

    protected ErpInvStockMove triggerIncomingMove(ErpSalReturn returnOrder, IServiceContext context) {
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        StockMoveRequest request = stockMoveBuilder.build(returnOrder, lines, context);
        request.setOriginReturnedMoveId(resolveSourceDeliveryMoveId(returnOrder, context));
        return stockMoveBiz.generateMove(request, context);
    }

    protected Long resolveSourceDeliveryMoveId(ErpSalReturn returnOrder, IServiceContext context) {
        ErpSalDelivery delivery = returnOrder.getDelivery();
        if (delivery == null) {
            return null;
        }
        ErpInvStockMove sourceMove = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode(), context);
        return sourceMove == null ? null : sourceMove.getId();
    }

    protected boolean triggerPosting(ErpSalReturn returnOrder, IServiceContext context) {
        return postingDispatcher.tryPost(returnOrder);
    }

    protected void ensureReversed(ErpSalReturn returnOrder, IServiceContext context) {
        reversePostingIfAny(returnOrder, context);
        ErpInvStockMove original = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnOrder.getCode(), context);
        if (original == null) {
            throw new NopException(ErpSalErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
        ErpInvStockMove existingReversal = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode(), context);
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    protected void reversePostingIfAny(ErpSalReturn returnOrder, IServiceContext context) {
        if (Boolean.TRUE.equals(returnOrder.getPosted())) {
            postingDispatcher.reverse(returnOrder);
            ErpSalReturn reloaded = returnDao().getEntityById(returnOrder.getId());
            reloaded.setPosted(false);
            reloaded.setPostedAt(null);
            reloaded.setPostedBy(null);
        }
        refundOrchestrator.restoreRefund(returnOrder);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpSalReturn requireReturn(String id, IServiceContext context) {
        ErpSalReturn returnOrder = returnDao().getEntityById(id);
        if (returnOrder == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RETURN_CODE, id);
        }
        return returnOrder;
    }

    protected void validateNotCancelled(ErpSalReturn returnOrder, IServiceContext context) {
        if (returnOrder.isCancelled()) {
            throw illegalDocTransition(returnOrder, returnOrder.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpSalReturn returnOrder, IServiceContext context) {
        if (loadLines(returnOrder.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalReturn returnOrder, IServiceContext context) {
        if (returnOrder.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(returnOrder.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, returnOrder.getCustomerId());
        }
    }

    protected List<ErpSalReturnLine> loadLines(Long returnId) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    /**
     * 退货后回填原订单行的已出库数量（P1-RC-023 方案 A 写路径）。
     *
     * <p>L1 公式：未交货量 = 订单数量 − 已出库数量 + 退货数量。本方法补写 {@code ErpSalOrderLine.deliveredQuantity}
     * 的「毛口径」值（Σ APPROVED delivery-line qty by orderLineId，对齐 ReturnQtyValidator 聚合先例），
     * 使公式可派生成立。退货量 Σ 由读侧从 APPROVED 退货行聚合派生，不在本方法写入——净口径会与公式中
     * {@code + 退货数量} 项重复计算。幂等：按重新聚合重算（非增量累加）。
     *
     * <p>链路：退货行 {@code deliveryLineId} → {@code ErpSalDeliveryLine.orderLineId} → {@code ErpSalOrderLine}。
     * 与 P2-RC-019（deliveredQuantity 零 writer）同源联动，此处为首个写入口。
     */
    protected void updateUndeliveredQuantity(ErpSalReturn returnOrder, IServiceContext context) {
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        Set<Long> orderLineIds = new HashSet<>();
        IEntityDao<ErpSalDeliveryLine> deliveryLineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        for (ErpSalReturnLine line : lines) {
            Long deliveryLineId = line.getDeliveryLineId();
            if (deliveryLineId == null) {
                continue;
            }
            ErpSalDeliveryLine deliveryLine = deliveryLineDao.getEntityById(deliveryLineId);
            if (deliveryLine != null && deliveryLine.getOrderLineId() != null) {
                orderLineIds.add(deliveryLine.getOrderLineId());
            }
        }
        if (orderLineIds.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> deliveredByOrderLine = aggregateApprovedDelivered(orderLineIds);
        IEntityDao<ErpSalOrderLine> orderLineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        for (Long orderLineId : orderLineIds) {
            ErpSalOrderLine orderLine = orderLineDao.getEntityById(orderLineId);
            if (orderLine != null) {
                BigDecimal delivered = deliveredByOrderLine.getOrDefault(orderLineId, BigDecimal.ZERO);
                orderLine.setDeliveredQuantity(delivered);
                orderLineDao.updateEntity(orderLine);
            }
        }
    }

    /**
     * 按 orderLineId 聚合 APPROVED 出库行的数量（毛口径）。对齐 ReturnQtyValidator:72-101 先例：
     * 仅统计父出库单 approveStatus=APPROVED 的出库行数量。
     */
    private Map<Long, BigDecimal> aggregateApprovedDelivered(Set<Long> orderLineIds) {
        IEntityDao<ErpSalDeliveryLine> dlDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        QueryBean dlQuery = new QueryBean();
        dlQuery.addFilter(in("orderLineId", orderLineIds));
        List<ErpSalDeliveryLine> deliveryLines = dlDao.findAllByQuery(dlQuery);
        if (deliveryLines.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> deliveryIds = deliveryLines.stream()
                .map(ErpSalDeliveryLine::getDeliveryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        IEntityDao<ErpSalDelivery> dDao = daoProvider.daoFor(ErpSalDelivery.class);
        QueryBean dQuery = new QueryBean();
        dQuery.addFilter(and(
                in("id", deliveryIds),
                eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED)));
        Set<Long> approvedDeliveryIds = dDao.findAllByQuery(dQuery).stream()
                .map(ErpSalDelivery::getId)
                .collect(Collectors.toSet());

        Map<Long, BigDecimal> result = new HashMap<>();
        for (ErpSalDeliveryLine dl : deliveryLines) {
            if (dl.getDeliveryId() != null && approvedDeliveryIds.contains(dl.getDeliveryId())) {
                BigDecimal qty = dl.getQuantity() == null ? BigDecimal.ZERO : dl.getQuantity();
                result.merge(dl.getOrderLineId(), qty, BigDecimal::add);
            }
        }
        return result;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalReturn> returnDao() {
        return daoProvider.daoFor(ErpSalReturn.class);
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

    protected NopException illegalTransition(ErpSalReturn returnOrder, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalReturn returnOrder, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
