package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;

/**
 * BOM 多级展开结果节点（扁平化）。{@code IErpMfgBomBiz.explode} 的返回元素。
 *
 * <p>{@link #quantity} 为相对根产出 {@code requestedQty} 的有效用量（已按 {@code line.quantity × requestedQty / BOM.qty}
 * 逐层乘积）；{@link #level} 根的直接子件为 1，逐层递增；虚拟件（phantom）本身不出现在结果中，
 * 其子件并入父级层级（见 {@code bom-and-routing.md §多级 BOM 展开}）。
 */
@DataBean
public class BomExplosionNode {

    private Long materialId;
    private BigDecimal quantity;
    private Long operationId;
    private Long sourceBomId;
    private int level;
    private boolean manufactured;

    public BomExplosionNode() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public Long getSourceBomId() {
        return sourceBomId;
    }

    public void setSourceBomId(Long sourceBomId) {
        this.sourceBomId = sourceBomId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isManufactured() {
        return manufactured;
    }

    public void setManufactured(boolean manufactured) {
        this.manufactured = manufactured;
    }
}
