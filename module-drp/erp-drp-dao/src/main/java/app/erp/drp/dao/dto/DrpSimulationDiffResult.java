package app.erp.drp.dao.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DRP 仿真版本对比结果（plan 2026-07-22-1000-2 §结果对比算法 Decision C；权威：
 * `docs/design/manufacturing/simulation-engine.md §结果对比算法`）。
 *
 * <p>2 维 diff（B - A 符号约定）：补货量差 / 安全库存差。
 */
public class DrpSimulationDiffResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long versionIdA;
    private Long versionIdB;
    private List<LineDiff> lineDiffs;
    private BigDecimal totalReplenishmentQtyDelta;
    private BigDecimal totalSafetyStockDelta;

    public Long getVersionIdA() { return versionIdA; }
    public void setVersionIdA(Long v) { this.versionIdA = v; }

    public Long getVersionIdB() { return versionIdB; }
    public void setVersionIdB(Long v) { this.versionIdB = v; }

    public List<LineDiff> getLineDiffs() { return lineDiffs; }
    public void setLineDiffs(List<LineDiff> l) { this.lineDiffs = l; }

    public BigDecimal getTotalReplenishmentQtyDelta() { return totalReplenishmentQtyDelta; }
    public void setTotalReplenishmentQtyDelta(BigDecimal v) { this.totalReplenishmentQtyDelta = v; }

    public BigDecimal getTotalSafetyStockDelta() { return totalSafetyStockDelta; }
    public void setTotalSafetyStockDelta(BigDecimal v) { this.totalSafetyStockDelta = v; }

    public static class LineDiff implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long materialId;
        private Long warehouseId;
        private BigDecimal suggestedQtyA;
        private BigDecimal suggestedQtyB;
        private BigDecimal replenishmentQtyDelta;
        private BigDecimal safetyStockA;
        private BigDecimal safetyStockB;
        private BigDecimal safetyStockDelta;

        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long v) { this.materialId = v; }

        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long v) { this.warehouseId = v; }

        public BigDecimal getSuggestedQtyA() { return suggestedQtyA; }
        public void setSuggestedQtyA(BigDecimal v) { this.suggestedQtyA = v; }

        public BigDecimal getSuggestedQtyB() { return suggestedQtyB; }
        public void setSuggestedQtyB(BigDecimal v) { this.suggestedQtyB = v; }

        public BigDecimal getReplenishmentQtyDelta() { return replenishmentQtyDelta; }
        public void setReplenishmentQtyDelta(BigDecimal v) { this.replenishmentQtyDelta = v; }

        public BigDecimal getSafetyStockA() { return safetyStockA; }
        public void setSafetyStockA(BigDecimal v) { this.safetyStockA = v; }

        public BigDecimal getSafetyStockB() { return safetyStockB; }
        public void setSafetyStockB(BigDecimal v) { this.safetyStockB = v; }

        public BigDecimal getSafetyStockDelta() { return safetyStockDelta; }
        public void setSafetyStockDelta(BigDecimal v) { this.safetyStockDelta = v; }
    }
}
