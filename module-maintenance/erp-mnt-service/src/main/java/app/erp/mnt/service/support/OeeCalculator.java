package app.erp.mnt.service.support;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * OEE 按需计算器（RC-R1.78 / UC-MAIN-10，plan 2026-08-20-0518-1）：三分量 + 乘积查询时聚合，
 * 无聚合实体无物化快照（B 类裁决）。公式权威 {@code equipment-integration.md §6.1-6.3}：
 * 可用率 = 实际运行时间/计划运行时间（计划 = 日历班次 − 停机，L1「排除停机」）；
 * 性能效率 = 实际产量/理论产量（理论 = 标准产能 × 实际运行时间）；质量合格率 = 合格品/总产量
 * （主路径 qa Inspection ACCEPTED 批量，回退 mfg 报工 completed/(completed+scrapped)）；
 * OEE = 三者乘积。任一分量分母 0/无数据 → 分量与 OEE 置 null（无数据 ≠ 零效率，D4 裁决不 config 化）。
 *
 * <p>daoFor 直读说明（E3）：跨域 mfg（JobCard/TimeLog/WorkOrder/WorkcenterCalendar/WorkcenterCapacity）
 * 与 qa（Inspection）为只读聚合访问，经 IDaoProvider 直查并登记
 * {@code docs/architecture/data-dependency-matrix.md §2.4}（mnt→mfg / mnt→qa 只读 Java 层边，
 * matrix §9.4 只读豁免先例）；mnt 域内 Equipment/DowntimeEntry 为本域只读聚合。
 */
public class OeeCalculator {

    static final DateTimeFormatter SHIFT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH[:mm[:ss]]");

    // 跨域字典/弱指针镜像值（权威为 mfg/qa 域，调用方副本先例 ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART）
    static final String JOB_CARD_STATUS_CANCELLED = "CANCELLED";
    static final String QA_INSPECTION_RESULT_ACCEPTED = "ACCEPTED";
    static final String RELATED_BILL_TYPE_MFG_WORK_ORDER = "ERP_MFG_WORK_ORDER";
    static final String WORK_DATE_PATTERN_WEEKDAYS = "WEEKDAYS";
    static final String WORK_DATE_PATTERN_WEEKEND = "WEEKEND";
    static final String SHIFT_TYPE_ONE_SHIFT = "ONE_SHIFT";

    static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    static final int RATE_SCALE = 4;
    static final int HOURS_SCALE = 4;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    EquipmentRuntimeCalculator runtimeCalculator;

    /** 按设备计算 OEE 明细（设备不存在返回 null，由调用方裁决错误语义）。 */
    public Map<String, Object> computeOee(Long equipmentId, LocalDate dateFrom, LocalDate dateTo) {
        ErpMntEquipment equipment = runtimeCalculator.loadEquipment(equipmentId);
        if (equipment == null) {
            return null;
        }
        return computeOee(equipment, dateFrom, dateTo);
    }

