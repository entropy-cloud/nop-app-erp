package app.erp.aps.service.scheduling;

import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsOpRouting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 排产求解请求（E3.4 求解器分离，`constraint-based-planning.md` §1）。
 *
 * <p>承载一次求解的全部输入：待排工序 + 维护约束 + 替代路由 + horizon/缓冲参数，以及 TOC 模式的
 * 瓶颈识别输入（负荷率 + 阈值，由 {@code ApsBottleneckDetector} 派生链预计算）。
 */
public class ApsSchedulingRequest {

    private String mode;
    private List<ErpApsOperationOrder> orders;
    private List<ErpApsConstraint> maintenanceConstraints;
    private List<ErpApsOperationOrder> frozenPlanned;
    private List<ErpApsOpRouting> routings;
    private int bufferMinutes;
    private LocalDateTime horizonStart;
    private LocalDateTime horizonEnd;
    private LocalDateTime defaultEarliestStart;
    private LocalDate routingEffectiveDate;
    private Map<String, BigDecimal> machineLoadRates;
    private double bottleneckThreshold;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<ErpApsOperationOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<ErpApsOperationOrder> orders) {
        this.orders = orders;
    }

    public List<ErpApsConstraint> getMaintenanceConstraints() {
        return maintenanceConstraints;
    }

    public void setMaintenanceConstraints(List<ErpApsConstraint> maintenanceConstraints) {
        this.maintenanceConstraints = maintenanceConstraints;
    }

    public List<ErpApsOperationOrder> getFrozenPlanned() {
        return frozenPlanned;
    }

    public void setFrozenPlanned(List<ErpApsOperationOrder> frozenPlanned) {
        this.frozenPlanned = frozenPlanned;
    }

    public List<ErpApsOpRouting> getRoutings() {
        return routings;
    }

    public void setRoutings(List<ErpApsOpRouting> routings) {
        this.routings = routings;
    }

    public int getBufferMinutes() {
        return bufferMinutes;
    }

    public void setBufferMinutes(int bufferMinutes) {
        this.bufferMinutes = bufferMinutes;
    }

    public LocalDateTime getHorizonStart() {
        return horizonStart;
    }

    public void setHorizonStart(LocalDateTime horizonStart) {
        this.horizonStart = horizonStart;
    }

    public LocalDateTime getHorizonEnd() {
        return horizonEnd;
    }

    public void setHorizonEnd(LocalDateTime horizonEnd) {
        this.horizonEnd = horizonEnd;
    }

    public LocalDateTime getDefaultEarliestStart() {
        return defaultEarliestStart;
    }

    public void setDefaultEarliestStart(LocalDateTime defaultEarliestStart) {
        this.defaultEarliestStart = defaultEarliestStart;
    }

    public LocalDate getRoutingEffectiveDate() {
        return routingEffectiveDate;
    }

    public void setRoutingEffectiveDate(LocalDate routingEffectiveDate) {
        this.routingEffectiveDate = routingEffectiveDate;
    }

    public Map<String, BigDecimal> getMachineLoadRates() {
        return machineLoadRates;
    }

    public void setMachineLoadRates(Map<String, BigDecimal> machineLoadRates) {
        this.machineLoadRates = machineLoadRates;
    }

    public double getBottleneckThreshold() {
        return bottleneckThreshold;
    }

    public void setBottleneckThreshold(double bottleneckThreshold) {
        this.bottleneckThreshold = bottleneckThreshold;
    }
}
