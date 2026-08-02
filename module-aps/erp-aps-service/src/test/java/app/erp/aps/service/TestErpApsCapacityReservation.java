package app.erp.aps.service;

import app.erp.aps.dao.entity.ErpApsCapacityReservation;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-MA2-019 fix：aps 排产产能并发防护负向测试。
 *
 * <p>{@code docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11 P0-MA2-019} 指出
 * owner doc {@code docs/design/aps/state-machine.md §4} 显式声明"乐观锁/资源锁"防护并发排产产能双倍占用，
 * 但实现未落地。修复（方案 A）：{@code ErpApsSchedulingProcessor.persist} 在每个 PLANNED 工序落库前向
 * {@code ErpApsCapacityReservation} 写入时段预留，{@code UK_APS_CAPACITY_RESERVATION_SLOT (machineId,
 * plannedStartT, plannedEndT)} 作为 DB 兜底，重叠 pre-check 翻译为 {@code ERR_APS_CAPACITY_CONFLICT}。
 *
 * <p>本测试覆盖三条路径：
 * <ol>
 *   <li>{@link #testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict()} — 模拟并发排产
 *       竞态（直接 pre-insert 另一调度已占用的预留行），本调度 scheduleForward 引擎以相同确定性时段重排
 *       同一工序，persist pre-check 命中重叠 → 抛 {@code ERR_APS_CAPACITY_CONFLICT}（确定性，单会话模拟）。</li>
 *   <li>{@link #testForwardScheduleAcquiresReservation()} — 正向基线：scheduleForward 成功为 PLANNED 工序
 *       申请预留，且 reservation 行可读回。</li>
 *   <li>{@link #testReservationsReleasedOnRushOrderRevert()} — 插单区间重排：原 PLANNED 工序被回退时
 *       应按 operationOrderId 硬删除其预留，避免遗留行阻塞后续重排。</li>
 * </ol>
 *
 * <p>多线程机制取舍：真实 2-线程 CountDownLatch 测试受事务隔离 + IGraphQLEngine 调度时序影响不稳定；
 * 单会话"pre-insert 已落地预留"是确定性更强的等价模拟（覆盖生产路径 pre-check + JdbcException 翻译），
 * 与 {@code TestErpInvConcurrentDeduct#testConcurrentDeductRetrySucceeds} 的"outer session 推进版本"
 * 模拟同型。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsCapacityReservation extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private static final Long MACHINE_A = 100L;
    private static final LocalDateTime HORIZON_START = LocalDateTime.parse("2026-07-10T00:00:00");
    private static final LocalDateTime HORIZON_END = LocalDateTime.parse("2026-07-20T00:00:00");

    /**
     * 并发排产产能冲突防护：直接 pre-insert 一条预留（machineA, 09:00-10:00）模拟另一调度已胜出占用，
     * 本调度 scheduleForward 以确定性时段（earliest=09:00、60min duration）将同一工作中心同工序重排到
     * 09:00-10:00，persist 的重叠 pre-check 命中 → 抛 {@code ERR_APS_CAPACITY_CONFLICT}。
     */
    @Test
    public void testConcurrentScheduleForwardSharedWorkcenterThrowsCapacityConflict() {
        Long scheduleId = createSchedule("S-CONC");
        // 工序 X：DRAFT, machineA, earliest=09:00, setup=0 + runtime=60 * qty=1 → duration=60min
        // 引擎将 X 排到 [09:00, 10:00]（machineA 在展望期起点空闲，无维护停机）。
        createOp("OX-CONC", 1L, 10, MACHINE_A, 10, "0", "60", "1", "2026-07-10T09:00:00");

        // 模拟另一并发 scheduleForward 已抢先占用同工作中心同时段（不同 orderId，UK 仍冲突）
        seedReservation(MACHINE_A, "2026-07-10T09:00:00", "2026-07-10T10:00:00", 999_001L);

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__scheduleForward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));

        assertTrue(resp.getStatus() != 0,
                "并发排产共享工作中心产能冲突应被拒绝（status!=0）: " + resp);
        assertEquals(ErpApsErrors.ERR_APS_CAPACITY_CONFLICT.getErrorCode(), resp.getCode(),
                "应抛 ERR_APS_CAPACITY_CONFLICT 错误码");
    }

    /**
     * 正向基线：无并发冲突时 scheduleForward 成功，PLANNED 工序在 DB 留下预留行。
     */
    @Test
    public void testForwardScheduleAcquiresReservation() {
        Long scheduleId = createSchedule("S-OK");
        Long opId = createOp("OX-OK", 1L, 10, MACHINE_A, 10, "0", "30", "1", "2026-07-10T09:00:00");

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__scheduleForward",
                ApiRequest.build(Map.of("scheduleId", scheduleId)));
        assertEquals(0, resp.getStatus(), "无冲突时 scheduleForward 应成功: " + resp);

        Map<String, Object> reloaded = reloadOp(opId);
        assertEquals("PLANNED", reloaded.get("status"), "工序应排定为 PLANNED");

        List<ErpApsCapacityReservation> reservations = findReservationsByOrder(opId);
        assertEquals(1, reservations.size(), "PLANNED 工序应恰好产生 1 条产能预留");
        ErpApsCapacityReservation r = reservations.get(0);
        assertEquals(MACHINE_A, r.getMachineId());
        assertEquals(LocalDateTime.parse("2026-07-10T09:00:00"), r.getPlannedStartT().toLocalDateTime());
    }

    /**
     * 插单区间重排释放原预留：先让 PLANNED 工序占 09:00-09:30 时段（手工补建对应预留），再 insertRushOrder
     * 用高优先级急单将其挤出窗口，引擎把原工序重排到 09:20-09:50。若 release 未发生，则原预留（09:00-09:30）
     * 与新时段（09:20-09:50）区间重叠（existing.end=09:30 &gt; new.start=09:20），persist pre-check 会抛
     * ERR_APS_CAPACITY_CONFLICT。因此 insertRushOrder 成功 + OE 仅 1 条新时段预留，等价于验证 release 已生效。
     */
    @Test
    public void testReservationsReleasedOnRushOrderRevert() {
        Long scheduleId = createSchedule("S-REVERT");
        // 已 PLANNED 的低优先级工序 OE（占 09:00-09:30）
        Long existing = createOpPlanned("OE-REL", 1L, 10, MACHINE_A, 80, "0", "30", "1",
                "2026-07-11T09:00:00", "2026-07-11T09:30:00");
        // 手工补建对应预留（createOpPlanned 不走 persist，须补建以模拟 release 前的稳态）
        seedReservation(MACHINE_A, "2026-07-11T09:00:00", "2026-07-11T09:30:00", existing);
        // 急单：priority=10 高于 OE 的 80，窗口 09:00-10:00 → OE 被回退重排
        Long rush = createOp("OR-REL", 2L, 10, MACHINE_A, 10, "0", "20", "1", "2026-07-11T09:00:00");
        setLatest(rush, "2026-07-11T09:55:00");

        assertEquals(1, findReservationsByOrder(existing).size(),
                "前置：OE 应有 1 条原时段预留待释放");

        ApiResponse<?> resp = runMutation("ErpApsOperationOrder__insertRushOrder",
                ApiRequest.build(Map.of("operationOrderId", rush)));
        assertEquals(0, resp.getStatus(),
                "插单应成功——若 release 未生效，OE 重排新时段会与原预留重叠被拒绝: " + resp);

        List<ErpApsCapacityReservation> after = findReservationsByOrder(existing);
        assertEquals(1, after.size(), "OE 重排后应恰好 1 条新时段预留（原预留已释放，新预留已申请）");
        // 原时段 09:00 已被释放，新时段 ≥ 09:20（rush 让 OE 排在其后）
        assertTrue(after.get(0).getPlannedStartT().toLocalDateTime()
                        .isAfter(LocalDateTime.parse("2026-07-11T09:00:00")),
                "OE 新预留时段应晚于原 09:00（原预留已释放）");
    }

    // ==================== 辅助 ====================

    private Long createSchedule(String code) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", "FORWARD");
        d.put("horizonStart", HORIZON_START.toString());
        d.put("horizonEnd", HORIZON_END.toString());
        d.put("status", "DRAFT");
        ApiResponse<?> r = runMutation("ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 Schedule 应成功");
        return idOf(r.getData());
    }

    private Long createOp(String code, Long workOrderId, int sequence, Long machineId, int priority,
                          String setup, String perUnit, String qty, String earliestStart) {
        Map<String, Object> d = baseOp(code, workOrderId, sequence, machineId, priority, setup, perUnit, qty);
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", earliestStart);
        return saveOp(d, code);
    }

    private Long createOpPlanned(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                 String setup, String perUnit, String qty,
                                 String plannedStart, String plannedEnd) {
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

    private void setLatest(Long id, String latestEnd) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", id);
        d.put("latestEndDateT", latestEnd);
        ApiResponse<?> r = runMutation("ErpApsOperationOrder__update", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "update latestEndDateT 应成功");
    }

    /** 直接持久化一条预留（模拟并发调度已抢先占用），flushSession 使其对当前 session 的 pre-check 可见。 */
    private void seedReservation(Long machineId, String start, String end, Long operationOrderId) {
        ErpApsCapacityReservation r = reservationDao().newEntity();
        r.setMachineId(machineId);
        r.setPlannedStartT(Timestamp.valueOf(LocalDateTime.parse(start)));
        r.setPlannedEndT(Timestamp.valueOf(LocalDateTime.parse(end)));
        r.setOperationOrderId(operationOrderId);
        reservationDao().saveEntity(r);
        ormTemplate.flushSession();
    }

    private List<ErpApsCapacityReservation> findReservationsByOrder(Long operationOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("operationOrderId", operationOrderId));
        return reservationDao().findAllByQuery(q);
    }

    private Map<String, Object> reloadOp(Long id) {
        ApiResponse<?> r = runQuery("ErpApsOperationOrder__get",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        assertEquals(0, r.getStatus(), "reload op " + id + " 应成功");
        return (Map<String, Object>) r.getData();
    }

    private IEntityDao<ErpApsCapacityReservation> reservationDao() {
        return daoProvider.daoFor(ErpApsCapacityReservation.class);
    }

    private Long idOf(Object data) {
        Object id = ((Map<?, ?>) data).get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    private ApiResponse<?> runMutation(String action, ApiRequest<?> request) {
        return executeRpc(mutation, action, request);
    }

    private ApiResponse<?> runQuery(String action, ApiRequest<?> request) {
        return executeRpc(query, action, request);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
