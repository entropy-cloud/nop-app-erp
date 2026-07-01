package app.erp.inv.biz;

import java.math.BigDecimal;

/**
 * 库存移动单行生成请求（{@link IErpInvStockMoveBiz#generateMove} 入参）。
 *
 * <p>跨域调用方（purchase/sales）按行传入物料/SKU、数量、批次等；单位成本可选（入库可由调用方提供采购价）。
 */
public class StockMoveLineRequest {
    private Long materialId;
    private Long skuId;
    private Long uoMId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private Long currencyId;
    private String batchNo;
    private String serialNo;
    private Long sourceLocationId;
    private Long destLocationId;
    private String remark;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getUoMId() {
        return uoMId;
    }

    public void setUoMId(Long uoMId) {
        this.uoMId = uoMId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Long getDestLocationId() {
        return destLocationId;
    }

    public void setDestLocationId(Long destLocationId) {
        this.destLocationId = destLocationId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
