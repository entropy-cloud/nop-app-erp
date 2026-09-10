package app.erp.aps.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-aps-013-r3 批量排产行级容错回归：行级失败（含行级守卫拒绝）记入 failures，
 * 不阻塞其他行（javadoc「不阻塞其他行」承诺行为化）；失败行会话状态被丢弃，
 * 成功行排产结果不受失败行半途脏状态污染。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsBatchScheduleIsolation extends JunitAutoTestCase {

    private static final String MACHINE_A = "100";

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testRowFailureDoesNotBlockOtherRows() {
        // 行 1：PUBLISHED 方案（非 DRAFT 源态 → 行级守卫拒绝 ERR_APS_SCHEDULE_ILLEGAL_STATUS）
        String publishedId = createSchedule("S-BATCH-PUB", "FORWARD");
        rpc(GraphQLOperationType.mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", publishedId)));
        // 行 2：合法 DRAFT 方案 + 待排工序
        String draftId = createSchedule("S-BATCH-DRAFT", "FORWARD");
        String opA = createOp("OB-1", "1", 10, MACHINE_A, 10, "0", "10", "3", "2026-07-10T08:00:00");

        // 失败行在前，验证不阻塞后续行
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpApsOperationOrder__batchScheduleForward",
                ApiRequest.build(Map.of("ids", List.of(publishedId, draftId))));
        assertEquals(0, resp.getStatus(), "批量排产应整批返回（行级失败不上抛）: " + resp);

        Map<?, ?> data = (Map<?, ?>) resp.getData();
        assertEquals(2, ((Number) data.get("totalCount")).intValue());
        assertEquals(1, ((Number) data.get("successCount")).intValue(), "合法行应成功");
        assertEquals(1, ((Number) data.get("failedCount")).intValue(), "守卫拒绝行应记入 failures");
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> failures = (List<Map<?, ?>>) data.get("failures");
        assertEquals(publishedId, String.valueOf(failures.get(0).get("id")), "失败行应为主要非法方案");
        assertTrue(String.valueOf(failures.get(0).get("code")).contains("illegal-status"),
                "行级失败码应为领域守卫码: " + failures.get(0));

        // 合法行实际完成排产（失败行未中止整批的行为证据）
        assertEquals("PLANNED", reloadOp(opA).get("status"), "合法行工序应已排定");
    }

    // ---------- helpers（镜像 TestErpApsSchedulingEngine 模式） ----------

    private String createSchedule(String code, String mode) {
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", mode);
        d.put("horizonStart", "2026-07-10T00:00:00");
        d.put("horizonEnd", "2026-07-20T00:00:00");
        d.put("status", "DRAFT");
        ApiResponse<?> r = rpc(GraphQLOperationType.mutation, "ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 Schedule 应成功");
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    private String createOp(String code, String workOrderId, int sequence, String machineId, int priority,
                            String setup, String perUnit, String qty, String earliestStart) {
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", workOrderId);
        d.put("operationName", code);
        d.put("sequence", sequence);
        d.put("machineId", machineId);
        d.put("priority", priority);
        d.put("setupTime", new java.math.BigDecimal(setup));
        d.put("runtimePerUnit", new java.math.BigDecimal(perUnit));
        d.put("qty", new java.math.BigDecimal(qty));
        d.put("status", "DRAFT");
        d.put("earliestStartDateT", earliestStart);
        ApiResponse<?> r = rpc(GraphQLOperationType.mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "创建 OperationOrder 应成功: " + r);
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    private Map<String, Object> reloadOp(String id) {
        ApiResponse<?> r = rpc(GraphQLOperationType.query, "ErpApsOperationOrder__get", ApiRequest.build(Map.of("id", id)));
        assertEquals(0, r.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) r.getData();
        return data;
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        io.nop.graphql.core.IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
