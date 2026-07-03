package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 作业卡报工录入请求（{@code IErpMfgJobCardBiz.recordWork} 入参）。
 *
 * <p>作业员按班次录入实际工时与产量；人工成本 = {@code durationMins/60 × hourlyRate} →
 * {@code ErpMfgJobCardTimeLog.laborCost}，累加回写 {@code ErpMfgWorkOrder.laborCost}。
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md §适用对象二}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 4。
 */
@DataBean
public class JobCardWorkRecord {

    private Long jobCardId;
    private String operatorId;
    private LocalDate workDate;
    private BigDecimal durationMins;
    private BigDecimal setupMins;
    private BigDecimal runMins;
    private BigDecimal hourlyRate;
    private BigDecimal completedQuantity;
    private BigDecimal scrappedQuantity;
    private String remark;

    public Long getJobCardId() {
        return jobCardId;
    }

    public void setJobCardId(Long jobCardId) {
        this.jobCardId = jobCardId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public BigDecimal getDurationMins() {
        return durationMins;
    }

    public void setDurationMins(BigDecimal durationMins) {
        this.durationMins = durationMins;
    }

    public BigDecimal getSetupMins() {
        return setupMins;
    }

    public void setSetupMins(BigDecimal setupMins) {
        this.setupMins = setupMins;
    }

    public BigDecimal getRunMins() {
        return runMins;
    }

    public void setRunMins(BigDecimal runMins) {
        this.runMins = runMins;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public BigDecimal getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(BigDecimal completedQuantity) {
        this.completedQuantity = completedQuantity;
    }

    public BigDecimal getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(BigDecimal scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
