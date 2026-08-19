package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DECOMMISSIONED 设备引用守卫测试（RC-R1.77 / P1-RC-070 / UC-MAIN-08-B：「设备不可再被新维护计划/工单引用」，
 * plan 2026-08-19-0445-3 Phase 1 D2 Proof ①-⑥）。
 *
 * <p>覆盖：
 * ① DECOMMISSIONED 设备新建 Schedule/Request/Visit 经 GraphQL save 拒绝（错误码断言）
 * ② RUNNING 设备正常创建 ③ 既有行 update 非设备维度变更不误伤 ④ DRAFT visit 排程迁移拒绝
 * ⑤ 到期访问日批：DECOMMISSIONED 设备计划被跳过整批完成（nextDueDate 不推进）
 * ⑥ DECOMMISSIONED 设备的 OPEN request accept 被拒绝回滚（状态保持 OPEN 零 visit）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntEquipmentReferenceGuard extends JunitAutoTestCase {

    static final Long EQUIPMENT_DOWN = 501L;          // DECOMMISSIONED 设备
    static final Long EQUIPMENT_OK = 502L;            // RUNNING 设备
    static final Long ASSIGNEE_ID = 601L;
    static final LocalDate DUE = LocalDate.of(2026, 7, 1);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(500000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    private void seedEquipments() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
            for (Long id : new Long[]{EQUIPMENT_DOWN, EQUIPMENT_OK}) {
                ErpMntEquipment equipment = new ErpMntEquipment();
                equipment.setId(id);
                equipment.setCode("EQ-" + id);
                equipment.setName("设备" + id);
                equipment.setStatus(id.equals(EQUIPMENT_DOWN)
                        ? ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED
                        : ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
                dao.saveEntity(equipment);
            }
        });
    }

    // ---------- ① 新引用拒绝（三实体 save 钩子） ----------

    @Test
    public void testNewReferenceRejectedForDecommissionedEquipment() {
        seedEquipments();

        ApiResponse<?> schedule = rpc(mutation, "ErpMntSchedule__save",
                ApiRequest.build(Map.of("data", scheduleData("SCH-GUARD-1", EQUIPMENT_DOWN))));
        assertGuardRejected(schedule, "新维护计划（Schedule）");

        ApiResponse<?> request = rpc(mutation, "ErpMntRequest__save",
                ApiRequest.build(Map.of("data", requestData("REQ-GUARD-1", EQUIPMENT_DOWN))));
        assertGuardRejected(request, "新报修请求（Request）");

        ApiResponse<?> visit = rpc(mutation, "ErpMntVisit__save",
                ApiRequest.build(Map.of("data", visitData("VST-GUARD-1", EQUIPMENT_DOWN))));
        assertGuardRejected(visit, "新维护工单（Visit）");

        assertNull(findByCode("SCH-GUARD-1", ErpMntSchedule.class), "零 Schedule 落库");
        assertNull(findByCode("REQ-GUARD-1", ErpMntRequest.class), "零 Request 落库");
        assertNull(findByCode("VST-GUARD-1", ErpMntVisit.class), "零 Visit 落库");
    }

    // ---------- ② RUNNING 设备正常 + ③ update 非设备维度不误伤 ----------

    @Test
    public void testRunningEquipmentAcceptedAndUpdateNotHurt() {
        seedEquipments();

        ApiResponse<?> visit = rpc(mutation, "ErpMntVisit__save",
                ApiRequest.build(Map.of("data", visitData("VST-GUARD-OK", EQUIPMENT_OK))));
        assertEquals(0, visit.getStatus(), "RUNNING 设备新建 visit 应成功: " + visit);
        String id = String.valueOf(((Map<?, ?>) visit.getData()).get("id"));

        // 既有行 update 非设备维度变更（remark）不触发守卫
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id);
        upd.put("remark", "guard-not-hurt");
        ApiResponse<?> updated = rpc(mutation, "ErpMntVisit__update", ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, updated.getStatus(), "非设备维度 update 不误伤: " + updated);
        assertEquals("guard-not-hurt", ((Map<?, ?>) updated.getData()).get("remark"));

        ApiResponse<?> schedule = rpc(mutation, "ErpMntSchedule__save",
                ApiRequest.build(Map.of("data", scheduleData("SCH-GUARD-OK", EQUIPMENT_OK))));
        assertEquals(0, schedule.getStatus(), "RUNNING 设备新建 schedule 应成功: " + schedule);

        // IDLE 设备同样可被引用（仅 DECOMMISSIONED 被守卫）
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMntEquipment.class)
                .getEntityById(EQUIPMENT_OK).setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE));
        ApiResponse<?> idleVisit = rpc(mutation, "ErpMntVisit__save",
                ApiRequest.build(Map.of("data", visitData("VST-GUARD-IDLE", EQUIPMENT_OK))));
        assertEquals(0, idleVisit.getStatus(), "IDLE 设备新建 visit 应成功: " + idleVisit);
    }

    // ---------- ④ DRAFT visit 排程迁移拒绝 ----------

    @Test
    public void testScheduleMigrationRejectedForDecommissionedEquipment() {
        seedEquipments();
        Long visitId = nextId();
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
            ErpMntVisit visit = new ErpMntVisit();
            visit.setId(visitId);
            visit.setCode("VST-GUARD-SCH");
            visit.setEquipmentId(EQUIPMENT_DOWN);
            visit.setVisitDate(DUE);
            visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            visit.setAssignedTo(ASSIGNEE_ID);
            dao.saveEntity(visit);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpMntVisit__schedule", Map.of("visitId", visitId));
        assertGuardRejected(resp, "DRAFT visit 排程迁移");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT,
                daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId).getStatus(),
                "visit 保持 DRAFT（事务回滚）");
    }

    // ---------- ⑤ 到期访问日批：DECOMMISSIONED 设备计划跳过整批完成 ----------

    @Test
    public void testDueVisitBatchSkipsDecommissionedEquipmentSchedule() {
        seedEquipments();
        Long badScheduleId = nextId();
        Long okScheduleId = nextId();
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
            dao.saveEntity(rawSchedule(badScheduleId, "SCH-GUARD-BAD", EQUIPMENT_DOWN));
            dao.saveEntity(rawSchedule(okScheduleId, "SCH-GUARD-BATCH", EQUIPMENT_OK));
        });

        ApiResponse<?> resp = rpc(mutation, "ErpMntSchedule__generateDueVisits",
                Map.of("asOfDate", DUE.toString()));
        assertEquals(0, resp.getStatus(), "一条 DECOMMISSIONED 计划不应中断整批日批 job: " + resp);
        assertEquals(1, ((Number) resp.getData()).intValue(), "仅 RUNNING 设备计划生成访问");

        assertNotNull(findByCode("VST-SCH-" + okScheduleId + "-" + DUE, ErpMntVisit.class),
                "RUNNING 设备计划的访问已生成");
        assertNull(findByCode("VST-SCH-" + badScheduleId + "-" + DUE, ErpMntVisit.class),
                "DECOMMISSIONED 设备计划被查询侧排除跳过");

        ErpMntSchedule bad = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(badScheduleId);
        assertEquals(DUE, bad.getNextDueDate(), "跳过计划 nextDueDate 不推进（保持 due，处置后计划应停用）");
        ErpMntSchedule ok = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(okScheduleId);
        assertEquals(LocalDate.of(2026, 8, 1), ok.getNextDueDate(), "正常计划 nextDueDate 推进（MONTHLY+1）");
    }

    // ---------- ⑥ OPEN request accept 拒绝回滚 ----------

    @Test
    public void testAcceptRejectedForOpenRequestOnDecommissionedEquipment() {
        seedEquipments();
        Long requestId = nextId();
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
            ErpMntRequest request = new ErpMntRequest();
            request.setId(requestId);
            request.setCode("REQ-GUARD-ACC");
            request.setEquipmentId(EQUIPMENT_DOWN);
            request.setRequestDate(DUE);
            request.setDescription("处置前登记的报修");
            request.setPriority(ErpMntDaoConstants.PRIORITY_NORMAL);
            request.setStatus(ErpMntDaoConstants.REQUEST_STATUS_OPEN);
            request.setRequestedBy(ASSIGNEE_ID);
            request.setAssignedTo(ASSIGNEE_ID);
            dao.saveEntity(request);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpMntRequest__accept", Map.of("requestId", requestId));
        assertGuardRejected(resp, "OPEN request accept（对已处置设备开新维护工作）");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_OPEN,
                daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId).getStatus(),
                "request 保持 OPEN（事务回滚）");
        assertNull(findByCode("VST-REQ-" + requestId, ErpMntVisit.class), "零 RESPONSIVE visit 生成");
    }

    // ---------- helpers ----------

    private void assertGuardRejected(ApiResponse<?> resp, String scenario) {
        assertNotEquals(0, resp.getStatus(), scenario + " 应被拒绝");
        assertEquals(ErpMntErrors.ERR_EQUIPMENT_DECOMMISSIONED.getErrorCode(), resp.getCode(),
                scenario + " 应返回 ERR_EQUIPMENT_DECOMMISSIONED: " + resp);
    }

    private Map<String, Object> scheduleData(String code, Long equipmentId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("name", "计划" + code);
        data.put("equipmentId", equipmentId);
        data.put("scheduleType", ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        data.put("recurrenceType", ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY);
        data.put("frequency", 1);
        data.put("startDate", "2026-01-01");
        data.put("nextDueDate", DUE.toString());
        data.put("isActive", 1);
        return data;
    }

    private Map<String, Object> requestData(String code, Long equipmentId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("equipmentId", equipmentId);
        data.put("requestDate", DUE.toString());
        data.put("description", "报修" + code);
        data.put("priority", ErpMntDaoConstants.PRIORITY_NORMAL);
        data.put("status", ErpMntDaoConstants.REQUEST_STATUS_OPEN);
        data.put("requestedBy", ASSIGNEE_ID);
        return data;
    }

    private Map<String, Object> visitData(String code, Long equipmentId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("equipmentId", equipmentId);
        data.put("visitDate", DUE.toString());
        data.put("status", ErpMntDaoConstants.VISIT_STATUS_DRAFT);
        return data;
    }

    private ErpMntSchedule rawSchedule(Long id, String code, Long equipmentId) {
        ErpMntSchedule schedule = new ErpMntSchedule();
        schedule.setId(id);
        schedule.setCode(code);
        schedule.setName("计划" + code);
        schedule.setEquipmentId(equipmentId);
        schedule.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        schedule.setFrequency(1);
        schedule.setRecurrenceType(ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY);
        schedule.setStartDate(LocalDate.of(2026, 1, 1));
        schedule.setNextDueDate(DUE);
        schedule.setIsActive(1);
        return schedule;
    }

    private <E extends io.nop.orm.IOrmEntity> E findByCode(String code, Class<E> entityClass) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        var list = daoProvider.daoFor(entityClass).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        return rpc(op, action, ApiRequest.build(args));
    }
}