    public Map<String, Object> computeOee(ErpMntEquipment equipment, LocalDate dateFrom, LocalDate dateTo) {
        Timestamp windowStart = Timestamp.valueOf(dateFrom.atStartOfDay());
        Timestamp windowEndExclusive = Timestamp.valueOf(dateTo.plusDays(1).atStartOfDay());
        Long workcenterId = equipment.getWorkcenterId();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipmentId", equipment.getId());
        result.put("equipmentCode", equipment.getCode());
        result.put("equipmentName", equipment.getName());
        result.put("dateFrom", dateFrom);
        result.put("dateTo", dateTo);
        result.put("workcenterId", workcenterId);

        // ---- 可用率 = 实际运行时间 / 计划运行时间（D1：日历班次 − 停机）----
        BigDecimal runningHours = runtimeCalculator.computeRunningHoursInRange(
                equipment.getId(), windowStart, windowEndExclusive);
        BigDecimal calendarHours = workcenterId != null
                ? computeCalendarHours(workcenterId, dateFrom, dateTo) : null;
        BigDecimal downtimeHours = workcenterId != null
                ? computeDowntimeHours(equipment.getId(), windowStart, windowEndExclusive)
                : null;
        BigDecimal plannedHours = null;
        if (calendarHours != null) {
            plannedHours = downtimeHours == null
                    ? calendarHours : calendarHours.subtract(downtimeHours).max(BigDecimal.ZERO);
        }
        BigDecimal availability = plannedHours != null && plannedHours.signum() > 0
                ? divide(runningHours, plannedHours) : null;
        result.put("runningHours", runningHours);
        result.put("calendarHours", calendarHours);
        result.put("downtimeHours", downtimeHours);
        result.put("plannedHours", plannedHours);
        result.put("availability", availability);

        // ---- 实际产量（D2：Σ TimeLog.completedQuantity，经 JobCard.workcenterId 桥接）----
        OutputAggregate output = workcenterId != null
                ? collectOutput(workcenterId, dateFrom, dateTo) : OutputAggregate.EMPTY;
        BigDecimal actualOutput = output.completed;
        result.put("actualOutput", actualOutput);

        // ---- 性能效率 = 实际产量 / 理论产量（理论 = 标准产能 × 实际运行时间）----
        BigDecimal capacityPerHour = workcenterId != null && runningHours.signum() > 0
                ? resolveCapacityPerHour(workcenterId, output.productIds) : null;
        BigDecimal theoreticalOutput = capacityPerHour != null
                ? capacityPerHour.multiply(runningHours).setScale(HOURS_SCALE, RoundingMode.HALF_UP) : null;
        BigDecimal performance = theoreticalOutput != null && theoreticalOutput.signum() > 0
                ? divide(actualOutput, theoreticalOutput) : null;
        result.put("capacityPerHour", capacityPerHour);
        result.put("theoreticalOutput", theoreticalOutput);
        result.put("performance", performance);

        // ---- 质量合格率 = 合格品 / 总产量（D3：主 qa，回退 mfg 报工数量）----
        List<ErpQaInspection> linkedInspections = output.workOrderCodes.isEmpty()
                ? null : findLinkedInspections(output.workOrderCodes, dateFrom, dateTo);
        BigDecimal quality = null;
        String qualitySource = null;
        BigDecimal qualifiedQuantity = null;
        if (linkedInspections != null) {
            qualitySource = "QA";
            qualifiedQuantity = BigDecimal.ZERO;
            for (ErpQaInspection inspection : linkedInspections) {
                if (QA_INSPECTION_RESULT_ACCEPTED.equals(inspection.getResult())) {
                    qualifiedQuantity = qualifiedQuantity.add(nz(inspection.getLotQuantity()));
                }
            }
            if (actualOutput.signum() > 0) {
                quality = divide(qualifiedQuantity, actualOutput);
            }
        } else {
            BigDecimal fallbackDenominator = actualOutput.add(output.scrapped);
            if (fallbackDenominator.signum() > 0) {
                qualitySource = "MFG_FALLBACK";
                quality = divide(actualOutput, fallbackDenominator);
                qualifiedQuantity = actualOutput;
            }
        }
        result.put("qualifiedQuantity", qualifiedQuantity);
        result.put("totalOutput", actualOutput);
        result.put("qualitySource", qualitySource);
        result.put("quality", quality);

        // ---- OEE = 三分量乘积（任一 null → null，D4）----
        BigDecimal oee = availability != null && performance != null && quality != null
                ? availability.multiply(performance).multiply(quality).setScale(RATE_SCALE, RoundingMode.HALF_UP)
                : null;
        result.put("oee", oee);
        return result;
    }

    // ===================== 可用率分母 ======================

