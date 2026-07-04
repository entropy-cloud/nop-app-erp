package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.time.LocalDateTime;

/**
 * 模拟排程的单个工序时间结果（{@code IErpApsAtpCtpService.simulateSchedule} 返回元素）。
 * 用于向用户展示承诺排程，不落库。
 */
@DataBean
public class ScheduledOperationView {

    private Long workcenterId;
    private String operationName;
    private LocalDateTime plannedStartDateT;
    private LocalDateTime plannedEndDateT;
    private long durationMinutes;

    public Long getWorkcenterId() {
        return workcenterId;
    }

    public void setWorkcenterId(Long workcenterId) {
        this.workcenterId = workcenterId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public LocalDateTime getPlannedStartDateT() {
        return plannedStartDateT;
    }

    public void setPlannedStartDateT(LocalDateTime plannedStartDateT) {
        this.plannedStartDateT = plannedStartDateT;
    }

    public LocalDateTime getPlannedEndDateT() {
        return plannedEndDateT;
    }

    public void setPlannedEndDateT(LocalDateTime plannedEndDateT) {
        this.plannedEndDateT = plannedEndDateT;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
