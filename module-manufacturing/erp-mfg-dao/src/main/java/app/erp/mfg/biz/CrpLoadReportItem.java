package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CRP 负荷报表行（workcenter×date 聚合）。{@code IErpMfgCrpBiz.getLoadReport} 返回元素。
 *
 * <p>字段含义（{@code docs/design/manufacturing/crp.md §负载报表}）：
 * <ul>
 *   <li>{@code loadHours}：该工作中心该日已占用工时（Σ ErpMfgCrpLoad.loadHours）。</li>
 *   <li>{@code setupHours}：该工作中心该日换模工时（Σ ErpMfgCrpLoad.setupHours）。</li>
 *   <li>{@code capacityHours}：该工作中心该日可用工时（WorkcenterCalendar 出勤时段）× 效率系数。</li>
 *   <li>{@code loadRate}：负荷率 = loadHours / capacityHours（capacityHours=0 时记 0 并据 loadHours 判 overloaded）。</li>
 *   <li>{@code overloaded}：loadRate &gt; {@code erp-mfg.crp-overload-threshold}（默认 1.0）。</li>
 * </ul>
 */
@DataBean
public class CrpLoadReportItem {

    private Long workcenterId;
    private String workcenterCode;
    private LocalDate loadDate;
    private BigDecimal loadHours;
    private BigDecimal setupHours;
    private BigDecimal capacityHours;
    private BigDecimal loadRate;
    private Boolean overloaded;

    public Long getWorkcenterId() {
        return workcenterId;
    }

    public void setWorkcenterId(Long workcenterId) {
        this.workcenterId = workcenterId;
    }

    public String getWorkcenterCode() {
        return workcenterCode;
    }

    public void setWorkcenterCode(String workcenterCode) {
        this.workcenterCode = workcenterCode;
    }

    public LocalDate getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(LocalDate loadDate) {
        this.loadDate = loadDate;
    }

    public BigDecimal getLoadHours() {
        return loadHours;
    }

    public void setLoadHours(BigDecimal loadHours) {
        this.loadHours = loadHours;
    }

    public BigDecimal getSetupHours() {
        return setupHours;
    }

    public void setSetupHours(BigDecimal setupHours) {
        this.setupHours = setupHours;
    }

    public BigDecimal getCapacityHours() {
        return capacityHours;
    }

    public void setCapacityHours(BigDecimal capacityHours) {
        this.capacityHours = capacityHours;
    }

    public BigDecimal getLoadRate() {
        return loadRate;
    }

    public void setLoadRate(BigDecimal loadRate) {
        this.loadRate = loadRate;
    }

    public Boolean getOverloaded() {
        return overloaded;
    }

    public void setOverloaded(Boolean overloaded) {
        this.overloaded = overloaded;
    }
}
