package app.erp.aps.service.scheduling;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.mfg.biz.IErpMfgCapacityProvider;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * TOC 瓶颈识别器（E3.4 试点，`constraint-based-planning.md` §2 / AP-3）。
 *
 * <p>负荷侧：horizon 内待排工序（DRAFT/UNSCHEDULABLE）按 {@code computeDuration} 估算工时 + 既有
 * PLANNED 工序已占时段，按 machineId 聚合。产能侧：经 {@link IErpMfgCapacityProvider} SPI 复用
 * {@code CrpLoadCalculator} 产能派生链（日历出勤 × 效率，mfg-service 实现）；SPI 未收集（模块缺失/
 * 单模块测试）时兜底 24h/日。{@code loadRate = loadHours / capacityHours}（capacity≤0 且有负荷 → 9999，
 * 语义同 CrpLoadCalculator.computeLoadRate）。
 */
public class ApsBottleneckDetector {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final BigDecimal RATE_NO_CAPACITY = new BigDecimal("9999");
    private static final int SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    /** mfg 侧产能派生链 SPI（ioc:collect-beans 跨模块收集；空 list 时兜底 24h/日）。 */
    List<IErpMfgCapacityProvider> capacityProviders = Collections.emptyList();

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setCapacityProviders(List<IErpMfgCapacityProvider> capacityProviders) {
        this.capacityProviders = capacityProviders == null ? Collections.emptyList() : capacityProviders;
    }

    /**
     * horizon 内各工作中心负荷率（含待排 + 已排占用）。
     *
     * @param pendingOrders 待排工序（DRAFT/UNSCHEDULABLE，duration 由引擎公式估算）
     * @param horizonStart  可空（空 = 不裁剪已排占用窗口，产能兜底退化为按天计）
     * @param horizonEnd    可空同上
     */
    public Map<String, BigDecimal> detectLoadRates(List<ErpApsOperationOrder> pendingOrders,
                                                   LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        ErpApsSchedulingEngine engine = new ErpApsSchedulingEngine(0, horizonStart, horizonEnd);
        Map<String, BigDecimal> loadMinutes = new LinkedHashMap<>();
        Set<String> machines = new LinkedHashSet<>();
        for (ErpApsOperationOrder op : pendingOrders) {
            if (op.getMachineId() == null) {
                continue;
            }
            machines.add(op.getMachineId());
            loadMinutes.merge(op.getMachineId(), BigDecimal.valueOf(engine.computeDuration(op)), BigDecimal::add);
        }
        for (ErpApsOperationOrder op : findPlannedInHorizon(horizonStart, horizonEnd)) {
            if (op.getMachineId() == null || op.getPlannedStartDateT() == null || op.getPlannedEndDateT() == null) {
                continue;
            }
            machines.add(op.getMachineId());
            long mins = Duration.between(op.getPlannedStartDateT().toLocalDateTime(),
                    op.getPlannedEndDateT().toLocalDateTime()).toMinutes();
            if (mins > 0) {
                loadMinutes.merge(op.getMachineId(), BigDecimal.valueOf(mins), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> capacityHours = capacityHours(machines, horizonStart, horizonEnd);
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (String machineId : machines) {
            BigDecimal loadHours = loadMinutes.getOrDefault(machineId, BigDecimal.ZERO)
                    .divide(SIXTY, SCALE, RoundingMode.HALF_UP);
            rates.put(machineId, computeLoadRate(loadHours, capacityHours.get(machineId)));
        }
        return rates;
    }

    private Map<String, BigDecimal> capacityHours(Set<String> machines, LocalDateTime horizonStart,
                                                  LocalDateTime horizonEnd) {
        LocalDate from = horizonStart == null ? CoreMetrics.today() : horizonStart.toLocalDate();
        LocalDate to = horizonEnd == null ? from : horizonEnd.toLocalDate();
        if (to.isBefore(from)) {
            to = from;
        }
        if (capacityProviders != null && !capacityProviders.isEmpty()) {
            for (IErpMfgCapacityProvider provider : capacityProviders) {
                Map<String, BigDecimal> hours = provider.getCapacityHours(from, to, List.copyOf(machines));
                if (hours != null && !hours.isEmpty()) {
                    return hours;
                }
            }
        }
        // 兜底：SPI 未收集（mfg-service 不在 classpath，如单模块测试）→ horizon 时长对应的连续产能
        Map<String, BigDecimal> fallback = new LinkedHashMap<>();
        BigDecimal fallbackHours = null;
        if (horizonStart != null && horizonEnd != null && horizonEnd.isAfter(horizonStart)) {
            fallbackHours = new BigDecimal(Duration.between(horizonStart, horizonEnd).toMinutes())
                    .divide(SIXTY, SCALE, RoundingMode.HALF_UP);
        }
        for (String machineId : machines) {
            fallback.put(machineId, fallbackHours == null ? BigDecimal.ZERO : fallbackHours);
        }
        return fallback;
    }

    private List<ErpApsOperationOrder> findPlannedInHorizon(LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        if (horizonStart == null || horizonEnd == null) {
            return Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("status", List.of("PLANNED", "IN_PROGRESS")));
        q.addFilter(ge("plannedEndDateT", horizonStart));
        // 同域实体只读聚合（machineId 聚合统计），IDaoProvider 直访对齐 ApsLoadSourceProvider 范式
        return daoProvider.daoFor(ErpApsOperationOrder.class).findAllByQuery(q);
    }

    private BigDecimal computeLoadRate(BigDecimal loadHours, BigDecimal capacityHours) {
        if (capacityHours == null || capacityHours.signum() <= 0) {
            return loadHours != null && loadHours.signum() > 0 ? RATE_NO_CAPACITY : BigDecimal.ZERO;
        }
        return loadHours.divide(capacityHours, SCALE, RoundingMode.HALF_UP);
    }
}
