package app.erp.inv.biz;

import io.nop.api.core.annotations.data.DataBean;

/**
 * 期末存货成本兜底重算报告（{@code IErpInvCostingBiz.reclosePeriodCosts} 返回值）。
 *
 * <p>记录本期扫描的移动单数与补算的入库成本层 / 出库 COGS 流水条数。正常路径（{@code FifoCostingStrategy}
 * 在 DONE 时已维护成本层）下补算数应为 0；非 0 表示存在历史/异常单据（如成本核算开关曾关闭期间入库）经兜底修复。
 *
 * <p>权威：{@code docs/design/finance/period-close.md §步骤2}、{@code docs/plans/2026-07-02-1538-1-inventory-costing-engine.md} Phase 3。
 */
@DataBean
public class CostingRecloseReport {

    private Long periodId;
    private int scannedMoves;
    private int recomputedIncomingLayers;
    private int recomputedOutgoingLedgers;

    public CostingRecloseReport() {
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public int getScannedMoves() {
        return scannedMoves;
    }

    public void setScannedMoves(int scannedMoves) {
        this.scannedMoves = scannedMoves;
    }

    public int getRecomputedIncomingLayers() {
        return recomputedIncomingLayers;
    }

    public void setRecomputedIncomingLayers(int recomputedIncomingLayers) {
        this.recomputedIncomingLayers = recomputedIncomingLayers;
    }

    public int getRecomputedOutgoingLedgers() {
        return recomputedOutgoingLedgers;
    }

    public void setRecomputedOutgoingLedgers(int recomputedOutgoingLedgers) {
        this.recomputedOutgoingLedgers = recomputedOutgoingLedgers;
    }
}
