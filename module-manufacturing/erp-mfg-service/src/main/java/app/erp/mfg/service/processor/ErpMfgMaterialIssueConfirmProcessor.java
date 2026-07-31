package app.erp.mfg.service.processor;

import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
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

    public ErpMfgMaterialIssue confirm(Long issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = requireIssue(issueId, context);
        String status = issue.getDocStatus();
        // 幂等：已 DONE（已出库）直接返回，不重复触发库存出库（state-machine §4）
        if (status != null && Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DONE)) {
            return issue;
        }
        if (status == null || !Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DRAFT)) {
            throw illegalTransition(issue, status, "DRAFT");
        }
        List<ErpMfgMaterialIssueLine> lines = loadLines(issueId);
        if (lines.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_ISSUE_LINES_EMPTY)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode());
        }

        // issue-status DRAFT→CONFIRMED
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_CONFIRMED);
        issueDao().updateEntity(issue);

        // 构造出库移动单请求并生成（业务联动自动 DRAFT→CONFIRMED→DONE，扣减库存；幂等键防重复）
        StockMoveRequest request = stockMoveBuilder.build(issue, lines, context);
        ErpInvStockMove move = stockMoveBiz.generateMove(request, context);
        // 跨域 generateMove 推进至 DONE 并更新余额；刷盘使 DONE 状态落地当前事务连接
        ormTemplate.flushSession();

        // 回写 WorkOrderLine.actualQuantity（按领料行 workOrderLineId 匹配）
        writebackWorkOrderLineActualQty(lines, context);

        // issue-status CONFIRMED→DONE（已出库）；汇总领料出库流水 totalCost → WorkOrder.materialCost
        BigDecimal materialCostDelta = aggregateIssueMaterialCost(move, context);
        issue = requireIssue(issueId, context);
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DONE);
        issueDao().updateEntity(issue);

        applyMaterialCostToWorkOrder(issue.getWorkOrderId(), materialCostDelta, context);

        // 领料 GL 过账（Dr: WIP / Cr: Inventory），镜像 ProductionVarianceDispatcher 显式调用范式
        issuePostingDispatcher.dispatchIfApplicable(issueId);
        return issue;
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
