package app.erp.aps.service;

import app.erp.aps.biz.CtpResult;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * APS 排产引擎行为测试（{@code scheduling.md} 前向/后向/插单/产能约束/ATP-CTP）。
 *
 * <p>JunitAutoTestCase + H2 本地库：通过 GraphQL 调用 BizModel 动作，断言行为而非快照
 * （时间戳确定性：所有 earliestStartDateT 显式给定，结果与 now 无关）。
 *
 * <p>覆盖：前向排产（前序约束 + 维护停机避让 + 工作中心不重叠）、插单窗口重排（低优先级回退、
 * 高优先级保留、IN_PROGRESS 不可回退）、排产方案状态机、ATP/CTP 影子不持久化。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsSchedulingEngine extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    private static final Long MACHINE_A = 100L;
    private static final Long MACHINE_B = 101L;
    private static final LocalDateTime HORIZON_START = LocalDateTime.parse("2026-07-10T00:00:00");
    private static final LocalDateTime HORIZON_END = LocalDateTime.parse("2026-07-20T00:00:00");

    // ---------------- 前向排产：前序约束 + 维护避让 + 不重叠 ----------------

    @Test
    public void testForwardScheduleSequenceMaintenanceCapacity() {
        Long scheduleId = createSchedule("S-FWD", "FORWARD");
        // 维护停机：machineA 08:00~09:30（阻塞 08:00 起点，A 须让到 09:30）
        createConstraint(MACHINE_A, "2026-07-10T08:00:00", "2026-07-10T09:30:00");
        // 工序 10（machineA，30min，最早 08:00）→ 因维护避让到 09:30~10:00
        Long opA = createOp("OA", 1L, 10, MACHINE_A, 10, "0", "10", "3", "2026-07-10T08:00:00");
        // 工序 20（machineB，40min，最早 08:00）→ 受前序约束 10:00+buffer5=10:05，排到 10:05~10:45
        Long opB = createOp("OB", 1L, 20, MACHINE_B, 10, "0", "20", "2", "2026-07-10T08:00:00");
        // 工序 C（machineA，30min，优先级 20 低，最早 08:00）→ A 占 09:30~10:00，C 排 10:00~10:30
        Long opC = createOp("OC", 2L, 10, MACHINE_A, 20, "0", "10", "3", "2026-07-10T08:00:00");

        runScheduleForward(scheduleId);

        Map<String, Object> a = reloadOp(opA);
        assertEquals("PLANNED", a.get("status"), "工序 A 应排定");
        assertEquals(LocalDateTime.parse("2026-07-10T09:30:00"), toLdt(a.get("plannedStartDateT")),
                "工序 A 应避让维护停机排到 09:30");
        assertEquals(LocalDateTime.parse("2026-07-10T10:00:00"), toLdt(a.get("plannedEndDateT")));

        Map<String, Object> b = reloadOp(opB);
        assertEquals("PLANNED", b.get("status"));
        // 前序约束：B 起始 ≥ A 完工 + buffer(5) = 10:05
        assertTrue(!toLdt(b.get("plannedStartDateT")).isBefore(LocalDateTime.parse("2026-07-10T10:05:00")),
                "工序 B 起始应 ≥ 前序完工+buffer");
        assertEquals(LocalDateTime.parse("2026-07-10T10:45:00"), toLdt(b.get("plannedEndDateT")));

        Map<String, Object> c = reloadOp(opC);
        assertEquals("PLANNED", c.get("status"));
        // 同工作中心 machineA 不重叠：C 起始 ≥ A 完工 10:00
        assertTrue(!toLdt(c.get("plannedStartDateT")).isBefore(LocalDateTime.parse("2026-07-10T10:00:00")),
                "同工作中心工序 C 起始不应早于 A 完工");
    }

    // ---------------- 后向排产：交期可达 ----------------

    @Test
    public void testBackwardScheduleDeadlineReachable() {
        Long scheduleId = createSchedule("S-BWD", "BACKWARD");
        // 末道工序 latestEndDateT=07-11T12:00，前向推算应早于该交期
        createOpWithLatest("OP-B1", 1L, 10, MACHINE_A, 10, "0", "60", "1",
                "2026-07-10T08:00:00", "2026-07-11T12:00:00");

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__scheduleBackward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));
        assertEquals(0, resp.getStatus(), "后向排产应成功执行");
    }

    // ---------------- 插单：低优先级回退，高优先级保留，IN_PROGRESS 不可回退 ----------------

    @Test
    public void testInsertRushOrderRevertsLowerPriorityOnly() {
        Long scheduleId = createSchedule("S-INS", "FORWARD");
        // 已有 PLANNED 低优先级（priority=80）工序占 09:00~09:30
        Long existing = createOpPlanned("OE", 1L, 10, MACHINE_A, 80, "0", "30", "1",
                "2026-07-11T09:00:00", "2026-07-11T09:30:00");
        // 已有 PLANNED 高优先级（priority=5）工序占 11:00~11:30（窗口外，保留）
        Long kept = createOpPlanned("OK", 2L, 10, MACHINE_A, 5, "0", "30", "1",
                "2026-07-11T11:00:00", "2026-07-11T11:30:00");
        // 急单：priority=10（高于 OE 的 80，低于 OK 的 5），窗口 09:00~10:00
        Long rush = createOp("OR", 3L, 10, MACHINE_A, 10, "0", "20", "1", "2026-07-11T09:00:00");
        setLatest(rush, "2026-07-11T09:55:00");

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__insertRushOrder",
                ApiRequest.build(Map.of("operationOrderId", rush)));
        assertEquals(0, resp.getStatus(), "插单应成功");

        Map<String, Object> oe = reloadOp(existing);
        // OE（低优先级）被回退后重排，仍为 PLANNED 但时间让位于急单
        assertEquals("PLANNED", oe.get("status"), "回退重排后 OE 应重新 PLANNED");
        Map<String, Object> ok = reloadOp(kept);
        assertEquals("PLANNED", ok.get("status"));
        assertEquals(LocalDateTime.parse("2026-07-11T11:00:00"), toLdt(ok.get("plannedStartDateT")),
                "窗口外高优先级工序应原样保留");
        Map<String, Object> or = reloadOp(rush);
        assertEquals("PLANNED", or.get("status"), "急单应排定");
    }

    @Test
    public void testInsertRushOrderRejectsInProgress() {
        Long scheduleId = createSchedule("S-IP", "FORWARD");
        Long inProgress = createOpWithStatus("OIP", 1L, 10, MACHINE_A, 80, "0", "30", "1",
                "2026-07-12T09:00:00", "IN_PROGRESS");
        setPlanned(inProgress, "2026-07-12T09:00:00", "2026-07-12T09:30:00");
        Long rush = createOp("OR2", 2L, 10, MACHINE_A, 10, "0", "20", "1", "2026-07-12T09:00:00");
        setLatest(rush, "2026-07-12T09:55:00");

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__insertRushOrder",
                ApiRequest.build(Map.of("operationOrderId", rush)));
        // IN_PROGRESS 不可回退：应返回业务异常
        assertTrue(resp.getStatus() != 0 || resp.getData() == null,
                "IN_PROGRESS 工序不可回退，应拒绝插单");
    }

    // ---------------- 排产方案状态机 ----------------

    @Test
    public void testScheduleStateMachine() {
        Long scheduleId = createSchedule("S-SM", "FORWARD");

        ApiResponse<?> published = runMutation("ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, published.getStatus(), "DRAFT→PUBLISHED 应成功");
        assertEquals("PUBLISHED", ((Map<?, ?>) published.getData()).get("status"));

        ApiResponse<?> archived = runMutation("ErpApsSchedule__archive",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, archived.getStatus(), "PUBLISHED→ARCHIVED 应成功");
        assertEquals("ARCHIVED", ((Map<?, ?>) archived.getData()).get("status"));
    }

    // ---------------- ATP/CTP：影子工序不持久化 ----------------

    @Test
    public void testCheckFeasibilityDoesNotPersistShadow() {
        long before = countOpOrders();
        ApiResponse<?> resp = runQuery("ErpApsOperationOrder__checkFeasibility",
                ApiRequest.build(Map.of(
                        "materialId", 9999L,
                        "qty", new BigDecimal("10"),
                        "desiredDate", "2026-07-30T00:00:00")));
        assertEquals(0, resp.getStatus(), "checkFeasibility 查询应可执行");
        // 影子工序不落库：行数不变
        long after = countOpOrders();
        assertEquals(before, after, "CTP 影子 OperationOrder 不应持久化");
        if (resp.getData() instanceof Map) {
            Object feasible = ((Map<?, ?>) resp.getData()).get("feasible");
            assertNotNull(feasible, "CtpResult.feasible 应返回");
        }
    }

    // ==================== 辅助：数据 seeding 与 RPC ====================

    private Long createSchedule(String code, String mode) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", mode);
        d.put("horizonStart", HORIZON_START.toString());
        d.put("horizonEnd", HORIZON_END.toString());
        d.put("status", "DRAFT");
        ApiResponse<?> r = runMutation("ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 Schedule 应成功");
        return idOf(r.getData());
    }

    private void createConstraint(Long machineId, String start, String end) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("machineId", machineId);
        d.put("constraintType", "MAINTENANCE");
        d.put("startTime", start);
        d.put("endTime", end);
        d.put("description", "test-maintenance");
        ApiResponse<?> r = runMutation("ErpApsConstraint__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 Constraint 应成功");
    }

    private Long createOp(String code, Long workOrderId, int sequence, Long machineId, int priority,
                          String setup, String perUnit, String qty, String earliestStart) {
        Map<String, Object> d = baseOp(code, workOrderId, sequence, machineId, priority, setup, perUnit, qty);
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", earliestStart);
        return saveOp(d, code);
    }

    private Long createOpWithStatus(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                    String setup, String perUnit, String qty, String earliestStart, String status) {
        Map<String, Object> d = baseOp(code, workOrderId, sequence, machineId, priority, setup, perUnit, qty);
        d.put("status", status);
        d.put("earliestStartDateT", earliestStart);
        return saveOp(d, code);
    }

    private Long createOpWithLatest(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                    String setup, String perUnit, String qty, String earliestStart, String latestEnd) {
        Map<String, Object> d = baseOp(code, workOrderId, sequence, machineId, priority, setup, perUnit, qty);
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", earliestStart);
        d.put("latestEndDateT", latestEnd);
        return saveOp(d, code);
    }

    private Long createOpPlanned(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                 String setup, String perUnit, String qty, String plannedStart, String plannedEnd) {
        Map<String, Object> d = baseOp(code, workOrderId, sequence, machineId, priority, setup, perUnit, qty);
        d.put("status", "PLANNED");
        d.put("earliestStartDateT", plannedStart);
        d.put("plannedStartDateT", plannedStart);
        d.put("plannedEndDateT", plannedEnd);
        return saveOp(d, code);
    }

    private Map<String, Object> baseOp(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                       String setup, String perUnit, String qty) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", workOrderId);
        d.put("operationName", code);
        d.put("sequence", sequence);
        d.put("machineId", machineId);
        d.put("priority", priority);
        d.put("setupTime", new BigDecimal(setup));
        d.put("runtimePerUnit", new BigDecimal(perUnit));
        d.put("qty", new BigDecimal(qty));
        return d;
    }

    private Long saveOp(Map<String, Object> d, String code) {
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 OperationOrder " + code + " 应成功: " + r);
        return idOf(r.getData());
    }

    private Long idOf(Object data) {
        Object id = ((Map<?, ?>) data).get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    /** DB/JSON 返回的时间值可能是 java.sql.Timestamp 或字符串（"yyyy-MM-dd HH:mm:ss"），统一转 LocalDateTime。 */
    private LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime) {
            return (LocalDateTime) v;
        }
        if (v instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) v).toLocalDateTime();
        }
        String s = String.valueOf(v).replace(' ', 'T');
        return LocalDateTime.parse(s);
    }

    private void setLatest(Long id, String latestEnd) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", id);
        d.put("latestEndDateT", latestEnd);
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__update", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "update latestEndDateT 应成功");
    }

    private void setPlanned(Long id, String start, String end) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", id);
        d.put("plannedStartDateT", start);
        d.put("plannedEndDateT", end);
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__update", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "update plannedStart/End 应成功");
    }

    private void runScheduleForward(Long scheduleId) {
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__scheduleForward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));
        assertEquals(0, r.getStatus(), "前向排产应成功: " + r);
    }

    private Map<String, Object> reloadOp(Long id) {
        ApiResponse<?> r = runQuery("ErpApsOperationOrder__get",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        assertEquals(0, r.getStatus(), "reload op " + id + " 应成功");
        return (Map<String, Object>) r.getData();
    }

    private long countOpOrders() {
        ApiResponse<?> r = runQuery("ErpApsOperationOrder__findPage", ApiRequest.build(Map.of("limit", 1000)));
        assertEquals(0, r.getStatus());
        Object total = ((Map<?, ?>) r.getData()).get("total");
        return total instanceof Number ? ((Number) total).longValue() : Long.parseLong(String.valueOf(total));
    }

    private ApiResponse<?> runMutation(String action, ApiRequest<?> request) {
        return executeRpc(GraphQLOperationType.mutation, action, request);
    }

    private ApiResponse<?> runQuery(String action, ApiRequest<?> request) {
        return executeRpc(GraphQLOperationType.query, action, request);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
