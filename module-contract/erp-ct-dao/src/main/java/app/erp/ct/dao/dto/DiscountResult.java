package app.erp.ct.dao.dto;

import java.math.BigDecimal;

/**
 * 批量折扣解析结果（{@code IErpCtVolumeDiscountBiz.resolveDiscount} 返回值）。
 *
 * <p>{@code discountedUnitPrice}：命中区间带后的折后单价（覆盖价或原价×(1-折扣率)）。
 * {@code lineAmount}：行金额 = qty × discountedUnitPrice。无命中时回退原价。
 */
public class DiscountResult {
    private BigDecimal discountedUnitPrice;
    private BigDecimal lineAmount;
    private boolean bandMatched;

    public DiscountResult() {
    }

    public DiscountResult(BigDecimal discountedUnitPrice, BigDecimal lineAmount, boolean bandMatched) {
        this.discountedUnitPrice = discountedUnitPrice;
        this.lineAmount = lineAmount;
        this.bandMatched = bandMatched;
    }

    public BigDecimal getDiscountedUnitPrice() {
        return discountedUnitPrice;
    }

    public void setDiscountedUnitPrice(BigDecimal discountedUnitPrice) {
        this.discountedUnitPrice = discountedUnitPrice;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }

    public void setLineAmount(BigDecimal lineAmount) {
        this.lineAmount = lineAmount;
    }

    public boolean isBandMatched() {
        return bandMatched;
    }

    public void setBandMatched(boolean bandMatched) {
        this.bandMatched = bandMatched;
    }
}
