package app.erp.inv.service;

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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证自定义 @BizMutation 方法经 IGraphQLEngine 调用时 session 是否由引擎管理。
 * 对照：直调 stockMoveBiz.generateMove(req, ctx) 会因缺 OrmSession 报 update-entity-no-current-session。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockMoveGraphQL extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testGenerateMoveViaGraphQL() {
        String uomId = saveUoM();
        String materialId = saveMaterial(uomId);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", uomId);
        line.put("quantity", 10);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", "INCOMING");
        req.put("orgId", 1001);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", 3001);
        req.put("acctSchemaId", 7001);
        req.put("currencyId", 6001);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> result = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));

        assertEquals(0, result.getStatus(), "generateMove 经 GraphQL 引擎应成功（session 由引擎管）");
        assertNotNull(((Map<?, ?>) result.getData()).get("id"));
    }

    /**
     * 验证 {@code ErpInvStockMove__reverse} 经 GraphQL 可达：业务联动 generateMove 自动 DONE 后，reverse 生成反向冲销单。
     * （{@code confirm} 为内部过渡步骤 DRAFT→CONFIRMED，generateMove 内部经 doConfirm 自动推进，
     *  复用同一 @BizMutation 注册机制；独立 confirm 无独立 DRAFT 创建入口，故不单列直测。）
     */
    @Test
    public void testReverseViaGraphQL() {
        String uomId = saveUoM();
        String materialId = saveMaterial(uomId);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", uomId);
        line.put("quantity", 10);
        line.put("unitCost", 5);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", "INCOMING");
        req.put("orgId", 1001);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", 3001);
        req.put("acctSchemaId", 7001);
        req.put("currencyId", 6001);
        req.put("relatedBillType", "GQL-REV");
        req.put("relatedBillCode", "REV-001");
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> gen = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, gen.getStatus(), "业务联动 generateMove 应自动 DONE");
        String moveId = idOf(gen);

        ApiResponse<?> rev = executeRpc(mutation, "ErpInvStockMove__reverse",
                ApiRequest.build(Map.of("moveId", moveId)));
        assertEquals(0, rev.getStatus(), "reverse 经 GraphQL 引擎应成功（DONE→生成反向冲销单）");
        assertNotNull(((Map<?, ?>) rev.getData()).get("id"));
    }

    private String saveUoM() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", "PCS");
        d.put("name", "个");
        return idOf(executeRpc(mutation, "ErpMdUoM__save", ApiRequest.build(Map.of("data", d))));
    }

    private String saveMaterial(String uomId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", "M-GQL");
        d.put("name", "graphQL验证物料");
        d.put("materialType", "GOODS");
        d.put("uoMId", uomId);
        d.put("status", "ACTIVE");
        return idOf(executeRpc(mutation, "ErpMdMaterial__save", ApiRequest.build(Map.of("data", d))));
    }

    private static String idOf(ApiResponse<?> r) {
        return String.valueOf(((Map<?, ?>) r.getData()).get("id"));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
