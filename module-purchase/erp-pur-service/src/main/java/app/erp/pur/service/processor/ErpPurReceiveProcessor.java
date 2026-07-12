package app.erp.pur.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.entity.ReceiveStockMoveBuilder;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
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

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ReceiveStockMoveBuilder stockMoveBuilder;

    @Inject
    IErpPurOrderBiz orderBiz;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpQaInspectionBiz inspectionBiz;

    public ErpPurReceive submitForApproval(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        validateTransitionForSubmit(receive, context);
        validateBusinessRulesForSubmit(receive, context);
        doSubmit(receive, context);
        return receive;
    }

    public ErpPurReceive withdrawApproval(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        validateNotCancelled(receive, context);
        validateTransitionForWithdraw(receive, context);
        doWithdrawSubmit(receive, context);
        return receive;
    }

    public ErpPurReceive approve(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        if (isAlreadyApproved(receive)) {
            return receive;
        }
        validateNotCancelled(receive, context);
        validateTransitionForApprove(receive, context);
        validateBusinessRulesForApprove(receive, context);
        enforceInspectionGate(receive, context);

        ErpInvStockMove move = triggerIncomingMove(receive, context);
        receive = receiveDao().getEntityById(id);
        doApprove(receive, move, context);
        postProcessApprove(receive, context);
        return receive;
    }

    public ErpPurReceive reject(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        validateNotCancelled(receive, context);
        validateTransitionForReject(receive, context);
        doReject(receive, context);
        return receive;
    }

    public ErpPurReceive reverseApprove(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        if (isAlreadyRejected(receive)) {
            return receive;
        }
        validateTransitionForReverseApprove(receive, context);
        ensureReversed(receive, context);
        receive = receiveDao().getEntityById(id);
        doReverseApprove(receive, context);
        return receive;
    }

    public ErpPurReceive cancel(String id, IServiceContext context) {
        ErpPurReceive receive = requireReceive(id, context);
        validateTransitionForCancel(receive, context);
        if (isApproved(receive)) {
            ensureReversed(receive, context);
            receive = receiveDao().getEntityById(id);
        }
        doCancel(receive, context);
        return receive;
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
            throw illegalTransition(receive, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receive, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(receive, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurReceive receive, IServiceContext context) {
        String status = receive.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(receive, status, "APPROVED");
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
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        receive.setApprovedBy(currentUserId());
        receive.setApprovedAt(CoreMetrics.currentDateTime());
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

    // ---------- 库存触发 + 过账接线 + 冲销 ----------

    protected ErpInvStockMove triggerIncomingMove(ErpPurReceive receive, IServiceContext context) {
        List<ErpPurReceiveLine> lines = loadLines(receive.getId());
        StockMoveRequest request = stockMoveBuilder.build(receive, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    protected void applyPostingResult(ErpPurReceive receive, ErpInvStockMove move) {
        receive.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(receive.getPosted())) {
            receive.setPostedAt(CoreMetrics.currentDateTime());
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
        addLineQuantities(receivedByOrderLine, loadLines(currentReceive.getId()));
        for (ErpPurReceive r : findApprovedReceives(orderId)) {
            if (r.getId().equals(currentReceive.getId())) {
                continue;
            }
            addLineQuantities(receivedByOrderLine, loadLines(r.getId()));
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
        for (ErpPurReceiveLine line : loadLines(receive.getId())) {
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
        String docStatus = receive.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(receive, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpPurReceive receive) {
        String status = receive.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isApproved(ErpPurReceive receive) {
        return isAlreadyApproved(receive);
    }

    protected boolean isAlreadyRejected(ErpPurReceive receive) {
        String status = receive.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpPurReceive receive, IServiceContext context) {
        if (loadLines(receive.getId()).isEmpty()) {
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

    protected List<ErpPurReceiveLine> loadLines(Long receiveId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiveId", receiveId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

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
