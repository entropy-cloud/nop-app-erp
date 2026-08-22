package app.erp.inv.biz;

import java.math.BigDecimal;

/**
 * 库存移动单行生成请求（{@link IErpInvStockMoveBiz#generateMove} 入参）。
 *
 * <p>跨域调用方（purchase/sales）按行传入物料/SKU、数量、批次等；单位成本可选（入库可由调用方提供采购价）。
 */
public class StockMoveLineRequest {
    private String materialId;
    private String skuId;
    private String uoMId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private String currencyId;
    private String batchNo;
    private String serialNo;
    private String sourceLocationId;
    private String destLocationId;
    private String remark;

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getUoMId() {
        return uoMId;
    }

    public void setUoMId(String uoMId) {
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

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
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

    public String getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(String sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getDestLocationId() {
        return destLocationId;
    }

    public void setDestLocationId(String destLocationId) {
        this.destLocationId = destLocationId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
