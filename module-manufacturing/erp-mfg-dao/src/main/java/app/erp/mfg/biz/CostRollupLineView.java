package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;

/**
 * 成本卷算行视图（{@code IErpMfgBomBiz.rollupCost} 返回结果的元素）。对应一条 {@code ErpMfgCostRollupLine}，
 * 所有金额均为**单位标准成本**（per unit）。
 */
@DataBean
public class CostRollupLineView {

    private Long materialId;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;

    public CostRollupLineView() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public BigDecimal getOverheadCost() {
        return overheadCost;
    }

    public void setOverheadCost(BigDecimal overheadCost) {
        this.overheadCost = overheadCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
}
