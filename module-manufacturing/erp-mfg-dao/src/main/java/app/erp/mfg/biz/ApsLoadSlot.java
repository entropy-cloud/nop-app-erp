package app.erp.mfg.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * APS 排程时段 DTO（{@link IErpApsLoadSourceProvider#findScheduledSlots} 返回元素，plan 2026-07-05-0306-2）。
 *
 * <p>对应 APS {@code ErpApsOperationOrder} 已排程（PLANNED）的单个工序时段，由 aps-service 实现组装；
 * CRP {@code CrpLoadCalculator} 据此将负荷精确分派到 workcenter×date 而非按 WorkOrder 计划日期均匀分摊。
 *
 * <p>字段映射（APS OperationOrder → 本 DTO）：
 * <ul>
 *   <li>{@code workOrderId} ← {@code ErpApsOperationOrder.workOrderId}</li>
 *   <li>{@code sequence} ← {@code ErpApsOperationOrder.sequence}（工序顺序，CRP 不直接消费，调试/排序用）</li>
 *   <li>{@code workcenterId} ← {@code ErpApsOperationOrder.machineId}（APS 称"设备/工作中心"，CRP 侧统称 workcenterId）</li>
 *   <li>{@code plannedStartT} ← {@code ErpApsOperationOrder.plannedStartDateT}</li>
 *   <li>{@code plannedEndT} ← {@code ErpApsOperationOrder.plannedEndDateT}</li>
 *   <li>{@code setupTime} ← {@code ErpApsOperationOrder.setupTime}（分钟，APS 排程已计入 setup）</li>
 * </ul>
 */
@DataBean
public class ApsLoadSlot {

    private Long workOrderId;
    private Integer sequence;
    private Long workcenterId;
    private LocalDateTime plannedStartT;
    private LocalDateTime plannedEndT;
    private BigDecimal setupTime;

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getWorkcenterId() {
        return workcenterId;
    }

    public void setWorkcenterId(Long workcenterId) {
        this.workcenterId = workcenterId;
    }

    public LocalDateTime getPlannedStartT() {
        return plannedStartT;
    }

    public void setPlannedStartT(LocalDateTime plannedStartT) {
        this.plannedStartT = plannedStartT;
    }

    public LocalDateTime getPlannedEndT() {
        return plannedEndT;
    }

    public void setPlannedEndT(LocalDateTime plannedEndT) {
        this.plannedEndT = plannedEndT;
    }

    public BigDecimal getSetupTime() {
        return setupTime;
    }

    public void setSetupTime(BigDecimal setupTime) {
        this.setupTime = setupTime;
    }
}