    /** D1：Σ 生效日历班次窗口时长（窗口内命中工作日 × 每日各班次时长；无任何命中返回 null）。 */
    protected BigDecimal computeCalendarHours(Long workcenterId, LocalDate dateFrom, LocalDate dateTo) {
        IEntityDao<ErpMfgWorkcenterCalendar> dao = daoProvider.daoFor(ErpMfgWorkcenterCalendar.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("workcenterId", workcenterId));
        List<ErpMfgWorkcenterCalendar> rows = dao.findAllByQuery(q);
        List<ErpMfgWorkcenterCalendar> active = new ArrayList<>();
        for (ErpMfgWorkcenterCalendar row : rows) {
            if (Boolean.TRUE.equals(row.getIsActive())) {
                active.add(row);
            }
        }
        if (active.isEmpty()) {
            return null;
        }
        long totalMinutes = 0L;
        boolean anyMatchedDay = false;
        for (LocalDate d = dateFrom; !d.isAfter(dateTo); d = d.plusDays(1)) {
            for (ErpMfgWorkcenterCalendar row : active) {
                if (!effectiveCovers(row, d) || !patternMatches(row.getWorkDatePattern(), d)) {
                    continue;
                }
                anyMatchedDay = true;
                totalMinutes += shiftMinutes(row);
            }
        }
        if (!anyMatchedDay) {
            return null;
        }
        return BigDecimal.valueOf(totalMinutes).divide(MINUTES_PER_HOUR, HOURS_SCALE, RoundingMode.HALF_UP);
    }

    protected boolean effectiveCovers(ErpMfgWorkcenterCalendar row, LocalDate d) {
        return (row.getEffectiveFrom() == null || !d.isBefore(row.getEffectiveFrom()))
                && (row.getEffectiveTo() == null || !d.isAfter(row.getEffectiveTo()));
    }

    /** null/ALL_WEEK=每日；WEEKDAYS=周一至五；WEEKEND=周六日。 */
    protected boolean patternMatches(String workDatePattern, LocalDate d) {
        if (WORK_DATE_PATTERN_WEEKDAYS.equals(workDatePattern)) {
            DayOfWeek dow = d.getDayOfWeek();
            return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }
        if (WORK_DATE_PATTERN_WEEKEND.equals(workDatePattern)) {
            DayOfWeek dow = d.getDayOfWeek();
            return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        }
        return true;
    }

