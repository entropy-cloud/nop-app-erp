package app.erp.mnt.service.dashboard;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维护看板聚合（{@code ErpMntDashboard__*}）集成测试。覆盖：设备总数/运行中/待处理请求/本期访问计数、
 * 状态分布、设备停机预警触发/不触发、维护逾期预警触发/不触发、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpMntDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0L, kpi.get("equipmentTotal"));
        assertEquals(0L, kpi.get("runningCount"));
        assertEquals(0L, kpi.get("openRequestCount"));
        assertEquals(0L, kpi.get("periodVisitCount"));
    }

    @Test
    public void testKpiCounts() {
        LocalDate today = CoreMetrics.currentDate();
        ormTemplate.runInSession(() -> {
            // 3 设备：RUNNING + DOWN + DECOMMISSIONED（不计入总数）
            seedEquipment(101L, "RUN-1", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedEquipment(102L, "DOWN-1", ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            seedEquipment(103L, "DECOM-1", ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED);
            // 2 维护请求：1 OPEN + 1 COMPLETED
            seedRequest(201L, 101L, ErpMntDaoConstants.REQUEST_STATUS_OPEN);
            seedRequest(202L, 102L, ErpMntDaoConstants.REQUEST_STATUS_COMPLETED);
            // 2 访问（COMPLETED，本期）：1 个本期 + 1 个非本期（不计入）
            seedVisit(301L, 101L, today, ErpMntDaoConstants.VISIT_STATUS_COMPLETED);
            seedVisit(302L, 102L, today.minusMonths(2), ErpMntDaoConstants.VISIT_STATUS_COMPLETED);
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        // 设备总数 2（不含 DECOMMISSIONED）
        assertEquals(2L, kpi.get("equipmentTotal"));
        // 运行中 1
        assertEquals(1L, kpi.get("runningCount"));
        // OPEN 请求 1
        assertEquals(1L, kpi.get("openRequestCount"));
        // 本期访问 1（302 非本期）
        assertEquals(1L, kpi.get("periodVisitCount"));
    }

    @Test
    public void testEquipmentStatusDistribution() {
        ormTemplate.runInSession(() -> {
            seedEquipment(111L, "R1", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedEquipment(112L, "R2", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedEquipment(113L, "I1", ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE);
        });
        List<Map<String, Object>> dist = dashboardBiz.getEquipmentStatusDistribution(CTX);
        assertEquals(2, dist.size());
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, dist.get(0).get("status"));
        assertEquals(2L, dist.get(0).get("count"));
    }

    @Test
    public void testEquipmentDowntimeAlertTriggersAndNot() {
        ormTemplate.runInSession(() -> {
            // DOWN 设备 + 未恢复停机 → 触发
            seedEquipment(121L, "DOWN-A", ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            seedDowntimeEntry(201L, 121L, null);
            // DOWN 设备 + 已恢复停机 → 不触发
            seedEquipment(122L, "DOWN-B", ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            seedDowntimeEntry(202L, 122L, CoreMetrics.currentDateTime().minusHours(1));
            // RUNNING 设备 → 不触发（非 DOWN）
            seedEquipment(123L, "RUN-C", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedDowntimeEntry(203L, 123L, null);
        });
        List<Map<String, Object>> alerts = dashboardBiz.findEquipmentDowntimeAlert(CTX);
        assertEquals(1, alerts.size(), "仅设备 121 触发");
        assertEquals(121L, alerts.get(0).get("equipmentId"));
    }

    @Test
    public void testMaintenanceOverdueAlertTriggersAndNot() {
        LocalDate past = CoreMetrics.currentDate().minusDays(10);
        LocalDate future = CoreMetrics.currentDate().plusDays(10);
        ormTemplate.runInSession(() -> {
            // 计划 A: nextDueDate 过去, active, 无 Visit → 触发
            seedSchedule(301L, "SCH-A", 401L, past, 1);
            // 计划 B: nextDueDate 过去, active, 有 Visit → 不触发
            seedSchedule(302L, "SCH-B", 402L, past, 1);
            seedVisitForSchedule(311L, 402L, 302L, CoreMetrics.currentDate(), ErpMntDaoConstants.VISIT_STATUS_COMPLETED);
            // 计划 C: nextDueDate 未来 → 不触发（未到期）
            seedSchedule(303L, "SCH-C", 403L, future, 1);
            // 计划 D: nextDueDate 过去, inactive → 不触发
            seedSchedule(304L, "SCH-D", 404L, past, 0);
        });
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_DASH_MNT_MAINTENANCE_OVERDUE_DAYS,
                String.valueOf(ErpMntConstants.DEFAULT_DASH_MNT_MAINTENANCE_OVERDUE_DAYS));
        List<Map<String, Object>> alerts = dashboardBiz.findMaintenanceOverdueAlert(CTX);
        assertEquals(1, alerts.size(), "仅计划 A 触发");
        assertEquals(301L, alerts.get(0).get("scheduleId"));
        assertEquals(10L, alerts.get(0).get("overdueDays"));
    }

    // ---------- helpers ----------

    private void seedEquipment(long id, String code, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode(code);
        e.setName("设备-" + code);
        e.setStatus(status);
        dao.saveEntity(e);
    }

    private void seedRequest(long id, long equipmentId, String status) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        ErpMntRequest r = dao.newEntity();
        r.orm_propValue(1, id);
        r.setCode("REQ-" + id);
        r.setEquipmentId(equipmentId);
        r.setRequestDate(CoreMetrics.currentDate());
        r.setDescription("维护请求-" + id);
        r.setPriority(ErpMntDaoConstants.PRIORITY_NORMAL);
        r.setStatus(status);
        r.setRequestedBy(1L);
        dao.saveEntity(r);
    }

    private void seedVisit(long id, long equipmentId, LocalDate visitDate, String status) {
        seedVisitForSchedule(id, equipmentId, null, visitDate, status);
    }

    private void seedVisitForSchedule(long id, long equipmentId, Long scheduleId,
                                      LocalDate visitDate, String status) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit v = dao.newEntity();
        v.orm_propValue(1, id);
        v.setCode("VIS-" + id);
        v.setEquipmentId(equipmentId);
        if (scheduleId != null) v.setScheduleId(scheduleId);
        v.setVisitDate(visitDate);
        v.setBusinessDate(visitDate);
        v.setStatus(status);
        dao.saveEntity(v);
    }

    private void seedDowntimeEntry(long id, long equipmentId, LocalDateTime endTime) {
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        ErpMntDowntimeEntry d = dao.newEntity();
        d.orm_propValue(1, id);
        d.setEquipmentId(equipmentId);
        d.setStartTime(Timestamp.valueOf(CoreMetrics.currentDateTime().minusHours(2)));
        d.setEndTime(endTime != null ? Timestamp.valueOf(endTime) : null);
        dao.saveEntity(d);
    }

    private void seedSchedule(long id, String code, long equipmentId, LocalDate nextDueDate, int isActive) {
        IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
        ErpMntSchedule s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName("计划-" + code);
        s.setEquipmentId(equipmentId);
        s.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        s.setStartDate(LocalDate.of(2026, 1, 1));
        s.setNextDueDate(nextDueDate);
        s.setIsActive(isActive);
        dao.saveEntity(s);
    }
}
