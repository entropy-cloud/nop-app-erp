package app.erp.aps.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-MA2-077：aps OperationOrder start/complete/cancel 状态守卫测试（plan
 * 2026-07-30-0720-2 Phase 2）。
 *
 * <p>覆盖 owner doc aps/state-machine.md §2 迁移图契约：
 * <ul>
 *   <li>start：PLANNED→IN_PROGRESS 成功；非 PLANNED 态（DRAFT/FINISHED/CANCELLED）拒绝。</li>
 *   <li>complete：IN_PROGRESS→FINISHED 成功；非 IN_PROGRESS 态（PLANNED/FINISHED/CANCELLED）拒绝。</li>
 *   <li>cancel：DRAFT/PLANNED/IN_PROGRESS→CANCELLED 成功；终态 FINISHED/CANCELLED 拒绝。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsOperationOrderStateGuards extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStartFromPlannedSucceeds() {
        String id = createOrder("OP-START-OK", OP_STATUS_PLANNED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__start",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertEquals(0, resp.getStatus());
        assertEquals(OP_STATUS_IN_PROGRESS, ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testStartFromFinishedFails() {
        String id = createOrder("OP-START-F", OP_STATUS_FINISHED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__start",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "FINISHED->IN_PROGRESS should be rejected");
    }

    @Test
    public void testStartFromCancelledFails() {
        String id = createOrder("OP-START-C", OP_STATUS_CANCELLED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__start",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "CANCELLED->IN_PROGRESS should be rejected");
    }

    @Test
    public void testCompleteFromInProgressSucceeds() {
        String id = createOrder("OP-COMP-OK", OP_STATUS_IN_PROGRESS);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__complete",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertEquals(0, resp.getStatus());
        assertEquals(OP_STATUS_FINISHED, ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testCompleteFromPlannedFails() {
        String id = createOrder("OP-COMP-P", OP_STATUS_PLANNED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__complete",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "PLANNED->FINISHED should be rejected");
    }

    @Test
    public void testCompleteFromFinishedFails() {
        String id = createOrder("OP-COMP-F", OP_STATUS_FINISHED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__complete",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "FINISHED->FINISHED should be rejected");
    }

    @Test
    public void testCancelFromDraftSucceeds() {
        String id = createOrder("OP-CANC-D", OP_STATUS_DRAFT);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__cancel",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertEquals(0, resp.getStatus());
        assertEquals(OP_STATUS_CANCELLED, ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testCancelFromPlannedSucceeds() {
        String id = createOrder("OP-CANC-P", OP_STATUS_PLANNED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__cancel",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertEquals(0, resp.getStatus());
        assertEquals(OP_STATUS_CANCELLED, ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testCancelFromInProgressSucceeds() {
        String id = createOrder("OP-CANC-I", OP_STATUS_IN_PROGRESS);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__cancel",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertEquals(0, resp.getStatus());
        assertEquals(OP_STATUS_CANCELLED, ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testCancelFromFinishedFails() {
        String id = createOrder("OP-CANC-F", OP_STATUS_FINISHED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__cancel",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "FINISHED->CANCELLED should be rejected (terminal state)");
    }

    @Test
    public void testCancelFromCancelledFails() {
        String id = createOrder("OP-CANC-C", OP_STATUS_CANCELLED);

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsOperationOrder__cancel",
                ApiRequest.build(Map.of("operationOrderId", id)));

        assertTrue(resp.getStatus() != 0, "CANCELLED->CANCELLED should be rejected (terminal state)");
    }

    private static final String OP_STATUS_DRAFT = "DRAFT";
    private static final String OP_STATUS_PLANNED = "PLANNED";
    private static final String OP_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String OP_STATUS_CANCELLED = "CANCELLED";

    private String createOrder(String code, String status) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", 1);
        d.put("operationName", code);
        d.put("sequence", 10);
        d.put("machineId", 1);
        d.put("qty", 100);
        d.put("status", status);
        ApiResponse<?> r = executeRpc(mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus(), "seed order should be created: " + code);
        return idOf(r.getData());
    }

    private String idOf(Object data) {
        return String.valueOf(((Map<?, ?>) data).get("id"));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
