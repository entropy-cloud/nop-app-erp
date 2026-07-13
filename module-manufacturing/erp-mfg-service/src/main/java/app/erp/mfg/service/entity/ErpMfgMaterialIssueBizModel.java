
package app.erp.mfg.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.biz.IErpMfgMaterialIssueBiz;
import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.biz.IErpMfgWorkOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.posting.ManufacturingIssuePostingDispatcher;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.biz.IErpInvStockLedgerBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 领料单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现领料确认→出库移动单 + 材料成本回写。
 *
 * <p>{@link #confirm}：issue-status DRAFT→CONFIRMED→DONE；DONE 时调 {@link IErpInvStockMoveBiz#generateMove}
 * （出库方向、moveType=OUTGOING、relatedBillType=ERP_MFG_ISSUE、业务联动自动 DRAFT→DONE 扣减库存）；
 * 回写 {@link ErpMfgWorkOrderLine#getActualQuantity}；汇总领料出库流水 {@link ErpInvStockLedger#getTotalCost}
 * → {@link ErpMfgWorkOrder#getMaterialCost}。
 *
 * <p>幂等：已 DONE 的领料单重复确认为空操作（state-machine §4）。幂等键 {@code (ERP_MFG_ISSUE, issue.code)}
 * 由 {@code generateMove} 防重复触发。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：库存出库经 {@link IErpInvStockMoveBiz}；工单/行回写用基类
 * {@link CrudBizModel#daoFor}（同聚合跨实体，父工单已由业务上下文授权）。
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md}、{@code docs/design/inventory/cross-domain.md}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 3。
 */
@BizModel("ErpMfgMaterialIssue")
public class ErpMfgMaterialIssueBizModel extends CrudBizModel<ErpMfgMaterialIssue> implements IErpMfgMaterialIssueBiz {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpInvStockLedgerBiz stockLedgerBiz;
    @Inject
    MaterialIssueStockMoveBuilder stockMoveBuilder;
    @Inject
    IErpMfgWorkOrderBiz workOrderBiz;
    @Inject
    IErpMfgWorkOrderLineBiz workOrderLineBiz;
    @Inject
    ManufacturingIssuePostingDispatcher issuePostingDispatcher;

    public ErpMfgMaterialIssueBizModel() {
        setEntityName(ErpMfgMaterialIssue.class.getName());
    }

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setStockMoveBuilder(MaterialIssueStockMoveBuilder stockMoveBuilder) {
        this.stockMoveBuilder = stockMoveBuilder;
    }

    @Override
    @BizMutation
    public ErpMfgMaterialIssue confirm(@Name("issueId") Long issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = requireEntity(String.valueOf(issueId), null, context);
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
        updateEntity(issue, null, context);

        // 构造出库移动单请求并生成（业务联动自动 DRAFT→CONFIRMED→DONE，扣减库存；幂等键防重复）
        StockMoveRequest request = stockMoveBuilder.build(issue, lines, context);
        ErpInvStockMove move = stockMoveBiz.generateMove(request, context);
        // 跨域 generateMove 推进至 DONE 并更新余额；刷盘使 DONE 状态落地当前事务连接
        orm().flushSession();

        // 回写 WorkOrderLine.actualQuantity（按领料行 workOrderLineId 匹配）
        writebackWorkOrderLineActualQty(lines, context);

        // issue-status CONFIRMED→DONE（已出库）；汇总领料出库流水 totalCost → WorkOrder.materialCost
        BigDecimal materialCostDelta = aggregateIssueMaterialCost(move, context);
        issue = requireEntity(String.valueOf(issueId), null, context);
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DONE);
        updateEntity(issue, null, context);

        applyMaterialCostToWorkOrder(issue.getWorkOrderId(), materialCostDelta, context);

        // 领料 GL 过账（Dr: WIP / Cr: Inventory），镜像 ProductionVarianceDispatcher 显式调用范式
        issuePostingDispatcher.dispatchIfApplicable(issueId);
        return issue;
    }

    private void writebackWorkOrderLineActualQty(List<ErpMfgMaterialIssueLine> lines, IServiceContext context) {
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
    private BigDecimal aggregateIssueMaterialCost(ErpInvStockMove move, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", move.getId()));
        List<ErpInvStockLedger> ledgers = stockLedgerBiz.findList(q, null, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpInvStockLedger l : ledgers) {
            sum = sum.add(nz(l.getTotalCost()));
        }
        return sum.abs();
    }

    private void applyMaterialCostToWorkOrder(Long workOrderId, BigDecimal materialCostDelta, IServiceContext context) {
        if (workOrderId == null || materialCostDelta == null || materialCostDelta.signum() == 0) {
            return;
        }
        ErpMfgWorkOrder wo = workOrderBiz.get(String.valueOf(workOrderId), false, context);
        if (wo == null) {
            return;
        }
        wo.setMaterialCost(nz(wo.getMaterialCost()).add(materialCostDelta));
        recomputeTotals(wo);
        workOrderBiz.updateEntity(wo, null, context);
    }

    static void recomputeTotals(ErpMfgWorkOrder wo) {
        BigDecimal total = nz(wo.getMaterialCost()).add(nz(wo.getLaborCost()))
                .add(nz(wo.getOverheadCost())).add(nz(wo.getSubcontractCost()));
        wo.setTotalCost(total);
        BigDecimal completed = nz(wo.getCompletedQuantity());
        wo.setUnitCost(completed.signum() != 0 ? total.divide(completed, 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
    }

    List<ErpMfgMaterialIssueLine> loadLines(Long issueId) {
        // 同聚合子表加载，父领料单已由 requireEntity 经管道授权
        IEntityDao<ErpMfgMaterialIssueLine> dao = daoFor(ErpMfgMaterialIssueLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("issueId", issueId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private NopException illegalTransition(ErpMfgMaterialIssue issue, String current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 注意：confirm 等复杂 mutation 在响应序列化时 ORM 会话可能已关闭，
    // batchLoadProps 会失败。此时安全降级返回 null（grid 列表页 findList 会话活跃不受影响）。

    @BizLoader(forType = ErpMfgMaterialIssue.class)
    public List<String> workOrderNo(@ContextSource List<ErpMfgMaterialIssue> issues) {
        if (!safeBatchLoad(issues, "workOrder")) {
            return nulls(issues);
        }
        List<String> result = new ArrayList<>(issues.size());
        for (ErpMfgMaterialIssue issue : issues) {
            result.add(issue.getWorkOrder() != null ? issue.getWorkOrder().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgMaterialIssue.class)
    public List<String> warehouseName(@ContextSource List<ErpMfgMaterialIssue> issues) {
        if (!safeBatchLoad(issues, "warehouse")) {
            return nulls(issues);
        }
        List<String> result = new ArrayList<>(issues.size());
        for (ErpMfgMaterialIssue issue : issues) {
            result.add(issue.getWarehouse() != null ? issue.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgMaterialIssue.class)
    public List<String> orgName(@ContextSource List<ErpMfgMaterialIssue> issues) {
        if (!safeBatchLoad(issues, "org")) {
            return nulls(issues);
        }
        List<String> result = new ArrayList<>(issues.size());
        for (ErpMfgMaterialIssue issue : issues) {
            result.add(issue.getOrg() != null ? issue.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgMaterialIssue.class)
    public List<String> currencyName(@ContextSource List<ErpMfgMaterialIssue> issues) {
        if (!safeBatchLoad(issues, "currency")) {
            return nulls(issues);
        }
        List<String> result = new ArrayList<>(issues.size());
        for (ErpMfgMaterialIssue issue : issues) {
            result.add(issue.getCurrency() != null ? issue.getCurrency().getName() : null);
        }
        return result;
    }

    private boolean safeBatchLoad(List<?> entities, String prop) {
        try {
            orm().batchLoadProps(entities, Collections.singleton(prop));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static List<String> nulls(List<?> entities) {
        List<String> result = new ArrayList<>(entities.size());
        for (int i = 0, n = entities.size(); i < n; i++) {
            result.add(null);
        }
        return result;
    }
}
