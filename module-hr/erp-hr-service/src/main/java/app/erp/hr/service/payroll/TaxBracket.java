package app.erp.hr.service.payroll;

import java.math.BigDecimal;

/**
 * 个税税率表单档（payroll.md §4.2 综合所得七级超额累进税率）。
 */
public class TaxBracket {
    /** 累计应纳税所得额上界（最后一档为 null 或极大数表示 >上限）。 */
    private final BigDecimal rangeUpperLimit;
    private final BigDecimal rate;
    private final BigDecimal quickDeduction;

    public TaxBracket(BigDecimal rangeUpperLimit, BigDecimal rate, BigDecimal quickDeduction) {
        this.rangeUpperLimit = rangeUpperLimit;
        this.rate = rate;
        this.quickDeduction = quickDeduction;
    }

    public BigDecimal getRangeUpperLimit() {
        return rangeUpperLimit;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getQuickDeduction() {
        return quickDeduction;
    }
}
