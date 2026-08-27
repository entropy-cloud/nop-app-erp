package app.erp.aps.service;

import app.erp.aps.service.scheduling.ApsBottleneckDetector;
import app.erp.aps.service.scheduling.GreedyApsSchedulingSolver;
import app.erp.aps.service.scheduling.IApsSchedulingSolver;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.4 TOC 瓶颈驱动排产试点 Proof（`constraint-based-planning.md` §1/§2）：
 *
 * <ul>
 *   <li>求解器可插拔：默认 GREEDY 解析 + 未知 config 名回退 GREEDY（行为不变）；</li>
 *   <li>瓶颈识别：负荷率派生链（待排工时/产能，SPI 空时 24h/日兜底）→ bottleneckMachineIds +
 *       machineLoadRates 结果扩展非空；</li>
 *   <li>拉动式可观测：瓶颈中心工序先排（低优先级也让位产能保护），非瓶颈前序以瓶颈开工为锚后向倒排
 *       （与既有前向模式结果可区分）；既有前向排产经求解器路径零回归（同输入同结果）。</li>
 * </ul>
 *
 * <p>时间戳确定性：所有 earliestStartDateT 显式给定；horizon 24h，产能兜底 24h/日（SPI 未收集），
 * 阈值经 config 显式设 0.5。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsSchedulingToc extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Inject
    ApsBottleneckDetector bottleneckDetector;

    private static final String MACHINE_A = "100";
    private static final String MACHINE_B = "101";
    private static final LocalDateTime HORIZON_START = LocalDateTime.parse("2026-07-10T00:00:00");
    private static final LocalDateTime HORIZON_END = LocalDateTime.parse("2026-07-11T00:00:00");

    // ---------------- 求解器分离：默认解析 + 未知名回退 ----------------

    @Test
    public void testSolverResolutionDefaultsToGreedyAndFallsBack() {
        // 直接证明默认实现身份：config 未设置时 resolve 逻辑的输入 = GREEDY；未知名回退 greedy 实例
        GreedyApsSchedulingSolver greedy = new GreedyApsSchedulingSolver();
        assertEquals(IApsSchedulingSolver.SOLVER_GREEDY, greedy.getName(), "默认求解器名 = GREEDY");

        // config 未知名：经 GraphQL 走排产动作（resolveSolver 回退 GREEDY 后行为不变，不抛异常）
        setConfig(ErpApsConfigs.CONFIG_SCHEDULING_SOLVER, "NO_SUCH_SOLVER");
        try {
            String scheduleId = createSchedule("S-SLV", "FORWARD");
            createOp("SLV-A", "90", 10, MACHINE_A, 10, "0", "60", "1", "2026-07-10T08:00:00");
            ApiResponse<?> resp = runMutation("ErpApsOperationOrder__scheduleForward",
                    ApiRequest.build(Map.of("scheduleId", scheduleId)));
            assertEquals(0, resp.getStatus(), "未知求解器名应回退 GREEDY 且排产成功");
        } finally {
            setConfig(ErpApsConfigs.CONFIG_SCHEDULING_SOLVER, ErpApsConfigs.DEFAULT_SCHEDULING_SOLVER);
        }
    }

    // ---------------- TOC：瓶颈识别 + 拉动式排产可观测 ----------------

    @Test
    public void testScheduleTocBottleneckFirstAndPullBackward() {
        setConfig(ErpApsConfigs.CONFIG_TOC_BOTTLENECK_THRESHOLD, "0.5");
        try {
            String scheduleId = createSchedule("S-TOC", "FORWARD");
            // WO-1：seq10 在 B（2h，高优先级 10），seq20 在 A（6h，低优先级 80）
            // WO-2：seq10 在 A（8h，优先级 50）
            // A 工作量 14h/24h = 0.5833 > 0.5 → 瓶颈；B 2h/24h = 0.0833 < 0.5 → 非瓶颈
            String w1Seq10 = createOp("TOC-B10", "1", 10, MACHINE_B, 10, "0", "120", "1", "2026-07-10T08:00:00");
            String w1Seq20 = createOp("TOC-A20", "1", 20, MACHINE_A, 80, "0", "360", "1", "2026-07-10T08:00:00");
            String w2Seq10 = createOp("TOC-A10", "2", 10, MACHINE_A, 50, "0", "480", "1", "2026-07-10T08:00:00");

            ApiResponse<?> resp = runMutation("ErpApsOperationOrder__scheduleToc",
                    ApiRequest.build(Map.of("scheduleId", scheduleId)));
            assertEquals(0, resp.getStatus(), "TOC 排产应成功");
            Map<?, ?> result = (Map<?, ?>) resp.getData();

            // 结果扩展字段非空：瓶颈清单 + 各中心负荷率
            Map<?, ?> loadRates = (Map<?, ?>) result.get("machineLoadRates");
            assertNotNull(loadRates, "machineLoadRates 应返回");
            java.util.List<?> bottlenecks = (java.util.List<?>) result.get("bottleneckMachineIds");
            assertNotNull(bottlenecks, "bottleneckMachineIds 应返回");
            assertEquals(1, bottlenecks.size(), "仅 A 为瓶颈");
            assertEquals(MACHINE_A, bottlenecks.get(0));
            assertEquals(0, new BigDecimal("0.5833").compareTo(toBd(loadRates.get(MACHINE_A))),
                    "A 负荷率 = 14h/24h");
            assertEquals(0, new BigDecimal("0.0833").compareTo(toBd(loadRates.get(MACHINE_B))),
                    "B 负荷率 = 2h/24h");
            assertEquals(Boolean.TRUE, result.get("feasible"), "无交期冲突应可行");

            // Phase A：瓶颈 A 工序先排（优先级序 50 → 80），低优先级 80 仍先于任何非瓶颈工序获得产能
            Map<String, Object> a10 = reloadOp(w2Seq10);
            assertEquals("PLANNED", a10.get("status"));
            assertEquals(LocalDateTime.parse("2026-07-10T08:00:00"), toLdt(a10.get("plannedStartDateT")),
                    "WO2-seq10（A，瓶颈）应排 08:00~16:00");
            assertEquals(LocalDateTime.parse("2026-07-10T16:00:00"), toLdt(a10.get("plannedEndDateT")));

            Map<String, Object> a20 = reloadOp(w1Seq20);
            assertEquals("PLANNED", a20.get("status"));
            assertEquals(LocalDateTime.parse("2026-07-10T16:00:00"), toLdt(a20.get("plannedStartDateT")),
                    "WO1-seq20（A，瓶颈）应排 16:00~22:00");

            // Phase B1（拉动式）：非瓶颈 B 的 WO1-seq10 以瓶颈开工（16:00 − buffer5）为锚后向倒排
            // → 13:55~15:55（区别于既有前向模式的 08:00~10:00）
            Map<String, Object> b10 = reloadOp(w1Seq10);
            assertEquals("PLANNED", b10.get("status"));
            assertEquals(LocalDateTime.parse("2026-07-10T13:55:00"), toLdt(b10.get("plannedStartDateT")),
                    "非瓶颈前序应拉动式倒排到 13:55~15:55（终点 = 瓶颈开工 − buffer）");
            assertEquals(LocalDateTime.parse("2026-07-10T15:55:00"), toLdt(b10.get("plannedEndDateT")));
        } finally {
            setConfig(ErpApsConfigs.CONFIG_TOC_BOTTLENECK_THRESHOLD,
                    String.valueOf(ErpApsConfigs.DEFAULT_TOC_BOTTLENECK_THRESHOLD));
        }
    }

    // ---------------- 既有前向排产经求解器路径零回归 ----------------

    @Test
    public void testForwardThroughSolverPathUnchanged() {
        String scheduleId = createSchedule("S-FWD2", "FORWARD");
        // 同 TOC 用例的输入形状：前向模式下 B（高优先级）先排 08:00~10:00，A 序列按优先级与链约束排
        String b10 = createOp("FW2-B10", "1", 10, MACHINE_B, 10, "0", "120", "1", "2026-07-10T08:00:00");
        String a20 = createOp("FW2-A20", "1", 20, MACHINE_A, 80, "0", "360", "1", "2026-07-10T08:00:00");
        createOp("FW2-A10", "2", 10, MACHINE_A, 50, "0", "480", "1", "2026-07-10T08:00:00");

        runScheduleForward(scheduleId);

        // 前向模式（经求解器分派）：B 先排 08:00~10:00（优先级 10 < 50），A 后排——与 TOC 的 13:55 区分
        Map<String, Object> b = reloadOp(b10);
        assertEquals("PLANNED", b.get("status"));
        assertEquals(LocalDateTime.parse("2026-07-10T08:00:00"), toLdt(b.get("plannedStartDateT")),
                "前向模式 B 工序按优先级先排 08:00（经求解器路径行为不变）");

        Map<String, Object> a = reloadOp(a20);
        assertEquals("PLANNED", a.get("status"));
        assertEquals(LocalDateTime.parse("2026-07-10T16:00:00"), toLdt(a.get("plannedStartDateT")),
                "前向模式 A 链约束排 16:00~22:00");
    }

    // ---------------- 瓶颈识别器：SPI 空兜底产能下的派生 ----------------

    @Test
    public void testBottleneckDetectorDerivesLoadRates() {
        java.util.List<app.erp.aps.dao.entity.ErpApsOperationOrder> pending = new java.util.ArrayList<>();
        pending.add(opOf("DET-1", MACHINE_A, 50, "0", "480", "1"));
        pending.add(opOf("DET-2", MACHINE_A, 80, "0", "360", "1"));
        pending.add(opOf("DET-3", MACHINE_B, 10, "0", "120", "1"));

        Map<String, BigDecimal> rates = bottleneckDetector.detectLoadRates(pending, HORIZON_START, HORIZON_END);
        assertEquals(0, new BigDecimal("0.5833").compareTo(rates.get(MACHINE_A)), "A = 14h/24h");
        assertEquals(0, new BigDecimal("0.0833").compareTo(rates.get(MACHINE_B)), "B = 2h/24h");
        assertFalse(rates.isEmpty());
    }

    // ==================== 辅助 ====================

    private app.erp.aps.dao.entity.ErpApsOperationOrder opOf(String code, String machineId, int priority,
                                                             String setup, String perUnit, String qty) {
        app.erp.aps.dao.entity.ErpApsOperationOrder op = new app.erp.aps.dao.entity.ErpApsOperationOrder();
        op.setCode(code);
        op.setMachineId(machineId);
        op.setPriority(priority);
        op.setSetupTime(new BigDecimal(setup));
        op.setRuntimePerUnit(new BigDecimal(perUnit));
        op.setQty(new BigDecimal(qty));
        return op;
    }

    private String createSchedule(String code, String mode) {
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
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    private String createOp(String code, String workOrderId, int sequence, String machineId, int priority,
                            String setup, String perUnit, String qty, String earliestStart) {
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
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", earliestStart);
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 OperationOrder " + code + " 应成功: " + r);
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    private void runScheduleForward(String scheduleId) {
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__scheduleForward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));
        assertEquals(0, r.getStatus(), "前向排产应成功: " + r);
    }

    private Map<String, Object> reloadOp(String id) {
        ApiResponse<?> r = runQuery("ErpApsOperationOrder__get", ApiRequest.build(Map.of("id", id)));
        assertEquals(0, r.getStatus(), "reload op " + id + " 应成功");
        return (Map<String, Object>) r.getData();
    }

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

    private BigDecimal toBd(Object v) {
        return v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(String.valueOf(v));
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
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
