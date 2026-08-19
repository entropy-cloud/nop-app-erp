package app.erp.mnt.service.dashboard;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.support.OeeCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 维护看板聚合入口（{@code dashboards.md §8}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：设备总数取自 {@link ErpMntEquipment}（status != DECOMMISSIONED count）；
 * 运行中设备 count(RUNNING)；待处理维护请求取自 {@link ErpMntRequest}（count OPEN）；
 * 本期维护访问数取自 {@link ErpMntVisit}（count, 期内 COMPLETED）。
 *
 * <p>OEE（RC-R1.78 / UC-MAIN-10，plan 2026-08-20-0518-1）：{@link #computeOee} 单设备三分量明细 +
 * {@link #computeOeeList} 按设备聚合 + {@link #getDashboardOeeKpi} 看板卡片聚合，
 * 计算载体 {@link app.erp.mnt.service.support.OeeCalculator}（按需 @BizQuery 查询时聚合，
 * 无聚合实体；公式与数据源映射见 {@code equipment-integration.md §六}）。
 */
@BizModel("ErpMntDashboard")
public class ErpMntDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    OeeCalculator oeeCalculator;

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            long equipmentTotal = countEquipmentNotDecommissioned();
            long runningCount = countEquipmentByStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            long openRequestCount = countRequestsByStatus(ErpMntDaoConstants.REQUEST_STATUS_OPEN);
            long periodVisitCount = countCompletedVisitsInRange(from, to);

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("equipmentTotal", equipmentTotal);
            kpi.put("runningCount", runningCount);
            kpi.put("openRequestCount", openRequestCount);
            kpi.put("periodVisitCount", periodVisitCount);
            return kpi;
        });
    }

    /**
     * 单设备 OEE 明细（RC-R1.78 / UC-MAIN-10 A-E）：三分量（可用率/性能效率/质量合格率）+ OEE 乘积
     * + 各分母分子明细；时间窗参数化（日报/月报窗口经同一入口）。空窗口/零分母分量置 null 不抛错（D4）。
     */
    @BizQuery
    public Map<String, Object> computeOee(@Name("equipmentId") Long equipmentId,
                                           @Optional @Name("dateFrom") LocalDate dateFrom,
                                           @Optional @Name("dateTo") LocalDate dateTo,
                                           IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = dateFrom != null ? dateFrom : today.withDayOfMonth(1);
            LocalDate to = dateTo != null ? dateTo : today;
            Map<String, Object> row = oeeCalculator.computeOee(equipmentId, from, to);
            if (row == null) {
                throw new NopException(ErpMntErrors.ERR_EQUIPMENT_NOT_FOUND)
                        .param(ErpMntErrors.ARG_EQUIPMENT_ID, equipmentId);
            }
            return row;
        });
    }

    /** 列表级 OEE 查询（按设备聚合，status != DECOMMISSIONED；窗口默认本月至今）。 */
    @BizQuery
    public List<Map<String, Object>> computeOeeList(@Optional @Name("dateFrom") LocalDate dateFrom,
                                                     @Optional @Name("dateTo") LocalDate dateTo,
                                                     IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = dateFrom != null ? dateFrom : today.withDayOfMonth(1);
            LocalDate to = dateTo != null ? dateTo : today;
            List<ErpMntEquipment> equipments = loadEquipmentsNotDecommissioned();
            List<Map<String, Object>> rows = new ArrayList<>(equipments.size());
            for (ErpMntEquipment equipment : equipments) {
                rows.add(oeeCalculator.computeOee(equipment, from, to));
            }
            return rows;
        });
    }

    /**
     * 看板 OEE 卡片聚合（UC-MAIN-11-B）：fleet 级三分量均值 + OEE 均值（仅统计可计算设备，
     * 无数据 ≠ 零效率，D4）+ 展示字符串（无数据显示 "—"）。
     */
    @BizQuery
    public Map<String, Object> getDashboardOeeKpi(@Optional @Name("startDate") LocalDate startDate,
                                                   @Optional @Name("endDate") LocalDate endDate,
                                                   IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;
            long equipmentTotal = countEquipmentNotDecommissioned();

            BigDecimal availabilitySum = BigDecimal.ZERO, performanceSum = BigDecimal.ZERO,
                    qualitySum = BigDecimal.ZERO, oeeSum = BigDecimal.ZERO;
            long availabilityCount = 0, performanceCount = 0, qualityCount = 0, computedCount = 0;
            for (ErpMntEquipment equipment : loadEquipmentsNotDecommissioned()) {
                Map<String, Object> row = oeeCalculator.computeOee(equipment, from, to);
                if (row.get("availability") != null) {
                    availabilitySum = availabilitySum.add((BigDecimal) row.get("availability"));
                    availabilityCount++;
                }
                if (row.get("performance") != null) {
                    performanceSum = performanceSum.add((BigDecimal) row.get("performance"));
                    performanceCount++;
                }
                if (row.get("quality") != null) {
                    qualitySum = qualitySum.add((BigDecimal) row.get("quality"));
                    qualityCount++;
                }
                if (row.get("oee") != null) {
                    oeeSum = oeeSum.add((BigDecimal) row.get("oee"));
                    computedCount++;
                }
            }

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("equipmentTotal", equipmentTotal);
            kpi.put("computedCount", computedCount);
            BigDecimal availabilityAvg = avg(availabilitySum, availabilityCount);
            BigDecimal performanceAvg = avg(performanceSum, performanceCount);
            BigDecimal qualityAvg = avg(qualitySum, qualityCount);
            BigDecimal oeeAvg = avg(oeeSum, computedCount);
            kpi.put("availabilityAvg", availabilityAvg);
            kpi.put("performanceAvg", performanceAvg);
            kpi.put("qualityAvg", qualityAvg);
            kpi.put("oeeAvg", oeeAvg);
            kpi.put("availabilityDisplay", percentDisplay(availabilityAvg));
            kpi.put("performanceDisplay", percentDisplay(performanceAvg));
            kpi.put("qualityDisplay", percentDisplay(qualityAvg));
            kpi.put("oeeDisplay", percentDisplay(oeeAvg));
            return kpi;
        });
    }

    /** 设备状态分布（按 status 聚合）。 */
    @BizQuery
    public List<Map<String, Object>> getEquipmentStatusDistribution(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY status + COUNT，避免全表物化
            QueryBean q = new QueryBean();
            q.setSourceName(ErpMntEquipment.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("status");
            QueryFieldBean cnt = QueryFieldBean.mainField("status").count().alias("cnt");
            q.setFields(java.util.Arrays.asList(dim, cnt));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String s = row.get("status") == null ? "UNKNOWN" : String.valueOf(row.get("status"));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("status", s);
                r.put("count", ((Number) row.get("cnt")).longValue());
                result.add(r);
            }
            result.sort(Comparator.<Map<String, Object>, Long>comparing(
                    r -> (Long) r.get("count"), Comparator.reverseOrder()));
            return result;
        });
    }

    /** 设备停机预警（status=DOWN 且 DowntimeEntry.endTime=null 未恢复）。 */
    @BizQuery
    public List<Map<String, Object>> findEquipmentDowntimeAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpMntEquipment> downEquipments = loadEquipmentsByStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            if (downEquipments.isEmpty()) return Collections.emptyList();
            Set<Long> equipmentIds = new HashSet<>();
            for (ErpMntEquipment e : downEquipments) equipmentIds.add(e.getId());
            Set<Long> equipmentIdsWithOngoingDowntime = loadEquipmentIdsWithOngoingDowntime(equipmentIds);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMntEquipment e : downEquipments) {
                if (equipmentIdsWithOngoingDowntime.contains(e.getId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("equipmentId", e.getId());
                    row.put("equipmentCode", e.getCode());
                    row.put("equipmentName", e.getName());
                    row.put("status", e.getStatus());
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * 维护逾期预警（Schedule.nextDueDate 早于 today-minus-overdueDays 且 isActive=1 且未生成 Visit）。
     * 阈值经 {@code erp-dash.mnt-maintenance-overdue-days} 配置（默认 0=直接 < today 比对）。
     */
    @BizQuery
    public List<Map<String, Object>> findMaintenanceOverdueAlert(IServiceContext context) {
        int overdueDays = AppConfig.var(
                ErpMntConstants.CONFIG_DASH_MNT_MAINTENANCE_OVERDUE_DAYS,
                ErpMntConstants.DEFAULT_DASH_MNT_MAINTENANCE_OVERDUE_DAYS);
        LocalDate today = CoreMetrics.currentDate();
        LocalDate cutoff = today.minusDays(overdueDays);
        return ormTemplate.runInSession(session -> {
            List<ErpMntSchedule> schedules = loadActiveSchedules();
            Set<Long> scheduleIdsWithVisit = loadScheduleIdsWithVisit();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMntSchedule s : schedules) {
                LocalDate due = s.getNextDueDate();
                if (due == null || !due.isBefore(cutoff)) continue;
                if (scheduleIdsWithVisit.contains(s.getId())) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("scheduleId", s.getId());
                row.put("scheduleCode", s.getCode());
                row.put("scheduleName", s.getName());
                row.put("equipmentId", s.getEquipmentId());
                row.put("nextDueDate", due);
                long overdue = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                row.put("overdueDays", overdue);
                rows.add(row);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private long countEquipmentNotDecommissioned() {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(ne("status", ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED));
        return dao.countByQuery(q);
    }

    private List<ErpMntEquipment> loadEquipmentsNotDecommissioned() {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(ne("status", ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED));
        return dao.findAllByQuery(q);
    }

    private BigDecimal avg(BigDecimal sum, long count) {
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

    private String percentDisplay(BigDecimal rate) {
        return rate == null ? "—" : rate.multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private long countEquipmentByStatus(String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", status));
        return dao.countByQuery(q);
    }

    private List<ErpMntEquipment> loadEquipmentsByStatus(String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", status));
        return dao.findAllByQuery(q);
    }

    private long countRequestsByStatus(String status) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", status));
        return dao.countByQuery(q);
    }

    private long countCompletedVisitsInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpMntDaoConstants.VISIT_STATUS_COMPLETED));
        q.addFilter(ge("businessDate", from));
        q.addFilter(le("businessDate", to));
        return dao.countByQuery(q);
    }

    private Set<Long> loadEquipmentIdsWithOngoingDowntime(Set<Long> equipmentIds) {
        if (equipmentIds.isEmpty()) return Collections.emptySet();
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("equipmentId", equipmentIds));
        q.addFilter(eq("endTime", null));
        Set<Long> ids = new HashSet<>();
        for (ErpMntDowntimeEntry d : dao.findAllByQuery(q)) {
            if (d.getEquipmentId() != null) ids.add(d.getEquipmentId());
        }
        return ids;
    }

    private List<ErpMntSchedule> loadActiveSchedules() {
        IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", 1));
        return dao.findAllByQuery(q);
    }

    /** 收集已生成 Visit 的 scheduleId 集合（类 C：单字段收集，带硬上限的受限扫描）。 */
    private Set<Long> loadScheduleIdsWithVisit() {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.setLimit(5000);
        Set<Long> ids = new HashSet<>();
        for (ErpMntVisit v : dao.findAllByQuery(q)) {
            if (v.getScheduleId() != null) ids.add(v.getScheduleId());
        }
        return ids;
    }
}
