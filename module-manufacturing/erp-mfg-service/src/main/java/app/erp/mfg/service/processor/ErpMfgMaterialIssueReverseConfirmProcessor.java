package app.erp.mfg.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpMfgMaterialIssue reverseConfirm per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含领料红冲编排（红冲 MANUFACTURING_ISSUE 凭证 → 反向 OUTGOING 库存移动单 → 镜像回退 mfg 累计字段
 * → posted=false + docStatus=CANCELLED）；从 ErpMfgMaterialIssueBizModel 内联 @BizMutation 提取。
 * 共享 protected helper 单一真相源在 {@link AbstractErpMfgMaterialIssueProcessor}。
 *
 * <p>P1-CK-mfg-005 修复：任一步失败时<b>中止红冲</b>（抛 {@code NopException} 回滚整个 @BizMutation，
 * 状态保持 DONE+posted=true 可重试）。修复前吞异常后仍翻 CANCELLED+posted=false，红冲入口被守卫永久关闭，
 * 悬挂不可恢复且无告警。P1-CK-mfg-003 修复：红冲成功后镜像回退 mfg 侧累计字段
 * （WorkOrder.materialCost / WorkOrderLine.actualQuantity / 预留 consumedQuantity），
 * 修复前 GL 与库存正确回滚但 mfg 聚合字段单向残留。
 */
public class ErpMfgMaterialIssueReverseConfirmProcessor extends AbstractErpMfgMaterialIssueProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMfgMaterialIssueReverseConfirmProcessor.class);

    public ErpMfgMaterialIssue reverseConfirm(String issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = requireIssue(issueId, context);
        validateCanReverse(issue, context);

        // 1. 红冲 MANUFACTURING_ISSUE 凭证（P1-CK-mfg-005：失败中止红冲——吞异常使 GL 悬挂且入口封死，
        //    改为抛错回滚整个事务，状态保持 DONE+posted=true 可重试）。
        issuePostingDispatcher.reverse(issue);

        // 2. 反向 OUTGOING 库存移动单（经 relatedBillType+relatedBillCode 反查原移动单 → IErpInvStockMoveBiz.reverse
        //    生成 REVERSAL 反向移动单，余额自动回滚；失败中止红冲同步骤 1）。
        ErpInvStockMove originalMove = findIssueMove(issue.getCode());
        if (originalMove != null) {
            stockMoveBiz.reverse(originalMove.getId(), context);
        }

        // 3. P1-CK-mfg-003：镜像回退 mfg 侧累计字段（正向 confirm 的 writebackWorkOrderLineActualQty /
        //    applyMaterialCostToWorkOrder / consumeReservations 逆操作）。GL 与库存已回滚，此处回退
        //    WorkOrder.materialCost / WorkOrderLine.actualQuantity / 预留 consumedQuantity，
        //    避免业账两面失配。
        List<ErpMfgMaterialIssueLine> lines = loadLines(issueId);
        rollbackWorkOrderLineActualQty(lines, context);
        rollbackMaterialCostToWorkOrder(issue.getWorkOrderId(), originalMove, context);
        unconsumeReservations(issue, lines, context);

        // 4. 翻 posted=false + docStatus=CANCELLED（状态机终态）。
        //    跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 confirm 范式）。
        issue = requireIssue(issueId, context);
        doReverseConfirm(issue, context);
        return issue;
    }

    // ---------- step：红冲守卫与终态（protected，供派生复用与覆盖） ----------

    /**
     * 红冲前置守卫：仅 posted=true 且 docStatus=DONE（已 confirm 出库）的领料单可红冲。
     *
     * <p>posted 判定为动态业务守卫保留原位（未过账抛 {@code ERR_MATERIAL_ISSUE_NOT_POSTED}）；
     * 固定状态边守卫（仅 DONE）委托 {@code ErpMfgMaterialIssueStateMachine.assertCanReverseConfirm}
     * （M4.39；posted=true 时 docStatus 必为 DONE，状态守卫为矩阵防御，映射同码保持行为一致）。
     */
    protected void validateCanReverse(ErpMfgMaterialIssue issue, IServiceContext context) {
        String status = issue.getDocStatus();
        if (!Boolean.TRUE.equals(issue.getPosted())) {
            throw new NopException(ErpMfgErrors.ERR_MATERIAL_ISSUE_NOT_POSTED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode());
        }
        try {
            stateMachine.assertCanReverseConfirm(status);
        } catch (NopException e) {
            throw new NopException(ErpMfgErrors.ERR_MATERIAL_ISSUE_NOT_POSTED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode());
        }
    }

    /**
     * 翻 posted=false + docStatus=CANCELLED（红冲终态）。对齐 confirm 反向操作。
     */
    protected void doReverseConfirm(ErpMfgMaterialIssue issue, IServiceContext context) {
        issue.setDocStatus(stateMachine.reverseConfirmTargetStatus());
        issue.setPosted(false);
        issueDao().updateEntity(issue);
    }

    // ---------- step：mfg 侧累计字段镜像回退（P1-CK-mfg-003，protected） ----------

    /**
     * 回退 WorkOrderLine.actualQuantity（正向 confirm 的 {@code writebackWorkOrderLineActualQty} 逆操作）。
     * 按领料行 workOrderLineId 匹配，逐行减回 issued 数量。
     */
    protected void rollbackWorkOrderLineActualQty(List<ErpMfgMaterialIssueLine> lines, IServiceContext context) {
        Map<String, BigDecimal> byWorkOrderLine = new HashMap<>();
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
        for (Map.Entry<String, BigDecimal> e : byWorkOrderLine.entrySet()) {
            ErpMfgWorkOrderLine wol = workOrderLineBiz.get(e.getKey(), false, context);
            if (wol == null) {
                continue;
            }
            BigDecimal rolledBack = nz(wol.getActualQuantity()).subtract(e.getValue()).max(BigDecimal.ZERO);
            wol.setActualQuantity(rolledBack);
            workOrderLineBiz.updateEntity(wol, null, context);
        }
    }

    /**
     * 回退 WorkOrder.materialCost（正向 confirm 的 {@code applyMaterialCostToWorkOrder} 逆操作）。
     * 以 REVERSAL 移动单流水的 totalCost 绝对值扣减（正向为出库流水 totalCost 绝对值）。
     * originalMove 为 null（无移动单）时跳过（与正向 applyMaterialCost 的移动单依赖一致）。
     */
    protected void rollbackMaterialCostToWorkOrder(String workOrderId, ErpInvStockMove originalMove,
                                                   IServiceContext context) {
        if (workOrderId == null || originalMove == null) {
            return;
        }
        BigDecimal materialCostDelta = aggregateIssueMaterialCost(originalMove, context);
        if (materialCostDelta == null || materialCostDelta.signum() == 0) {
            return;
        }
        var wo = workOrderBiz.get(workOrderId, false, context);
        if (wo == null) {
            return;
        }
        wo.setMaterialCost(nz(wo.getMaterialCost()).subtract(materialCostDelta).max(BigDecimal.ZERO));
        recomputeTotals(wo);
        workOrderBiz.updateEntity(wo, null, context);
    }

    /** 汇总移动单流水 totalCost 绝对值（与正向 confirm 的 aggregateIssueMaterialCost 同口径）。 */
    private BigDecimal aggregateIssueMaterialCost(ErpInvStockMove move, IServiceContext context) {
        var q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("moveId", move.getId()));
        var ledgers = stockLedgerBiz.findList(q, null, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (var l : ledgers) {
            sum = sum.add(nz(l.getTotalCost()));
        }
        return sum.abs();
    }
}
