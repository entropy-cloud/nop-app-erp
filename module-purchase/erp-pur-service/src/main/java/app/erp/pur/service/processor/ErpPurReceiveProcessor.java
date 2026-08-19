package app.erp.pur.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.SoDGuard;
import app.erp.pur.service.entity.ReceiveStockMoveBuilder;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.biz.IErpInvDrpLeadTimeRecordBiz;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购入库单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpPurReceiveProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurReceiveProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    IErpPrjCostCollectionBiz costCollectionBiz;

    @Inject
    ReceiveStockMoveBuilder stockMoveBuilder;

    @Inject
    IErpPurOrderBiz orderBiz;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpQaInspectionBiz inspectionBiz;

    @Inject
    @Nullable
    IErpInvDrpCrossDockBiz crossDockBiz;

    @Inject
    @Nullable
    IErpInvDrpLeadTimeRecordBiz leadTimeRecordBiz;

    @Inject
    ErpPurReceiveSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpPurReceiveApproveProcessor approveProcessor;

    @Inject
    ErpPurReceiveRejectProcessor rejectProcessor;

    @Inject
    ErpPurReceiveReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpPurReceiveWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpPurReceiveCancelProcessor cancelProcessor;

    public ErpPurReceive submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpPurReceive withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpPurReceive approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpPurReceive reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpPurReceive reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpPurReceive cancel(String id, IServiceContext context) {
        return cancelProcessor.cancel(id, context);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpPurReceive receive, IServiceContext context) {
        validateNotCancelled(receive, context);
        String status = receive.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(receive, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receive, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receive, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receive, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(receive, status, ErpPurConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void validateTransitionForCancel(ErpPurReceive receive, IServiceContext context) {
        String docStatus = receive.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(receive, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurReceive receive, IServiceContext context) {
        requireLinesNonEmpty(receive, context);
        requireSupplierActive(receive, context);
    }

    protected void validateBusinessRulesForApprove(ErpPurReceive receive, IServiceContext context) {
        requireSupplierActive(receive, context);
        validateOverReceiptTolerance(receive, context);
    }

    /**
     * receive-vs-order 超收容差校验（P1-RC-019 / RC-R1.11，three-way-match.md §数量差异「超收」行）。
     * 按 {@code erp-pur.match-qty-tolerance}（默认 5%）判定「当前入库单行 + 同订单其他 APPROVED 入库单行」的
     * 累计入库数量 Σ &gt; 订单行数量 × (1 + 容差%)：严格模式（{@code erp-pur.match-strict-mode}=true）抛
     * {@link ErpPurErrors#ERR_RECEIVE_QTY_OVER_TOLERANCE} 拒绝审核，非严格模式 LOG.warn 放行。
     * {@code orderLineId == null} 的行（无订单关联独立入库）跳过；订单行数量为 0/null 按 0 基处理（Σ&gt;0 即超收）。
     * 聚合口径继承 {@link #rollupOrderReceiveStatus} 现状：CANCELLED 但仍 APPROVED 的入库单计入 Σ。
     */
    protected void validateOverReceiptTolerance(ErpPurReceive receive, IServiceContext context) {
        Long orderId = receive.getOrderId();
        if (orderId == null) {
            return;
        }
        List<ErpPurOrderLine> orderLines = loadOrderLines(orderId);
        if (orderLines.isEmpty()) {
            return;
        }
        BigDecimal tolerance = readDecimalConfig(ErpPurConstants.CONFIG_MATCH_QTY_TOLERANCE, "5");
        boolean strict = readBooleanConfig(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, "false");
        BigDecimal toleranceFactor = BigDecimal.ONE.add(
                tolerance.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        Map<Long, BigDecimal> receivedByOrderLine = new HashMap<>();
        addLineQuantities(receivedByOrderLine, loadLines(receive));
        for (ErpPurReceive r : findApprovedReceives(orderId)) {
            if (r.getId().equals(receive.getId())) {
                continue;
            }
            addLineQuantities(receivedByOrderLine, loadLines(r));
        }

        for (ErpPurReceiveLine rl : loadLines(receive)) {
            Long orderLineId = rl.getOrderLineId();
            if (orderLineId == null) {
                continue;
            }
            ErpPurOrderLine orderLine = findOrderLine(orderLines, orderLineId);
            if (orderLine == null) {
                continue;
            }
            BigDecimal ordered = orderLine.getQuantity() == null ? BigDecimal.ZERO : orderLine.getQuantity();
            BigDecimal received = receivedByOrderLine.getOrDefault(orderLineId, BigDecimal.ZERO);
            if (received.compareTo(ordered.multiply(toleranceFactor)) <= 0) {
                continue;
            }
            NopException err = new NopException(ErpPurErrors.ERR_RECEIVE_QTY_OVER_TOLERANCE)
                    .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode())
                    .param(ErpPurErrors.ARG_LINE_NO, rl.getLineNo())
                    .param(ErpPurErrors.ARG_RECEIVED_QTY, received)
                    .param(ErpPurErrors.ARG_ORDER_QTY, ordered)
                    .param(ErpPurErrors.ARG_TOLERANCE, tolerance);
            if (strict) {
                throw err;
            }
            LOG.warn("入库超收超容差（非严格模式放行）：入库单={} 行={} 累计入库数量={} 订单数量={} 容差={}%",
                    receive.getCode(), rl.getLineNo(), received, ordered, tolerance);
        }
    }

    // ---------- step：执行 ----------

    protected void doSubmit(ErpPurReceive receive, IServiceContext context) {
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        receiveDao().updateEntity(receive);
    }

    protected void doWithdrawSubmit(ErpPurReceive receive, IServiceContext context) {
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receiveDao().updateEntity(receive);
    }

    protected void doApprove(ErpPurReceive receive, ErpInvStockMove move, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(receive.getCreatedBy(), currentUserId(), ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        receive.setApprovedBy(currentUserId());
        receive.setApprovedAt(CoreMetrics.currentTimestamp());
        applyPostingResult(receive, move);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_RECEIVED);
        receiveDao().updateEntity(receive);
    }

    protected void doReject(ErpPurReceive receive, IServiceContext context) {
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        receiveDao().updateEntity(receive);
    }

    protected void doReverseApprove(ErpPurReceive receive, IServiceContext context) {
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        receive.setApprovedBy(null);
        receive.setApprovedAt(null);
        receiveDao().updateEntity(receive);
    }

    protected void doCancel(ErpPurReceive receive, IServiceContext context) {
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        receiveDao().updateEntity(receive);
    }

    protected void postProcessApprove(ErpPurReceive currentReceive, IServiceContext context) {
        rollupOrderReceiveStatus(currentReceive, context);
    }

    /**
     * 越库收货识别（RC-R1.81 / P1-RC-081，D1 裁决选项 A，UC-DRP-07 触发方式 3 收货时匹配）：
     * 入库移动单生成后，按采购单号 + 收货行物料调用 drp Facade 将 PENDING 越库记录标记 STAGING 并回写
     * inboundMoveId。drp 侧 {@code erp-inv.drp-xdock-enabled} 默认 false 时 Facade 返回 0 零副作用；
     * 仅 PENDING 可迁移 → 重复审批/并发收货幂等。
     *
     * <p>@Nullable：drp 模块未部署（单域测试/裁剪部署）时跳过；失败隔离 try/catch 不阻断
     * RECEIVED 主迁移与过账（对齐 RC-R1.85 logistics→sal 回写容错范式）。
     */
    protected void markCrossDockReceived(ErpPurReceive receive, ErpInvStockMove move, IServiceContext context) {
        if (crossDockBiz == null) {
            return;
        }
        try {
            String orderCode = resolveOrderCode(receive.getOrderId());
            if (orderCode == null) {
                return;
            }
            Set<Long> materialIds = new LinkedHashSet<>();
            for (ErpPurReceiveLine line : loadLines(receive)) {
                if (line.getMaterialId() != null) {
                    materialIds.add(line.getMaterialId());
                }
            }
            if (materialIds.isEmpty()) {
                return;
            }
            crossDockBiz.markReceivedFromPurchase(orderCode, move != null ? move.getId() : null,
                    new ArrayList<>(materialIds), context);
        } catch (Exception e) {
            LOG.warn("入库审批后置：越库收货标记失败（隔离不阻断）：receiveCode={}, reason={}",
                    receive.getCode(), e.getMessage());
        }
    }

    /**
     * 按订单 id 解析采购单号（drp Facade 弱指针键 = CrossDock.sourceBillCode 存 PO 单号）。
     */
    protected String resolveOrderCode(Long orderId) {
        if (orderId == null) {
            return null;
        }
        ErpPurOrder order = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        return order != null ? order.getCode() : null;
    }

    /**
     * 提前期记录（RC-R1.82 / P1-RC-082，D4 裁决选项 A，UC-DRP-08 触发）：收货确认后调 drp Facade
     * 落 ErpInvDrpLeadTimeRecord（actualLeadTime = DATEDIFF(receiptDate, orderDate)；expected 取订单
     * deliveryDate - businessDate，缺失传 null 由 drp 侧跳过准时判定）。幂等守卫同单号+物料不重复落。
     *
     * <p>@Nullable：drp 模块未部署（单域测试/裁剪部署）时跳过；失败隔离 try/catch 不阻断
     * RECEIVED 主迁移与过账（订单/收货日期缺失时 drp 侧抛 dates-invalid，此处告警跳过 = L1 异常路径）。
     */
    protected void recordLeadTimeFromReceive(ErpPurReceive receive, IServiceContext context) {
        if (leadTimeRecordBiz == null) {
            return;
        }
        try {
            if (receive.getOrderId() == null) {
                return;
            }
            ErpPurOrder order = daoProvider.daoFor(ErpPurOrder.class).getEntityById(receive.getOrderId());
            if (order == null || order.getBusinessDate() == null || receive.getBusinessDate() == null) {
                return;
            }
            Set<Long> materialIds = new LinkedHashSet<>();
            for (ErpPurReceiveLine line : loadLines(receive)) {
                if (line.getMaterialId() != null) {
                    materialIds.add(line.getMaterialId());
                }
            }
            if (materialIds.isEmpty()) {
                return;
            }
            Integer expectedLeadTime = null;
            if (order.getDeliveryDate() != null
                    && !order.getDeliveryDate().isBefore(order.getBusinessDate())) {
                expectedLeadTime = (int) java.time.temporal.ChronoUnit.DAYS
                        .between(order.getBusinessDate(), order.getDeliveryDate());
            }
            leadTimeRecordBiz.recordFromPurchaseReceive(order.getCode(), receive.getSupplierId(),
                    order.getBusinessDate(), receive.getBusinessDate(), expectedLeadTime,
                    new ArrayList<>(materialIds), context);
        } catch (Exception e) {
            LOG.warn("收货审批后置：提前期记录失败（隔离不阻断）：receiveCode={}, reason={}",
                    receive.getCode(), e.getMessage());
        }
    }

    // ---------- 库存触发 + 过账接线 + 冲销 ----------

    protected ErpInvStockMove triggerIncomingMove(ErpPurReceive receive, IServiceContext context) {
        List<ErpPurReceiveLine> lines = loadLines(receive);
        StockMoveRequest request = stockMoveBuilder.build(receive, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 项目物料成本归集（RC-R1.61 / P1-RC-049，UC-PRJ-03 采购入库→项目 MATERIAL 来源）。
     * 入库移动单生成后逐行解析项目维度（{@code receiveLine.orderLineId → orderLine.projectId}，
     * 行级 projectId 为 null 跳过），调 {@link IErpPrjCostCollectionBiz#aggregateMaterialCost}
     * 跨域 Facade 归集（projects 侧守卫 requireReferenceable + 预算检查 STRICT 拒绝 + 幂等去重，
     * 归集行 costCategory=MATERIAL / amount=入库行金额不含税 / sourceBillCode=入库单号-行号）。
     *
     * <p>STRICT 预算/非 OPEN 项目经 Facade 异常传播 → 入库审核回滚拒绝（对齐 L1 UC-PRJ-04「采购审核
     * 拒绝该笔归集」）；config {@code erp-prj.material-aggregation-enabled} 关闭时 Facade 返回 0 零副作用。
     */
    protected void collectProjectMaterialCost(ErpPurReceive receive, IServiceContext context) {
        for (ErpPurReceiveLine line : loadLines(receive)) {
            Long orderLineId = line.getOrderLineId();
            if (orderLineId == null) {
                continue;
            }
            ErpPurOrderLine orderLine = line.getOrderLine();
            if (orderLine == null || orderLine.getProjectId() == null) {
                continue;
            }
            BigDecimal amount = line.getAmount();
            if (amount == null || amount.signum() <= 0) {
                continue;
            }
            String sourceBillCode = receive.getCode() + "-" + line.getLineNo();
            costCollectionBiz.aggregateMaterialCost(orderLine.getProjectId(), amount, sourceBillCode, context);
        }
    }

    protected void applyPostingResult(ErpPurReceive receive, ErpInvStockMove move) {
        receive.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(receive.getPosted())) {
            receive.setPostedAt(CoreMetrics.currentTimestamp());
            receive.setPostedBy(currentUserId());
        }
    }

    protected void ensureReversed(ErpPurReceive receive, IServiceContext context) {
        ErpInvStockMove original = stockMoveBiz.findByRelatedBill(
                ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode(), context);
        if (original == null) {
            throw new NopException(ErpPurErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode());
        }
        ErpInvStockMove existingReversal = stockMoveBiz.findByRelatedBill(
                ErpPurConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode(), context);
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    protected void rollupOrderReceiveStatus(ErpPurReceive currentReceive, IServiceContext context) {
        Long orderId = currentReceive.getOrderId();
        if (orderId == null) {
            return;
        }
        List<ErpPurOrderLine> orderLines = loadOrderLines(orderId);
        if (orderLines.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> receivedByOrderLine = new HashMap<>();
        addLineQuantities(receivedByOrderLine, loadLines(currentReceive));
        for (ErpPurReceive r : findApprovedReceives(orderId)) {
            if (r.getId().equals(currentReceive.getId())) {
                continue;
            }
            addLineQuantities(receivedByOrderLine, loadLines(r));
        }

        boolean anyReceived = false;
        boolean allFullyReceived = true;
        for (ErpPurOrderLine ol : orderLines) {
            BigDecimal ordered = ol.getQuantity() == null ? BigDecimal.ZERO : ol.getQuantity();
            BigDecimal received = receivedByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);
            if (received.signum() > 0) {
                anyReceived = true;
            }
            if (received.compareTo(ordered) < 0) {
                allFullyReceived = false;
            }
        }
        String rolled;
        if (allFullyReceived) {
            rolled = ErpPurConstants.RECEIVE_STATUS_RECEIVED;
        } else if (anyReceived) {
            rolled = ErpPurConstants.RECEIVE_STATUS_PARTIAL;
        } else {
            rolled = ErpPurConstants.RECEIVE_STATUS_UNRECEIVED;
        }
        orderBiz.updateReceiveStatus(orderId, rolled, context);
    }

    protected void enforceInspectionGate(ErpPurReceive receive, IServiceContext context) {
        String billType = ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE;
        if (!InspectionTrigger.isMandatoryBillType(billType)) {
            return;
        }
        for (ErpPurReceiveLine line : loadLines(receive)) {
            if (line.getMaterialId() == null) {
                continue;
            }
            int gate = InspectionTrigger.enforceGate(inspectionBiz, billType, receive.getCode(),
                    line.getMaterialId(), "INCOMING",
                    line.getQuantity(), receive.getSupplierId(), receive.getWarehouseId(), null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpPurErrors.ERR_RECEIVE_INSPECTION_BLOCKED)
                        .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode());
            }
        }
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurReceive requireReceive(String id, IServiceContext context) {
        ErpPurReceive receive = receiveDao().getEntityById(id);
        if (receive == null) {
            throw new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RECEIVE_ID, id);
        }
        return receive;
    }

    protected void validateNotCancelled(ErpPurReceive receive, IServiceContext context) {
        if (receive.isCancelled()) {
            throw illegalDocTransition(receive, receive.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpPurReceive receive, IServiceContext context) {
        if (receive.getLines().isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_RECEIVE_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode());
        }
    }

    protected void requireSupplierActive(ErpPurReceive receive, IServiceContext context) {
        if (receive.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(receive.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpPurConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, receive.getSupplierId());
        }
    }

    /**
     * 通过 ORM to-many 关系 {@code ErpPurReceive.lines} 加载行（懒加载，复用主实体 session）。
     * 关系已在 {@code app-erp-purchase.orm.xml} 声明。
     */
    protected List<ErpPurReceiveLine> loadLines(ErpPurReceive receive) {
        return new ArrayList<>(receive.getLines());
    }

    /**
     * 按订单 id 加载采购订单行。{@code ErpPurReceive} 与 {@code ErpPurOrder} 是不同聚合，
     * 此处仅需 orderId 即可查询；保留 daoFor 形式避免仅为导航而额外加载订单头实体。
     */
    protected List<ErpPurOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected List<ErpPurReceive> findApprovedReceives(Long orderId) {
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("orderId", orderId), eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED)));
        return new ArrayList<>(receiveDao().findAllByQuery(rq));
    }

    protected void addLineQuantities(Map<Long, BigDecimal> map, List<ErpPurReceiveLine> lines) {
        for (ErpPurReceiveLine rl : lines) {
            if (rl.getOrderLineId() == null) {
                continue;
            }
            BigDecimal qty = rl.getQuantity() == null ? BigDecimal.ZERO : rl.getQuantity();
            map.merge(rl.getOrderLineId(), qty, BigDecimal::add);
        }
    }

    protected ErpPurOrderLine findOrderLine(List<ErpPurOrderLine> orderLines, Long orderLineId) {
        for (ErpPurOrderLine ol : orderLines) {
            if (ol.getId().equals(orderLineId)) {
                return ol;
            }
        }
        return null;
    }

    /**
     * 配置读取（对齐 ThreeWayMatcher 同型容错范式）：缺失/非法回退默认值，不因配置问题阻断审核。
     */
    protected BigDecimal readDecimalConfig(String key, String defaultValue) {
        try {
            String value = AppConfig.var(key, defaultValue);
            if (value == null) {
                return new BigDecimal(defaultValue);
            }
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return new BigDecimal(defaultValue);
        }
    }

    protected boolean readBooleanConfig(String key, String defaultValue) {
        try {
            String value = AppConfig.var(key, defaultValue);
            return "true".equalsIgnoreCase(value) || "1".equals(value);
        } catch (Exception e) {
            return "true".equalsIgnoreCase(defaultValue);
        }
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpPurReceive> receiveDao() {
        return daoProvider.daoFor(ErpPurReceive.class);
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

    protected NopException illegalTransition(ErpPurReceive receive, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurReceive receive, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
