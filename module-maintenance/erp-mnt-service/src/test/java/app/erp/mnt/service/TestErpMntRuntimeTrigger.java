package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.support.EquipmentRuntimeCalculator;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import app.erp.mnt.service.support.EquipmentStatusLogWriter;
import app.erp.mnt.service.support.ScheduleDueGenerator;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-MAIN-02 运行时长触发链测试组（RC-R1.73 / plan 2026-08-19-0445-2 Phase 1）。
 *
 * <p>覆盖：①Σ RUNNING 段聚合数学（多周期 + 开放段 + 遗留基线双分支）②累计 ≥ 阈值触发 DRAFT + baseline 重置
 * ③未达阈值不生成 ④同日重跑幂等（baseline 已推进）⑤TIME 计划零回归（null triggerType 既有链）
 * ⑥非 RUNNING 段不计入 ⑦手动 changeStatus 写 MANUAL 日志行 + 状态日志 `_cases/` 快照。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntRuntimeTrigger extends JunitAutoTestCase {

    @RegisterExtension
    static MntFrozenClockExtension frozenClock = new MntFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 17);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ScheduleDueGenerator scheduleDueGenerator;
    @Inject
    EquipmentRuntimeCalculator runtimeCalculator;
    @Inject
    EquipmentStatusLinker equipmentStatusLinker;
    @Inject
    EquipmentStatusLogWriter statusLogWriter;

    // ---------- Proof ① + ⑥：Σ RUNNING 段聚合数学 ----------

    @Test
    public void testRunningSegmentAggregationMath() {
        Long equipmentId = 61001L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            // 多状态变更周期：RUNNING(4h) → DOWN(1h 不计) → RUNNING(2h) → IDLE(1h 不计) → RUNNING 开放段至 asOf(26h)
            seedStatusLog(61011L, equipmentId, null,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 8, 0));
            seedStatusLog(61012L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, ts(2026, 7, 15, 12, 0));
            seedStatusLog(61013L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 13, 0));
            seedStatusLog(61014L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 15, 15, 0));
            seedStatusLog(61015L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 16, 0));
            return null;
        });

        BigDecimal hours = runtimeCalculator.computeRunningHours(equipmentId, ts(2026, 7, 16, 18, 0));
        // 4h + 2h + 26h = 32h；DOWN(12→13) 与 IDLE(15→16) 段不计入（Proof ⑥）
        assertEquals(0, hours.compareTo(new BigDecimal("32")), "Σ RUNNING 段 = 4+2+26 = 32h（非 RUNNING 段不计入）");
    }

    @Test
    public void testNonRunningOnlySegmentsCountZero() {
        Long equipmentId = 61002L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE);
            seedStatusLog(61021L, equipmentId, null,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 15, 8, 0));
            seedStatusLog(61022L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, ts(2026, 7, 15, 12, 0));
            seedStatusLog(61023L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, ts(2026, 7, 15, 18, 0));
            return null;
        });

        BigDecimal hours = runtimeCalculator.computeRunningHours(equipmentId, ts(2026, 7, 16, 18, 0));
        assertEquals(0, hours.compareTo(BigDecimal.ZERO), "仅 IDLE/DOWN/UNDER_MAINTENANCE 段累计为 0");
    }

    // ---------- Proof ① 遗留基线双分支 ----------

    @Test
    public void testLegacyBaselineRunningSinceCreateTime() {
        Long equipmentId = 61003L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            return null;
        });
        // 独立 session 回拨 createTime（insert 落库后 update，update 路径不触碰 createTime）
        ormTemplate.runInSession(s -> {
            backdateCreateTime(equipmentId, ts(2026, 7, 10, 0, 0));
            return null;
        });

        // 无日志 + 当前 RUNNING → createTime 起算：2026-07-10 00:00 → 2026-07-17 00:00 = 168h
        BigDecimal hours = runtimeCalculator.computeRunningHours(equipmentId, ts(2026, 7, 17, 0, 0));
        assertEquals(0, hours.compareTo(new BigDecimal("168")),
                "遗留无日志 + 当前 RUNNING：createTime 起算 168h");
    }

    @Test
    public void testLegacyBaselineNonRunningZeroUntilFirstLog() {
        Long equipmentId = 61004L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE);
            return null;
        });

        // 无日志 + 当前非 RUNNING → 0（保守 fail-safe，防虚计触发）
        BigDecimal before = runtimeCalculator.computeRunningHours(equipmentId, ts(2026, 7, 17, 0, 0));
        assertEquals(0, before.compareTo(BigDecimal.ZERO), "遗留无日志 + 当前 IDLE：运行时长记 0");

        // 首条日志行后从日志起算（经 writer 追加 RUNNING 迁移，changeAt=冻结 now=2026-07-17 00:00）
        ormTemplate.runInSession(s -> {
            statusLogWriter.append(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL, null);
            return null;
        });
        BigDecimal after = runtimeCalculator.computeRunningHours(equipmentId, ts(2026, 7, 18, 0, 0));
        assertEquals(0, after.compareTo(new BigDecimal("24")), "首条日志行后从日志 changeAt 起算 24h");
    }

    // ---------- Proof ② ③ ④：RUNTIME 计划触发链 ----------

    @Test
    public void testRuntimeThresholdTriggersVisitAndResetsBaseline() {
        Long equipmentId = 61005L;
        Long scheduleId = 62005L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            // RUNNING 自 2026-07-15 00:00 起持续 → asOf 2026-07-17 00:00 累计 48h
            seedStatusLog(61051L, equipmentId, null,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 0, 0));
            seedRuntimeSchedule(scheduleId, equipmentId, "40", null);
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "累计 48h ≥ baseline 0 + 阈值 40 → 生成 1 条 DRAFT 访问");

        ErpMntVisit visit = findVisitByCode("VST-SCH-" + scheduleId + "-" + AS_OF_DATE);
        assertNotNull(visit, "生成 VST-SCH 编码访问");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus());
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_PLANNED, visit.getVisitType());
        assertEquals(AS_OF_DATE, visit.getVisitDate(), "RUNTIME 触发 visitDate=asOfDate");

        ErpMntSchedule managed = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(scheduleId);
        assertEquals(0, managed.getRuntimeBaselineHours().compareTo(new BigDecimal("48")),
                "生成后 baseline 重置为当前累计 48h");
        assertNull(managed.getNextDueDate(), "RUNTIME 计划 nextDueDate 不推进（保持 null）");
    }

    @Test
    public void testRuntimeBelowThresholdNoVisit() {
        Long equipmentId = 61006L;
        Long scheduleId = 62006L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedStatusLog(61061L, equipmentId, null,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 0, 0));
            seedRuntimeSchedule(scheduleId, equipmentId, "60", null);
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(0, count, "累计 48h < 阈值 60 → 不生成");

        assertNull(findVisitByCode("VST-SCH-" + scheduleId + "-" + AS_OF_DATE), "无访问行");
        ErpMntSchedule managed = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(scheduleId);
        assertNull(managed.getRuntimeBaselineHours(), "未触发 baseline 不变（null）");
    }

    @Test
    public void testRuntimeRerunSameDayIdempotent() {
        Long equipmentId = 61007L;
        Long scheduleId = 62007L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedStatusLog(61071L, equipmentId, null,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 0, 0));
            seedRuntimeSchedule(scheduleId, equipmentId, "40", null);
            return null;
        });

        int first = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, first, "首次触发生成 1 条");

        // 同日重跑：baseline 已推进至 48h，48 < 48+40 → 不重复生成
        int second = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(0, second, "同日重跑幂等（baseline 已推进）");

        QueryBean q = new QueryBean();
        q.addFilter(eq("code", "VST-SCH-" + scheduleId + "-" + AS_OF_DATE));
        assertEquals(1L, daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q).size(), "仅 1 条访问（无重复）");
    }

    // ---------- Proof ⑤：TIME 计划零回归 ----------

    @Test
    public void testTimeScheduleNullTriggerZeroRegression() {
        Long equipmentId = 61008L;
        Long scheduleId = 62008L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedTimeSchedule(scheduleId, equipmentId, LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "null triggerType 走既有 nextDueDate 链");

        ErpMntVisit visit = findVisitByCode("VST-SCH-" + scheduleId + "-" + AS_OF_DATE);
        assertNotNull(visit, "既有编码约定访问生成");
        assertEquals(LocalDate.of(2026, 7, 15), visit.getVisitDate(), "visitDate=nextDueDate（既有行为）");

        ErpMntSchedule managed = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(scheduleId);
        assertEquals(LocalDate.of(2026, 8, 15), managed.getNextDueDate(), "nextDueDate 按 MONTHLY/1 推进（既有行为）");
        assertNull(managed.getRuntimeBaselineHours(), "TIME 计划不触碰 runtime baseline");
    }

    // ---------- Proof ⑦：手动 changeStatus + linker 写日志行 ----------

    @Test
    public void testManualChangeStatusWritesLog() {
        Long equipmentId = 61009L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            return null;
        });

        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpMntEquipment__changeStatus",
                ApiRequest.build(Map.of("equipmentId", equipmentId, "newStatus",
                        ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN)));
        assertEquals(0, resp.getStatus(), "changeStatus RPC 成功");

        ErpMntEquipmentStatusLog log = findLatestLog(equipmentId);
        assertNotNull(log, "手动变更追加状态日志行");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, log.getFromStatus());
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, log.getToStatus());
        assertEquals(ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL, log.getSource());
    }

    @Test
    public void testLinkerTransitionsWriteLogs() {
        Long equipmentId = 61010L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            return null;
        });

        ormTemplate.runInSession(s -> {
            equipmentStatusLinker.linkToUnderMaintenance(equipmentId, CTX);
            return null;
        });
        ErpMntEquipmentStatusLog visitLog = findLatestLog(equipmentId);
        assertEquals(ErpMntDaoConstants.STATUS_LOG_SOURCE_VISIT, visitLog.getSource(), "visit 路径来源 VISIT");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, visitLog.getToStatus());

        ormTemplate.runInSession(s -> {
            equipmentStatusLinker.restoreToRunning(equipmentId,
                    ErpMntDaoConstants.STATUS_LOG_SOURCE_DOWNTIME, CTX);
            return null;
        });
        ErpMntEquipmentStatusLog restoreLog = findLatestLog(equipmentId);
        assertEquals(ErpMntDaoConstants.STATUS_LOG_SOURCE_DOWNTIME, restoreLog.getSource(),
                "恢复路径来源经 logSource 参数传入");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, restoreLog.getToStatus());

        ormTemplate.runInSession(s -> {
            equipmentStatusLinker.linkToDown(equipmentId, CTX);
            return null;
        });
        ErpMntEquipmentStatusLog downLog = findLatestLog(equipmentId);
        assertEquals(ErpMntDaoConstants.STATUS_LOG_SOURCE_DOWNTIME, downLog.getSource(), "停机路径来源 DOWNTIME");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, downLog.getToStatus());
    }

    @EnableSnapshot
    @Test
    public void testManualChangeStatusSnapshot() {
        Long equipmentId = 61011L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            return null;
        });
        ApiRequest<Map> req = request("request.json5", Map.class);
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpMntEquipment__changeStatus", req);
        output("response.json5", resp);
        assertTrue(resp.getStatus() == 0, "快照路径 changeStatus 成功");
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static Timestamp ts(int year, int month, int day, int hour, int minute) {
        return Timestamp.valueOf(LocalDateTime.of(year, month, day, hour, minute));
    }

    private void seedEquipment(Long id, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = dao.newEntity();
        equipment.setId(id);
        equipment.setCode("EQ-RT-" + id);
        equipment.setName("运行时长测试设备" + id);
        equipment.setStatus(status);
        dao.saveEntity(equipment);
    }

    /** createTime 回拨：insert 后 update（update 路径不触碰 createTime），供遗留基线分支精确断言。 */
    private void backdateCreateTime(Long equipmentId, Timestamp createTime) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment managed = dao.getEntityById(equipmentId);
        managed.setCreateTime(createTime);
        dao.updateEntity(managed);
    }

    private void seedStatusLog(Long id, Long equipmentId, String fromStatus, String toStatus, Timestamp changeAt) {
        IEntityDao<ErpMntEquipmentStatusLog> dao = daoProvider.daoFor(ErpMntEquipmentStatusLog.class);
        ErpMntEquipmentStatusLog log = dao.newEntity();
        log.setId(id);
        log.setEquipmentId(equipmentId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangeAt(changeAt);
        log.setSource(ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL);
        dao.saveEntity(log);
    }

    private void seedRuntimeSchedule(Long id, Long equipmentId, String thresholdHours, String baselineHours) {
        ErpMntSchedule schedule = seedScheduleCommon(id, equipmentId);
        schedule.setTriggerType(ErpMntDaoConstants.TRIGGER_TYPE_RUNTIME);
        schedule.setThresholdHours(new BigDecimal(thresholdHours));
        if (baselineHours != null) {
            schedule.setRuntimeBaselineHours(new BigDecimal(baselineHours));
        }
        daoProvider.daoFor(ErpMntSchedule.class).saveEntity(schedule);
    }

    private void seedTimeSchedule(Long id, Long equipmentId, LocalDate nextDueDate) {
        ErpMntSchedule schedule = seedScheduleCommon(id, equipmentId);
        schedule.setNextDueDate(nextDueDate);
        daoProvider.daoFor(ErpMntSchedule.class).saveEntity(schedule);
    }

    private ErpMntSchedule seedScheduleCommon(Long id, Long equipmentId) {
        ErpMntSchedule schedule = daoProvider.daoFor(ErpMntSchedule.class).newEntity();
        schedule.setId(id);
        schedule.setCode("SCH-RT-" + id);
        schedule.setName("运行时长测试计划" + id);
        schedule.setEquipmentId(equipmentId);
        schedule.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        schedule.setFrequency(1);
        schedule.setRecurrenceType(ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY);
        schedule.setStartDate(LocalDate.of(2026, 1, 1));
        schedule.setIsActive(1);
        return schedule;
    }

    private ErpMntVisit findVisitByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpMntEquipmentStatusLog findLatestLog(Long equipmentId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("equipmentId", equipmentId));
        List<ErpMntEquipmentStatusLog> logs = daoProvider.daoFor(ErpMntEquipmentStatusLog.class).findAllByQuery(q);
        return logs.stream()
                .reduce((a, b) -> {
                    int byTime = a.getChangeAt().compareTo(b.getChangeAt());
                    return byTime < 0 || (byTime == 0 && a.getId() < b.getId()) ? b : a;
                })
                .orElse(null);
    }
}
