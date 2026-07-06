package app.erp.md.dao.dto;

import java.math.BigDecimal;

/**
 * 价格校验结果（{@code IErpMdMaterialSkuBiz.validatePrice} 返回值，UC-MD-04）。
 *
 * <p>{@code passed}：是否放行（HARD 低于底线=false；WARN/OFF=true）。
 * {@code warning}：是否带警告（WARN 低于底线=true；其他=false）。
 * {@code minPrice}：生效的最低价底线（用于前端提示与日志）。
 * {@code level}：生效的价格校验级别（OFF/WARN/HARD）。
 */
public class PriceValidationResult {
    private boolean passed;
    private boolean warning;
    private BigDecimal minPrice;
    private String level;

    public PriceValidationResult() {
    }

    public PriceValidationResult(boolean passed, boolean warning, BigDecimal minPrice, String level) {
        this.passed = passed;
        this.warning = warning;
        this.minPrice = minPrice;
        this.level = level;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
