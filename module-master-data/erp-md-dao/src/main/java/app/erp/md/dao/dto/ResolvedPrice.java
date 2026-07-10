package app.erp.md.dao.dto;

import java.math.BigDecimal;

/**
 * 客户价格清单解析结果（{@code IErpMdCustomerPriceResolver.resolveCustomerPrice} 返回值）。
 *
 * <p>{@code unitPrice}：命中的协议单价。
 * {@code source}：取价来源标记（如 PRICE_LIST），用于审计追踪。
 * {@code priceListId}/{@code priceListName}：命中的价格清单，用于前端提示与日志。
 */
public class ResolvedPrice {
    private BigDecimal unitPrice;
    private String source;
    private Long priceListId;
    private String priceListName;

    public ResolvedPrice() {
    }

    public ResolvedPrice(BigDecimal unitPrice, String source, Long priceListId, String priceListName) {
        this.unitPrice = unitPrice;
        this.source = source;
        this.priceListId = priceListId;
        this.priceListName = priceListName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public String getPriceListName() {
        return priceListName;
    }

    public void setPriceListName(String priceListName) {
        this.priceListName = priceListName;
    }
}
