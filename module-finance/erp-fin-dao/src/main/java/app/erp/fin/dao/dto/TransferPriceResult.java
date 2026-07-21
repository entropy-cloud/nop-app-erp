package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 转移定价解析结果 DTO（plan 2026-07-22-1000-1 A3，multi-company.md §转移定价规则模型）。
 *
 * <p>由 {@code app.erp.fin.dao.api.IErpFinTransferPriceResolver.resolvePrice} 返回，承载命中规则的定价方法 + 计算后单价。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §转移定价规则模型}。
 */
public class TransferPriceResult {
    private String pricingMethod;
    private BigDecimal unitPrice;
    private Long ruleId;
    private String ruleCode;

    public String getPricingMethod() {
        return pricingMethod;
    }

    public void setPricingMethod(String pricingMethod) {
        this.pricingMethod = pricingMethod;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }
}