    /** 班次时长（分钟）：startTime/endTime 可解析 → 差值（跨午夜 +24h）；否则按 shiftType 缺省（ONE_SHIFT=24h，其余 8h）。 */
    protected long shiftMinutes(ErpMfgWorkcenterCalendar row) {
        LocalTime start = parseShiftTime(row.getStartTime());
        LocalTime end = parseShiftTime(row.getEndTime());
        if (start != null && end != null && !start.equals(end)) {
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes < 0) {
                minutes += 24L * 60L;
            }
            return minutes;
        }
        return SHIFT_TYPE_ONE_SHIFT.equals(row.getShiftType()) ? 24L * 60L : 8L * 60L;
    }

    protected LocalTime parseShiftTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(value, SHIFT_TIME_FORMAT);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** D1：Σ 停机记录与窗口相交分钟数（开放段计至窗口末端）。 */
    protected BigDecimal computeDowntimeHours(Long equipmentId, Timestamp windowStart, Timestamp windowEndExclusive) {
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("equipmentId", equipmentId));
        List<ErpMntDowntimeEntry> entries = dao.findAllByQuery(q);
        long seconds = 0L;
        for (ErpMntDowntimeEntry entry : entries) {
            Timestamp start = entry.getStartTime();
            if (start == null) {
                continue;
            }
            Timestamp end = entry.getEndTime() != null ? entry.getEndTime() : windowEndExclusive;
            seconds += EquipmentRuntimeCalculator.overlapSeconds(start, end, windowStart, windowEndExclusive);
        }
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600L), HOURS_SCALE, RoundingMode.HALF_UP);
    }

    // ===================== 产量 / 产能 / 质量 ======================

    /** D2：窗口内该工作中心的报工聚合（经 JobCard.workcenterId 桥接 TimeLog；CANCELLED 卡排除）。 */
    protected OutputAggregate collectOutput(Long workcenterId, LocalDate dateFrom, LocalDate dateTo) {
        IEntityDao<ErpMfgJobCard> cardDao = daoProvider.daoFor(ErpMfgJobCard.class);
        QueryBean cardQuery = new QueryBean();
        cardQuery.addFilter(eq("workcenterId", workcenterId));
        Set<Long> cardIds = new LinkedHashSet<>();
        for (ErpMfgJobCard card : cardDao.findAllByQuery(cardQuery)) {
            if (!JOB_CARD_STATUS_CANCELLED.equals(card.getStatus())) {
                cardIds.add(card.getId());
            }
        }
        if (cardIds.isEmpty()) {
            return OutputAggregate.EMPTY;
        }
        IEntityDao<ErpMfgJobCardTimeLog> logDao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
        QueryBean logQuery = new QueryBean();
        logQuery.addFilter(in("jobCardId", cardIds));
        logQuery.addFilter(ge("workDate", dateFrom));
        logQuery.addFilter(le("workDate", dateTo));
        List<ErpMfgJobCardTimeLog> logs = logDao.findAllByQuery(logQuery);

        OutputAggregate aggregate = new OutputAggregate();
        Set<Long> workOrderIds = new LinkedHashSet<>();
        for (ErpMfgJobCardTimeLog log : logs) {
            aggregate.completed = aggregate.completed.add(nz(log.getCompletedQuantity()));
            aggregate.scrapped = aggregate.scrapped.add(nz(log.getScrappedQuantity()));
            if (log.getWorkOrderId() != null) {
                workOrderIds.add(log.getWorkOrderId());
            }
        }
        if (!workOrderIds.isEmpty()) {
            IEntityDao<ErpMfgWorkOrder> woDao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            QueryBean woQuery = new QueryBean();
            woQuery.addFilter(in("id", workOrderIds));
            for (ErpMfgWorkOrder workOrder : woDao.findAllByQuery(woQuery)) {
                aggregate.workOrderCodes.add(workOrder.getCode());
                if (workOrder.getProductId() != null) {
                    aggregate.productIds.add(workOrder.getProductId());
                }
            }
        }
        return aggregate;
    }

    /** D2：产能行选择——窗口报工产品唯一 → 该产品 active 行；否则唯一 active 行；否则 null。 */
    protected BigDecimal resolveCapacityPerHour(Long workcenterId, Set<Long> productIds) {
        IEntityDao<ErpMfgWorkcenterCapacity> dao = daoProvider.daoFor(ErpMfgWorkcenterCapacity.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("workcenterId", workcenterId));
        List<ErpMfgWorkcenterCapacity> active = new ArrayList<>();
        for (ErpMfgWorkcenterCapacity row : dao.findAllByQuery(q)) {
            if (Boolean.TRUE.equals(row.getIsActive())) {
                active.add(row);
            }
        }
        if (active.isEmpty()) {
            return null;
        }
        if (active.size() == 1) {
            return active.get(0).getCapacityPerHour();
        }
        if (productIds.size() == 1) {
            Long productId = productIds.iterator().next();
            for (ErpMfgWorkcenterCapacity row : active) {
                if (productId.equals(row.getMaterialId())) {
                    return row.getCapacityPerHour();
                }
            }
        }
        return null;
    }

    /** D3：qa 主路径——关联该工作中心窗口内报工工单的质检单（inspectionDate ∈ 窗口）；无关联行返回 null。 */
    protected List<ErpQaInspection> findLinkedInspections(Set<String> workOrderCodes,
                                                          LocalDate dateFrom, LocalDate dateTo) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", RELATED_BILL_TYPE_MFG_WORK_ORDER));
        q.addFilter(in("relatedBillCode", workOrderCodes));
        q.addFilter(ge("inspectionDate", dateFrom));
        q.addFilter(le("inspectionDate", dateTo));
        List<ErpQaInspection> inspections = dao.findAllByQuery(q);
        return inspections.isEmpty() ? null : inspections;
    }

    // ===================== helpers ======================

    protected BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, RATE_SCALE, RoundingMode.HALF_UP);
    }

    protected BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    protected static class OutputAggregate {
        static final OutputAggregate EMPTY = new OutputAggregate();
        BigDecimal completed = BigDecimal.ZERO;
        BigDecimal scrapped = BigDecimal.ZERO;
        final Set<String> workOrderCodes = new LinkedHashSet<>();
        final Set<Long> productIds = new LinkedHashSet<>();
    }
}
