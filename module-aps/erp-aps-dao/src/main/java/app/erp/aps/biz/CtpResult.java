package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.time.LocalDateTime;

/**
 * CTP（Capable-to-Promise）交期承诺模拟结果（{@code scheduling.md §7.3}）。
 *
 * <p>{@code feasible=true} 表示期望交期可达；{@code false} 时 {@code earliestCompletionDate}
 * 为模拟得出的最早可交付时间，{@code bottleneckWorkcenter}/{@code capacityGapMinutes} 描述瓶颈。
 */
@DataBean
public class CtpResult {

    private boolean feasible;
    private LocalDateTime earliestCompletionDate;
    private String bottleneckWorkcenter;
    private long capacityGapMinutes;
    /** 不可行时的原因说明（如无工艺路线/无可用时段）。 */
    private String reason;

    public boolean isFeasible() {
        return feasible;
    }

    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    public LocalDateTime getEarliestCompletionDate() {
        return earliestCompletionDate;
    }

    public void setEarliestCompletionDate(LocalDateTime earliestCompletionDate) {
        this.earliestCompletionDate = earliestCompletionDate;
    }

    public String getBottleneckWorkcenter() {
        return bottleneckWorkcenter;
    }

    public void setBottleneckWorkcenter(String bottleneckWorkcenter) {
        this.bottleneckWorkcenter = bottleneckWorkcenter;
    }

    public long getCapacityGapMinutes() {
        return capacityGapMinutes;
    }

    public void setCapacityGapMinutes(long capacityGapMinutes) {
        this.capacityGapMinutes = capacityGapMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
