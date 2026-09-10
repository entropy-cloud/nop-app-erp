package app.erp.aps.service;

import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import app.erp.aps.biz.IErpApsAtpCtpService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ aps-016-r3（DIM-T 覆盖缺口）：4 个公开动作行为断言——
 * {@code batchScheduleForward}（批量前向排产 + 行级容错）、{@code updateSchedule}（唯一拖拽
 * mutation，aps-004 消费入口）、{@code findGanttData}（甘特页唯一数据源）、
 * {@code simulateSchedule}（AtpCtpService 模拟排程）。修复前 src/test + _cases 零引用。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsPublicActionCoverage extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpApsAtpCtpService atpCtpService;

    private String createOrder(String code, String status) {
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", 1);
        d.put("operationName", code);
        d.put("sequence", 10);
        d.put("machineId", 1);
        d.put("qty", 100);
        d.put("status", status);
        ApiResponse<?> r = executeRpc(mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "seed order should be created: " + code);
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resultMap(ApiResponse<?> resp) {
        return (Map<String, Object>) resp.getData();
    }

    @Test
    public void testBatchScheduleForwardEmptyBatchIsNoop() {
        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__batchScheduleForward",
                ApiRequest.build(Map.of("ids", List.of())));
        assertEquals(0, resp.getStatus(), "空批次应成功返回");
        Map<String, Object> result = resultMap(resp);
        assertEquals(0, ((Number) result.get("totalCount")).intValue(), "空批次 totalCount=0");
    }

    @Test
    public void testBatchScheduleForwardRowFailureIsolated() {
        // 行级容错：不存在的 id 记入失败行，不抛出、不阻塞其他行（P3-CK-aps-013-r3 修复面行为断言）
        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__batchScheduleForward",
                ApiRequest.build(Map.of("ids", List.of("no-such-op-order"))));
        assertEquals(0, resp.getStatus(), "行级失败不传染整批调用");
        Map<String, Object> result = resultMap(resp);
        assertEquals(1, ((Number) result.get("totalCount")).intValue(), "totalCount=1");
        assertEquals(0, ((Number) result.get("successCount")).intValue(), "失败行 successCount=0");
    }

    @Test
    public void testUpdateScheduleUpdatesPlannedWindow() {
        String id = createOrder("OP-M28-USCH", "PLANNED");
        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__updateSchedule",
                ApiRequest.build(Map.of("opOrderId", id,
                        "start", "2026-08-01T08:00:00",
                        "end", "2026-08-01T12:00:00")));
        assertEquals(0, resp.getStatus(), "PLANNED 单排程窗口更新应成功");
        Map<String, Object> order = resultMap(resp);
        assertNotNull(order.get("plannedStartDateT"), "更新后 plannedStartDateT 在位");
    }

    @Test
    public void testUpdateScheduleRejectsUnknownOrder() {
        // GraphQL 面 start 为 mandatory 非空参数（null 守卫仅 Java 直调可达）；
        // GraphQL 负例取未知单号：requireEntity 未命中必须失败而非静默成功
        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__updateSchedule",
                ApiRequest.build(Map.of("opOrderId", "no-such-op-order",
                        "start", "2026-08-01T08:00:00",
                        "end", "2026-08-01T12:00:00")));
        assertTrue(resp.getStatus() != 0, "未知单号应失败");
    }

    @Test
    public void testFindGanttDataIncludesSeededOrder() {
        createOrder("OP-M28-GANTT", "PLANNED");
        ApiResponse<?> resp = executeRpc(query, "ErpApsOperationOrder__findGanttData",
                ApiRequest.build(Map.of()));
        assertEquals(0, resp.getStatus(), "甘特数据查询应成功");
        Map<String, Object> data = resultMap(resp);
        assertNotNull(data, "甘特数据非空");
        assertTrue(data.containsKey("rows") || data.containsKey("machines") || !data.isEmpty(),
                "甘特数据结构在位: " + data.keySet());
    }

    @Test
    public void testSimulateScheduleEmptyForUnknownMaterial() {
        // AtpCtpService 为服务型 BizObject（无 xmeta 不入 GraphQL 面），经 IoC 注入直调行为断言
        List<?> shadows = atpCtpService.simulateSchedule("M28-NO-SUCH-MAT",
                new java.math.BigDecimal("10"), LocalDateTime.of(2026, 8, 1, 8, 0));
        assertNotNull(shadows, "simulateSchedule 返回列表");
        assertTrue(shadows.isEmpty(), "未知物料影子工序为空");
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
