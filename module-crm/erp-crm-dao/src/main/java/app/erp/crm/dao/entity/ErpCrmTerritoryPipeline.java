package app.erp.crm.dao.entity;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;

/**
 * 区域管道对比报表的结构化返回（{@code IErpCrmQuotaBiz.getTerritoryPipeline} 返回值）。
 *
 * <p>同屏返回目标（QuotaSummary）/ 预测（ForecastSummary）/ 实际收入聚合三段，
 * 对齐 {@code docs/design/crm/territory.md §业务规则 3}（实际/预测/目标同屏对比）。
 */
@DataBean
public class ErpCrmTerritoryPipeline {

    private Long territoryId;
    private String periodLabel;
    private QuotaSummary quota;
    private ForecastSummary forecast;
    private ActualSummary actual;

    public Long getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(Long territoryId) {
        this.territoryId = territoryId;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public QuotaSummary getQuota() {
        return quota;
    }

    public void setQuota(QuotaSummary quota) {
        this.quota = quota;
    }

    public ForecastSummary getForecast() {
        return forecast;
    }

    public void setForecast(ForecastSummary forecast) {
        this.forecast = forecast;
    }

    public ActualSummary getActual() {
        return actual;
    }

    public void setActual(ActualSummary actual) {
        this.actual = actual;
    }

    /** 目标（Quota）聚合段。 */
    @DataBean
    public static class QuotaSummary {
        private BigDecimal quotaAmount;
        private boolean finalized;
        private String periodType;

        public BigDecimal getQuotaAmount() {
            return quotaAmount;
        }

        public void setQuotaAmount(BigDecimal quotaAmount) {
            this.quotaAmount = quotaAmount;
        }

        public boolean isFinalized() {
            return finalized;
        }

        public void setFinalized(boolean finalized) {
            this.finalized = finalized;
        }

        public String getPeriodType() {
            return periodType;
        }

        public void setPeriodType(String periodType) {
            this.periodType = periodType;
        }
    }

    /** 预测（Forecast）聚合段：commit/upside/best-case/weighted 聚合到 territoryId 维度。 */
    @DataBean
    public static class ForecastSummary {
        private BigDecimal commitAmount;
        private BigDecimal upsideAmount;
        private BigDecimal bestCaseAmount;
        private BigDecimal weightedAmount;
        private int opportunityCount;

        public BigDecimal getCommitAmount() {
            return commitAmount;
        }

        public void setCommitAmount(BigDecimal commitAmount) {
            this.commitAmount = commitAmount;
        }

        public BigDecimal getUpsideAmount() {
            return upsideAmount;
        }

        public void setUpsideAmount(BigDecimal upsideAmount) {
            this.upsideAmount = upsideAmount;
        }

        public BigDecimal getBestCaseAmount() {
            return bestCaseAmount;
        }

        public void setBestCaseAmount(BigDecimal bestCaseAmount) {
            this.bestCaseAmount = bestCaseAmount;
        }

        public BigDecimal getWeightedAmount() {
            return weightedAmount;
        }

        public void setWeightedAmount(BigDecimal weightedAmount) {
            this.weightedAmount = weightedAmount;
        }

        public int getOpportunityCount() {
            return opportunityCount;
        }

        public void setOpportunityCount(int opportunityCount) {
            this.opportunityCount = opportunityCount;
        }
    }

    /** 实际收入聚合段：区域内已 CONVERTED 商机的 expectedRevenue 求和。 */
    @DataBean
    public static class ActualSummary {
        private BigDecimal actualRevenue;
        private int convertedCount;

        public BigDecimal getActualRevenue() {
            return actualRevenue;
        }

        public void setActualRevenue(BigDecimal actualRevenue) {
            this.actualRevenue = actualRevenue;
        }

        public int getConvertedCount() {
            return convertedCount;
        }

        public void setConvertedCount(int convertedCount) {
            this.convertedCount = convertedCount;
        }
    }
}
