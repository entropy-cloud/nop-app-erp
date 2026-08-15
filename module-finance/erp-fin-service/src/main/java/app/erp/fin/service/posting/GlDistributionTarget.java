package app.erp.fin.service.posting;

import java.math.BigDecimal;

/**
 * GlDistribution 分摊规则目标行（RC-R1.41 / P1-RC-001）：命中规则的分录行按 percent 比例拆到
 * {@code targetCostCenterId} 目标成本中心。所有目标行 Σpercent 必须 = 100（校验见
 * {@link ErpFinGlDistributionValidator}）。
 */
public class GlDistributionTarget {

    /** 目标成本中心。 */
    private Long targetCostCenterId;
    /** 分摊比例（百分数，如 60 表示 60%）。 */
    private BigDecimal percent;

    public Long getTargetCostCenterId() {
        return targetCostCenterId;
    }

    public void setTargetCostCenterId(Long targetCostCenterId) {
        this.targetCostCenterId = targetCostCenterId;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }
}
