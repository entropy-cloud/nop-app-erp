package app.erp.aps.service;

import app.erp.aps.dao.entity.ErpApsOpRouting;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-APS-06 替代工艺路线选择行为测试（RC-R1.87 / P1-RC-089，alternative-routing.md §二/四）。
 *
 * <p>经 GraphQL 调 scheduleForward/manualOverrideRouting，断言路由选择/降级/UNSCHEDULABLE/
 * manualOverride/批量约束/降级开关语义；时间字段全部显式给定（与 now 无关，确定性断言）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsAlternativeRouting extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    static final Long WC_PRIMARY = 2001L;
    static final Long WC_ALT = 2002L;
    static final Long OP_DEF = 3001L; // ErpApsOpRouting.operationId（工序定义锚）
    private static final LocalDateTime HORIZON_START = LocalDateTime.parse("2026-07-10T00:00:00");
    private static final LocalDateTime HORIZON_END = LocalDateTime.parse("2026-07-20T00:00:00");

    // ---------------- 1. 主选可用选主选（零行为变化） ----------------

    @Test
    public void testPrimaryAvailableSelectsPrimary() {
        seedRouting(101L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        seedRouting(102L, OP_DEF, WC_ALT, 2, "5", "1", false);
        Long op = createOp("AR-1", 10, WC_PRIMARY, 10, "5", "2", "10");

        runScheduleForward(createSchedule("S-AR-1"));

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("PLANNED", reloaded.get("status"));
        assertEquals(101L, toLong(reloaded.get("selectedRoutingId")), "主选可用应选主选路由");
        assertEquals("DEFAULT", reloaded.get("routingSelectionReason"));
        assertEquals(WC_PRIMARY, toLong(reloaded.get("machineId")));
        // 时间差 0：duration = 5 + 2×10 = 25 分钟
        assertEquals(0, new BigDecimal("25").compareTo(toBd(reloaded.get("totalDuration"))));
    }

    // ---------------- 2. 主选过载自动备选（时间差计入） ----------------

    @Test
    public void testPrimaryOverloadedFallsBackWithTimeDeltas() {
        seedRouting(111L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        seedRouting(112L, OP_DEF, WC_ALT, 2, "5", "1", false);
        // 同批高优先级长工序先占满 WC_PRIMARY（busy reason=op:*，非维护）
        createOp("AR-2-BLOCKER", 20, WC_PRIMARY, 5, "0", "1440", "10"); // duration=14400min>展望期
        Long op = createOp("AR-2", 10, WC_PRIMARY, 10, "5", "2", "10");

        runScheduleForward(createSchedule("S-AR-2"));

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("PLANNED", reloaded.get("status"), "主选过载应自动降级到备选");
        assertEquals(112L, toLong(reloaded.get("selectedRoutingId")));
        assertEquals("PRIMARY_OVERBOOKED", reloaded.get("routingSelectionReason"));
        assertEquals(WC_ALT, toLong(reloaded.get("machineId")));
        // 时间差计入断言：duration = (5+5) + (2+1)×10 = 40 分钟（对比无备选差值的 25）
        assertEquals(0, new BigDecimal("40").compareTo(toBd(reloaded.get("totalDuration"))),
                "备选时间差应计入 totalDuration");
        assertEquals(0, new BigDecimal("10").compareTo(toBd(reloaded.get("setupTime"))));
        assertEquals(0, new BigDecimal("3").compareTo(toBd(reloaded.get("runtimePerUnit"))));
        LocalDateTime start = toLdt(reloaded.get("plannedStartDateT"));
        LocalDateTime end = toLdt(reloaded.get("plannedEndDateT"));
        assertEquals(40, java.time.Duration.between(start, end).toMinutes(), "排程时长 = 差值计入后的 duration");
    }

    // ---------------- 3. 批量约束过滤（主选被批量排除 → 备选 + BATCH_CONSTRAINT） ----------------

    @Test
    public void testBatchConstraintFiltersPrimary() {
        seedRoutingFull(121L, OP_DEF, WC_PRIMARY, 1, "0", "0", true, "100", null); // minBatch=100
        seedRouting(122L, OP_DEF, WC_ALT, 2, "0", "0", false);
        Long op = createOp("AR-3", 10, WC_PRIMARY, 10, "5", "2", "10"); // qty=10 < 100

        runScheduleForward(createSchedule("S-AR-3"));

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("PLANNED", reloaded.get("status"));
        assertEquals(122L, toLong(reloaded.get("selectedRoutingId")));
        assertEquals("BATCH_CONSTRAINT", reloaded.get("routingSelectionReason"),
                "主选被批量约束排除后选备选应记 BATCH_CONSTRAINT");
    }

    // ---------------- 4. 全不可用 UNSCHEDULABLE + 自愈重试 ----------------

    @Test
    public void testAllRoutingsUnavailableMarksUnschedulableAndSelfHeals() {
        seedRouting(131L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        seedRouting(132L, OP_DEF, WC_ALT, 2, "0", "0", false);
        // 两个工作中心均维护停机覆盖整个展望期
        createConstraint(WC_PRIMARY, "2026-07-10T00:00:00", "2026-07-20T00:00:00");
        createConstraint(WC_ALT, "2026-07-10T00:00:00", "2026-07-20T00:00:00");
        Long op = createOp("AR-4", 10, WC_PRIMARY, 10, "5", "2", "10");

        runScheduleForward(createSchedule("S-AR-4"));
        assertEquals("UNSCHEDULABLE", reloadOp(op).get("status"), "全部候选不可用应标 UNSCHEDULABLE");
        assertNull(reloadOp(op).get("plannedStartDateT"));

        // 自愈：解除 WC_ALT 维护后重排（UNSCHEDULABLE 与 DRAFT 同池重试）
        clearMaintenance(WC_ALT);
        runScheduleForward(createSchedule("S-AR-4B"));
        Map<String, Object> healed = reloadOp(op);
        assertEquals("PLANNED", healed.get("status"), "路由可用后重排应自愈为 PLANNED");
        assertEquals(WC_ALT, toLong(healed.get("machineId")));
        assertEquals("PRIMARY_DOWN", healed.get("routingSelectionReason"),
                "主选被维护停机阻断、备选承接应记 PRIMARY_DOWN");
    }

    // ---------------- 5. manualOverride 覆盖 + 重排保持 ----------------

    @Test
    public void testManualOverrideWinsAndSurvivesReschedule() {
        seedRouting(141L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        seedRouting(142L, OP_DEF, WC_ALT, 2, "5", "1", false);
        Long op = createOp("AR-5", 10, WC_PRIMARY, 10, "5", "2", "10");

        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__manualOverrideRouting",
                ApiRequest.build(Map.of("operationOrderId", op, "routingId", 142L)));
        assertEquals(0, r.getStatus(), "manualOverrideRouting 应成功: " + r);
        Map<String, Object> overridden = reloadOp(op);
        assertEquals(Boolean.TRUE, overridden.get("manualOverride"));
        assertEquals(142L, toLong(overridden.get("selectedRoutingId")));
        assertEquals(WC_ALT, toLong(overridden.get("machineId")));
        assertEquals("DRAFT", overridden.get("status"), "强制指定后回退 DRAFT 待重排");
        assertEquals(0, new BigDecimal("10").compareTo(toBd(overridden.get("setupTime"))), "差值 5 已叠加");
        assertEquals(0, new BigDecimal("3").compareTo(toBd(overridden.get("runtimePerUnit"))));
        assertNotNull(overridden.get("remark"), "remark 审计记录应存在");

        // 重排：manualOverride 工序跳过自动路由选择，保持人工指定
        runScheduleForward(createSchedule("S-AR-5"));
        Map<String, Object> rescheduled = reloadOp(op);
        assertEquals("PLANNED", rescheduled.get("status"));
        assertEquals(WC_ALT, toLong(rescheduled.get("machineId")), "重排保持人工指定工作中心");
        assertEquals(142L, toLong(rescheduled.get("selectedRoutingId")));
        assertEquals(Boolean.TRUE, rescheduled.get("manualOverride"));
    }

    // ---------------- 6. 无路由配置回归（legacy 零行为变化） ----------------

    @Test
    public void testNoRoutingConfigKeepsLegacyBehavior() {
        Long op = createOp("AR-6", 10, WC_PRIMARY, 10, "5", "2", "10");

        runScheduleForward(createSchedule("S-AR-6"));

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("PLANNED", reloaded.get("status"));
        assertEquals(WC_PRIMARY, toLong(reloaded.get("machineId")));
        assertNull(reloaded.get("selectedRoutingId"), "无路由配置不触碰路由字段");
        assertNull(reloaded.get("routingSelectionReason"));
        LocalDateTime start = toLdt(reloaded.get("plannedStartDateT"));
        assertEquals(HORIZON_START, start, "legacy：earliestStart=展望期起点（2026-07-10T00:00）");
    }

    // ---------------- 7. 降级开关关闭：主选不可用保持 UNSCHEDULABLE ----------------

    @Test
    public void testFallbackDisabledKeepsUnschedulable() {
        seedRouting(151L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        seedRouting(152L, OP_DEF, WC_ALT, 2, "0", "0", false);
        createConstraint(WC_PRIMARY, "2026-07-10T00:00:00", "2026-07-20T00:00:00");
        Long op = createOpWithFallback("AR-7", 10, WC_PRIMARY, 10, "5", "2", "10", Boolean.FALSE);

        runScheduleForward(createSchedule("S-AR-7"));

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("UNSCHEDULABLE", reloaded.get("status"),
                "allowFallback=false 主选不可用应保持 UNSCHEDULABLE（不尝试备选）");
        assertNull(reloaded.get("selectedRoutingId"));
        assertEquals(WC_PRIMARY, toLong(reloaded.get("machineId")), "未降级则不改变工作中心");
    }

    // ---------------- 8. 强制指定非法路由被拒 ----------------

    @Test
    public void testManualOverrideRejectsDisabledOrMissingRouting() {
        seedRouting(161L, OP_DEF, WC_PRIMARY, 1, "0", "0", true);
        Long disabledRoutingId = 162L;
        seedRoutingFull(disabledRoutingId, OP_DEF, WC_ALT, 2, "0", "0", false, null, null);
        setRoutingEnabled(disabledRoutingId, Boolean.FALSE);
        Long op = createOp("AR-8", 10, WC_PRIMARY, 10, "5", "2", "10");

        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__manualOverrideRouting",
                ApiRequest.build(Map.of("operationOrderId", op, "routingId", disabledRoutingId)));
        assertTrue(r.getStatus() != 0, "未启用路由强制指定应被拒绝");

        ApiResponse<?> missing = rpc(mutation, "ErpApsOperationOrder__manualOverrideRouting",
                ApiRequest.build(Map.of("operationOrderId", op, "routingId", 999999L)));
        assertTrue(missing.getStatus() != 0, "不存在路由强制指定应被拒绝");
    }

    // ==================== helpers ====================

    private void seedRouting(Long id, Long operationId, Long machineId, int priority,
                             String setupDelta, String perUnitDelta, boolean isDefault) {
        seedRoutingFull(id, operationId, machineId, priority, setupDelta, perUnitDelta, isDefault, null, null);
    }

    private void seedRoutingFull(Long id, Long operationId, Long machineId, int priority,
                                 String setupDelta, String perUnitDelta, boolean isDefault,
                                 String minBatch, String maxBatch) {
        ormTemplate.runInSession(() -> {
            ErpApsOpRouting r = daoProvider.daoFor(ErpApsOpRouting.class).newEntity();
            r.orm_propValueByName("id", id);
            r.setOperationId(operationId);
            r.setMachineId(machineId);
            r.setPriority(priority);
            r.setSetupTimeDelta(new BigDecimal(setupDelta));
            r.setRuntimePerUnitDelta(new BigDecimal(perUnitDelta));
            r.orm_propValueByName("isDefault", isDefault);
            r.orm_propValueByName("isEnabled", Boolean.TRUE);
            if (minBatch != null) {
                r.setMinBatchQty(new BigDecimal(minBatch));
            }
            if (maxBatch != null) {
                r.setMaxBatchQty(new BigDecimal(maxBatch));
            }
            daoProvider.daoFor(ErpApsOpRouting.class).saveEntity(r);
        });
    }

    private void setRoutingEnabled(Long id, Boolean enabled) {
        ormTemplate.runInSession(() -> {
            ErpApsOpRouting r = daoProvider.daoFor(ErpApsOpRouting.class).getEntityById(id);
            r.orm_propValueByName("isEnabled", enabled);
            daoProvider.daoFor(ErpApsOpRouting.class).updateEntity(r);
        });
    }

    private void createConstraint(Long machineId, String start, String end) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("machineId", machineId);
        d.put("constraintType", "MAINTENANCE");
        d.put("startTime", start);
        d.put("endTime", end);
        d.put("description", "test-maintenance");
        assertEquals(0, rpc(mutation, "ErpApsConstraint__save", ApiRequest.build(Map.of("data", d))).getStatus());
    }

    private void clearMaintenance(Long machineId) {
        ormTemplate.runInSession(() -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("machineId", machineId));
            for (app.erp.aps.dao.entity.ErpApsConstraint c
                    : daoProvider.daoFor(app.erp.aps.dao.entity.ErpApsConstraint.class).findAllByQuery(q)) {
                daoProvider.daoFor(app.erp.aps.dao.entity.ErpApsConstraint.class).deleteEntity(c);
            }
        });
    }

    private Long createOp(String code, int sequence, Long machineId, int priority,
                          String setup, String perUnit, String qty) {
        return createOpWithFallback(code, sequence, machineId, priority, setup, perUnit, qty, null);
    }

    private Long createOpWithFallback(String code, int sequence, Long machineId, int priority,
                                      String setup, String perUnit, String qty, Boolean allowFallback) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", 7001L);
        d.put("operationName", code);
        d.put("sequence", sequence);
        d.put("machineId", machineId);
        d.put("priority", priority);
        d.put("setupTime", new BigDecimal(setup));
        d.put("runtimePerUnit", new BigDecimal(perUnit));
        d.put("qty", new BigDecimal(qty));
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", "2026-07-10T00:00:00");
        if (allowFallback != null) {
            d.put("allowFallback", allowFallback);
        }
        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 OperationOrder " + code + " 应成功: " + r);
        return toLong(((Map<?, ?>) r.getData()).get("id"));
    }

    private Long createSchedule(String code) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", "FORWARD");
        d.put("horizonStart", HORIZON_START.toString());
        d.put("horizonEnd", HORIZON_END.toString());
        d.put("status", "DRAFT");
        ApiResponse<?> r = rpc(mutation, "ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus());
        return toLong(((Map<?, ?>) r.getData()).get("id"));
    }

    private void runScheduleForward(Long scheduleId) {
        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__scheduleForward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));
        assertEquals(0, r.getStatus(), "前向排产应成功: " + r);
    }

    private Map<String, Object> reloadOp(Long id) {
        ApiResponse<?> r = rpc(GraphQLOperationType.query, "ErpApsOperationOrder__get",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        assertEquals(0, r.getStatus());
        return (Map<String, Object>) r.getData();
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static BigDecimal toBd(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    private static LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime) {
            return (LocalDateTime) v;
        }
        if (v instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) v).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(v).replace(' ', 'T'));
    }
}
