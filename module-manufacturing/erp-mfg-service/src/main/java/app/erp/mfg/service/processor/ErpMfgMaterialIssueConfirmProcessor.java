package app.erp.mfg.service.processor;

import app.erp.inv.biz.IErpInvReservationBiz;
import app.erp.inv.biz.ReservationConsumeLine;
import app.erp.inv.biz.ReservationConsumeRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpMfgMaterialIssue confirm per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含领料确认→出库移动单 + 材料成本回写编排（issue-status DRAFT→CONFIRMED→DONE）；从 ErpMfgMaterialIssueBizModel
 * 内联 @BizMutation 提取。共享 protected helper 单一真相源在 {@link AbstractErpMfgMaterialIssueProcessor}。
 *
 * <p>幂等：已 DONE 的领料单重复确认为空操作（state-machine §4）。幂等键 {@code (ERP_MFG_ISSUE, issue.code)} 由 generateMove 防重复触发。
 */
public class ErpMfgMaterialIssueConfirmProcessor extends AbstractErpMfgMaterialIssueProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMfgMaterialIssueConfirmProcessor.class);

    @Inject
    IErpInvReservationBiz reservationBiz;

    public ErpMfgMaterialIssue confirm(Long issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = requireIssue(issueId, context);
        String status = issue.getDocStatus();
        // 幂等：已 DONE（已出库）直接返回，不重复触发库存出库（state-machine §4；动态幂等守卫保留原位）
        if (status != null && Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DONE)) {
            return issue;
        }
        // 固定来源态守卫（仅 DRAFT）委托 ErpMfgMaterialIssueStateMachine（M4.39）
        validateTransition(issue, context);
        List<ErpMfgMaterialIssueLine> lines = loadLines(issueId);
        if (lines.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_ISSUE_LINES_EMPTY)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode());
        }

        // issue-status DRAFT→CONFIRMED（confirm 动作瞬态中间态，同事务内立即推进至 DONE）
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_CONFIRMED);
        issueDao().updateEntity(issue);

        // 构造出库移动单请求并生成（业务联动自动 DRAFT→CONFIRMED→DONE，扣减库存；幂等键防重复）
        StockMoveRequest request = stockMoveBuilder.build(issue, lines, context);
        ErpInvStockMove move = stockMoveBiz.generateMove(request, context);
        // 跨域 generateMove 推进至 DONE 并更新余额；刷盘使 DONE 状态落地当前事务连接
        ormTemplate.flushSession();

        // 领料消耗预留（UC-MFG-06 ⑬⑭⑯）：config-gated；查无预留 no-op 零写入；领料移动单主链零改动
        consumeReservations(issue, lines, context);

        // 回写 WorkOrderLine.actualQuantity（按领料行 workOrderLineId 匹配）
        writebackWorkOrderLineActualQty(lines, context);

        // issue-status CONFIRMED→DONE（已出库）；汇总领料出库流水 totalCost → WorkOrder.materialCost
        BigDecimal materialCostDelta = aggregateIssueMaterialCost(move, context);
        issue = requireIssue(issueId, context);
        issue.setDocStatus(stateMachine.confirmTargetStatus());
        issueDao().updateEntity(issue);

        applyMaterialCostToWorkOrder(issue.getWorkOrderId(), materialCostDelta, context);

        // 领料 GL 过账（Dr: WIP / Cr: Inventory），镜像 ProductionVarianceDispatcher 显式调用范式
        issuePostingDispatcher.dispatchIfApplicable(issueId);
        return issue;
    }

    // ---------- step：领料消耗预留（protected，供派生复用与覆盖；RC-R1.48） ----------

    /**
     * 领料消耗预留（UC-MFG-06 ⑬⑭⑯）：config-gated（{@code erp-mfg.reservation-enabled} 默认 true）。
     * 按工单 (WORK_ORDER, wo.code) 定位预留（查无 → 静默 no-op，既有无预留工单零回归）；
     * 超预留（领料量 > 预留未消耗量 reservedQuantity−consumedQuantity）按 D1 裁决
     * （{@code erp-mfg.over-pick-warning} 默认 true）LOG.warn 放行，不阻断领料主链。
     * 消耗经 {@code IErpInvReservationBiz.consumeReservation}（consumedQuantity+= / 库存余额预留量-=，
     * 超量按 min 封顶）。
     */
    protected void consumeReservations(ErpMfgMaterialIssue issue, List<ErpMfgMaterialIssueLine> lines,
                                       IServiceContext context) {
        if (!isReservationEnabled()) {
            return;
        }
        Long workOrderId = issue.getWorkOrderId();
        if (workOrderId == null) {
            return;
        }
        ErpMfgWorkOrder wo = workOrderBiz.get(String.valueOf(workOrderId), false, context);
        if (wo == null) {
            return;
        }
        ErpInvReservation reservation = findReservation(wo.getCode());
        if (reservation == null) {
            return;
        }
        List<ErpInvReservationLine> reservationLines = findReservationLines(reservation.getId());
        if (reservationLines.isEmpty()) {
            return;
        }

        ReservationConsumeRequest request = new ReservationConsumeRequest();
        request.setSourceBillType(ErpMfgConstants.SOURCE_BILL_TYPE_WORK_ORDER);
        request.setSourceBillCode(wo.getCode());
        List<ReservationConsumeLine> consumeLines = new ArrayList<>(lines.size());
        for (ErpMfgMaterialIssueLine line : lines) {
            if (line.getMaterialId() == null) {
                continue;
            }
            BigDecimal issued = line.getIssuedQuantity() != null ? line.getIssuedQuantity() : line.getRequiredQuantity();
            if (nz(issued).signum() <= 0) {
                continue;
            }
            warnIfOverPick(wo, reservationLines, line.getMaterialId(), issued);
            ReservationConsumeLine consume = new ReservationConsumeLine();
            consume.setMaterialId(line.getMaterialId());
            consume.setWarehouseId(issue.getWarehouseId());
            consume.setLocationId(line.getLocationId());
            consume.setBatchNo(line.getBatchNo());
            consume.setQuantity(issued);
            consumeLines.add(consume);
        }
        if (consumeLines.isEmpty()) {
            return;
        }
        request.setLines(consumeLines);
        reservationBiz.consumeReservation(request, context);
    }

    /**
     * 超预留警告（D1 裁决）：领料量 > 该物料预留未消耗量（reservedQuantity − consumedQuantity）时
     * LOG.warn 放行（config {@code erp-mfg.over-pick-warning} 默认 true；关闭则静默放行）。
     */
    protected void warnIfOverPick(ErpMfgWorkOrder wo, List<ErpInvReservationLine> reservationLines,
                                  Long materialId, BigDecimal issued) {
        BigDecimal remaining = BigDecimal.ZERO;
        for (ErpInvReservationLine line : reservationLines) {
            if (Objects.equals(line.getMaterialId(), materialId)) {
                remaining = remaining.add(nz(line.getReservedQuantity()).subtract(nz(line.getConsumedQuantity())));
            }
        }
        if (remaining.compareTo(issued) >= 0) {
            return;
        }
        if (isOverPickWarningEnabled()) {
            LOG.warn("工单 {} 领料超预留：materialId={}, 领料量={}, 预留未消耗量={}（over-pick-warning=true 放行）",
                    wo.getCode(), materialId, issued.toPlainString(), remaining.toPlainString());
        }
    }

    protected ErpInvReservation findReservation(String workOrderCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpMfgConstants.SOURCE_BILL_TYPE_WORK_ORDER));
        q.addFilter(eq("sourceBillCode", workOrderCode));
        List<ErpInvReservation> list = daoProvider.daoFor(ErpInvReservation.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected List<ErpInvReservationLine> findReservationLines(Long reservationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(daoProvider.daoFor(ErpInvReservationLine.class).findAllByQuery(q));
    }

    protected boolean isReservationEnabled() {
        try {
            String value = AppConfig.var(ErpMfgConstants.CONFIG_RESERVATION_ENABLED, "true");
            return value == null || value.trim().isEmpty() || Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return true;
        }
    }

    protected boolean isOverPickWarningEnabled() {
        try {
            String value = AppConfig.var(ErpMfgConstants.CONFIG_OVER_PICK_WARNING, "true");
            return value == null || value.trim().isEmpty() || Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return true;
        }
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ---------- step：成本回写（protected，供派生复用与覆盖） ----------

    protected void writebackWorkOrderLineActualQty(List<ErpMfgMaterialIssueLine> lines, IServiceContext context) {
        Map<Long, BigDecimal> byWorkOrderLine = new HashMap<>();
        for (ErpMfgMaterialIssueLine line : lines) {
            if (line.getWorkOrderLineId() == null) {
                continue;
            }
            BigDecimal issued = line.getIssuedQuantity() != null ? line.getIssuedQuantity() : line.getRequiredQuantity();
            byWorkOrderLine.merge(line.getWorkOrderLineId(), nz(issued), BigDecimal::add);
        }
        if (byWorkOrderLine.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> e : byWorkOrderLine.entrySet()) {
            ErpMfgWorkOrderLine wol = workOrderLineBiz.get(String.valueOf(e.getKey()), false, context);
            if (wol == null) {
                continue;
            }
            wol.setActualQuantity(nz(wol.getActualQuantity()).add(e.getValue()));
            workOrderLineBiz.updateEntity(wol, null, context);
        }
    }

    /**
     * 汇总领料出库移动单流水 totalCost。出库流水 totalCost 为负值（库存减少），故取绝对值作为材料成本增加额。
     */
    protected BigDecimal aggregateIssueMaterialCost(ErpInvStockMove move, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", move.getId()));
        List<ErpInvStockLedger> ledgers = stockLedgerBiz.findList(q, null, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpInvStockLedger l : ledgers) {
            sum = sum.add(nz(l.getTotalCost()));
        }
        return sum.abs();
    }

    protected void applyMaterialCostToWorkOrder(Long workOrderId, BigDecimal materialCostDelta, IServiceContext context) {
        if (workOrderId == null || materialCostDelta == null || materialCostDelta.signum() == 0) {
            return;
        }
        var wo = workOrderBiz.get(String.valueOf(workOrderId), false, context);
        if (wo == null) {
            return;
        }
        wo.setMaterialCost(nz(wo.getMaterialCost()).add(materialCostDelta));
        recomputeTotals(wo);
        workOrderBiz.updateEntity(wo, null, context);
    }
}
