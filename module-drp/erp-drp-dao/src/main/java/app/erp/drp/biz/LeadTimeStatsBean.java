package app.erp.drp.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 提前期统计分析结果 DTO（{@link IErpInvDrpLeadTimeRecordBiz#findLeadTimeStats} 返回，
 * RC-R1.82 / P1-RC-082，UC-DRP-08；权威：docs/design/drp/lead-time-tracking.md §提前期统计分析）。
 *
 * <p>粒度由查询参数组合决定：supplierId + materialId = 供应商+物料级（最细，DRP 动态参数调整用）；
 * 仅 supplierId = 供应商级；仅 materialId = 物料级（跨供应商基线比较）。
 *
 * <p>指标口径：μ/σ/min/max/中位数基于 actualLeadTime 全样本（记录创建已保证订单/收货日期齐全，
 * 「订单/收货日期缺失行不入统计」在写入侧守卫）；准时率 = isOnTime=true 数 / 已判定数
 * （expectedLeadTime 缺失行不可判定，不入分母——owner doc 实现注记）；变异系数 = σ/μ（μ≤0 时 null）。
 */
@DataBean
public class LeadTimeStatsBean {

    private Long supplierId;
    private Long materialId;
    private Integer sampleCount;
    private BigDecimal avgLeadTime;
    private BigDecimal leadTimeStdDev;
    private Integer minLeadTime;
    private Integer maxLeadTime;
    private BigDecimal medianLeadTime;
    private Integer onTimeCount;
    private Integer judgedCount;
    private BigDecimal onTimeRate;
    private BigDecimal variationCoefficient;
    private LocalDate windowFrom;
    private LocalDate windowTo;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public BigDecimal getAvgLeadTime() {
        return avgLeadTime;
    }

    public void setAvgLeadTime(BigDecimal avgLeadTime) {
        this.avgLeadTime = avgLeadTime;
    }

    public BigDecimal getLeadTimeStdDev() {
        return leadTimeStdDev;
    }

    public void setLeadTimeStdDev(BigDecimal leadTimeStdDev) {
        this.leadTimeStdDev = leadTimeStdDev;
    }

    public Integer getMinLeadTime() {
        return minLeadTime;
    }

    public void setMinLeadTime(Integer minLeadTime) {
        this.minLeadTime = minLeadTime;
    }

    public Integer getMaxLeadTime() {
        return maxLeadTime;
    }

    public void setMaxLeadTime(Integer maxLeadTime) {
        this.maxLeadTime = maxLeadTime;
    }

    public BigDecimal getMedianLeadTime() {
        return medianLeadTime;
    }

    public void setMedianLeadTime(BigDecimal medianLeadTime) {
        this.medianLeadTime = medianLeadTime;
    }

    public Integer getOnTimeCount() {
        return onTimeCount;
    }

    public void setOnTimeCount(Integer onTimeCount) {
        this.onTimeCount = onTimeCount;
    }

    public Integer getJudgedCount() {
        return judgedCount;
    }

    public void setJudgedCount(Integer judgedCount) {
        this.judgedCount = judgedCount;
    }

    public BigDecimal getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(BigDecimal onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public BigDecimal getVariationCoefficient() {
        return variationCoefficient;
    }

    public void setVariationCoefficient(BigDecimal variationCoefficient) {
        this.variationCoefficient = variationCoefficient;
    }

    public LocalDate getWindowFrom() {
        return windowFrom;
    }

    public void setWindowFrom(LocalDate windowFrom) {
        this.windowFrom = windowFrom;
    }

    public LocalDate getWindowTo() {
        return windowTo;
    }

    public void setWindowTo(LocalDate windowTo) {
        this.windowTo = windowTo;
    }
}
