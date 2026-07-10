package app.erp.sal.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-SAL-11 促销规则 CRUD 冒烟测试（plan §Phase 3）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalPricingRuleCrud extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateAndQuery() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ruleCode", "PROMO-001");
        data.put("ruleName", "全场9折");
        data.put("ruleType", "PERCENT_DISCOUNT");
        data.put("targetType", "LINE");
        data.put("discountPercent", 10);
        data.put("priority", 100);
        data.put("isActive", true);
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalPricingRule__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, created.getStatus());
        assertNotNull(((Map<?, ?>) created.getData()).get("id"));

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalPricingRule__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1);
    }

    @Test
    public void testUpdateRule() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ruleCode", "PROMO-002");
        data.put("ruleName", "满1000减200");
        data.put("ruleType", "AMOUNT_OFF");
        data.put("targetType", "ORDER");
        data.put("minOrderAmount", 1000);
        data.put("discountAmount", 200);
        data.put("priority", 200);
        data.put("isActive", true);
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalPricingRule__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, created.getStatus());
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id);
        upd.put("discountAmount", 300);
        ApiResponse<?> updated = executeRpc(GraphQLOperationType.mutation, "ErpSalPricingRule__update",
                ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, updated.getStatus());
        assertEquals(300, ((Number) ((Map<?, ?>) updated.getData()).get("discountAmount")).intValue());
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(opType, action, request));
    }
}
