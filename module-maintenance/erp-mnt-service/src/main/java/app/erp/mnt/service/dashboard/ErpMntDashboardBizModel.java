package app.erp.mnt.service.dashboard;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

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
 * <p>OEE 指标 Non-Goal（精确性能/质量分量需设备采集数据，见 plan 2026-07-06-1606-1 Non-Goals）。
 */
@BizModel("ErpMntDashboard")
public class ErpMntDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

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
