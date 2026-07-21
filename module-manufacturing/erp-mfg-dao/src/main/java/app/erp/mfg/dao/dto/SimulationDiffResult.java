package app.erp.mfg.dao.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * MRP 仿真版本对比结果（plan 2026-07-22-1000-2 §结果对比算法 Decision C；权威：
 * `docs/design/manufacturing/simulation-engine.md §结果对比算法`）。
 *
 * <p>4 维 diff：净需求差 / 建议量差 / 缺料物料集差 / 总采购额差。
 * 不可变快照可确定性派生，不持久化为实体（Decision C 裁决）。
 */
public class SimulationDiffResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long versionIdA;
    private Long versionIdB;
    private List<LineDiff> lineDiffs;
    private BigDecimal totalNetRequirementDelta;
    private BigDecimal totalPlannedQuantityDelta;
    private BigDecimal totalPurchaseAmountDelta;
    private List<Long> shortageOnlyInA;
    private List<Long> shortageOnlyInB;
    private List<Long> shortageInBoth;

    public Long getVersionIdA() { return versionIdA; }
    public void setVersionIdA(Long v) { this.versionIdA = v; }

    public Long getVersionIdB() { return versionIdB; }
    public void setVersionIdB(Long v) { this.versionIdB = v; }

    public List<LineDiff> getLineDiffs() { return lineDiffs; }
    public void setLineDiffs(List<LineDiff> l) { this.lineDiffs = l; }

    public BigDecimal getTotalNetRequirementDelta() { return totalNetRequirementDelta; }
    public void setTotalNetRequirementDelta(BigDecimal v) { this.totalNetRequirementDelta = v; }

    public BigDecimal getTotalPlannedQuantityDelta() { return totalPlannedQuantityDelta; }
    public void setTotalPlannedQuantityDelta(BigDecimal v) { this.totalPlannedQuantityDelta = v; }

    public BigDecimal getTotalPurchaseAmountDelta() { return totalPurchaseAmountDelta; }
    public void setTotalPurchaseAmountDelta(BigDecimal v) { this.totalPurchaseAmountDelta = v; }

    public List<Long> getShortageOnlyInA() { return shortageOnlyInA; }
    public void setShortageOnlyInA(List<Long> l) { this.shortageOnlyInA = l; }

    public List<Long> getShortageOnlyInB() { return shortageOnlyInB; }
    public void setShortageOnlyInB(List<Long> l) { this.shortageOnlyInB = l; }

    public List<Long> getShortageInBoth() { return shortageInBoth; }
    public void setShortageInBoth(List<Long> l) { this.shortageInBoth = l; }

    /**
     * 单物料维度 diff（B - A 符号约定：正值表示 B 多于 A）。
     */
    public static class LineDiff implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long materialId;
        private BigDecimal netRequirementA;
        private BigDecimal netRequirementB;
        private BigDecimal netRequirementDelta;
        private BigDecimal plannedQuantityA;
        private BigDecimal plannedQuantityB;
        private BigDecimal plannedQuantityDelta;

        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long v) { this.materialId = v; }

        public BigDecimal getNetRequirementA() { return netRequirementA; }
        public void setNetRequirementA(BigDecimal v) { this.netRequirementA = v; }

        public BigDecimal getNetRequirementB() { return netRequirementB; }
        public void setNetRequirementB(BigDecimal v) { this.netRequirementB = v; }

        public BigDecimal getNetRequirementDelta() { return netRequirementDelta; }
        public void setNetRequirementDelta(BigDecimal v) { this.netRequirementDelta = v; }

        public BigDecimal getPlannedQuantityA() { return plannedQuantityA; }
        public void setPlannedQuantityA(BigDecimal v) { this.plannedQuantityA = v; }

        public BigDecimal getPlannedQuantityB() { return plannedQuantityB; }
        public void setPlannedQuantityB(BigDecimal v) { this.plannedQuantityB = v; }

        public BigDecimal getPlannedQuantityDelta() { return plannedQuantityDelta; }
        public void setPlannedQuantityDelta(BigDecimal v) { this.plannedQuantityDelta = v; }
    }
}
