package app.erp.sal.biz;

import java.math.BigDecimal;

/**
 * 换货出库单生成行 DTO（RC-R1.51 P1-RC-025，D1 选项 A 入参契约）。
 *
 * <p>换货商品/数量由操作员显式决策（L1「换发等值或不同货物」），默认复制退货行。
 * unitPrice 为操作员录入的换货价（不含税），行金额 = quantity × unitPrice，
 * 头金额聚合后参与价差计算（D3 头级口径 Δ = 换货出库单 totalAmountWithTax − 退货单 totalAmountWithTax）。
 */
public class ErpSalExchangeDeliveryLine {

    private String materialId;

    private String skuId;

    private String uoMId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
