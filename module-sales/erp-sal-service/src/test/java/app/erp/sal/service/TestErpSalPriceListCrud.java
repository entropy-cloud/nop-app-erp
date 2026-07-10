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
 * UC-SAL-11 价格清单 CRUD 冒烟测试（plan §Phase 3）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalPriceListCrud extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateAndQuery() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "PL-001");
        data.put("name", "VIP价格清单");
        data.put("currencyId", pre.get("currency"));
        data.put("customerGroupCode", "VIP");
        data.put("priority", 100);
        data.put("isActive", true);
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalPriceList__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, created.getStatus());
        assertNotNull(((Map<?, ?>) created.getData()).get("id"));

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalPriceList__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1);
    }

    @Test
    public void testPriceListLineRelation() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "PL-002");
        headData.put("name", "清单-行测试");
        headData.put("currencyId", pre.get("currency"));
        headData.put("priority", 100);
        headData.put("isActive", true);
        ApiResponse<?> head = executeRpc(GraphQLOperationType.mutation, "ErpSalPriceList__save",
                ApiRequest.build(Map.of("data", headData)));
        String headId = String.valueOf(((Map<?, ?>) head.getData()).get("id"));

        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("priceListId", headId);
        lineData.put("materialId", pre.get("material"));
        lineData.put("unitPrice", 88);
        lineData.put("minQuantity", 0);
        ApiResponse<?> line = executeRpc(GraphQLOperationType.mutation, "ErpSalPriceListLine__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, line.getStatus());

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("filter_priceListId", headId);
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalPriceListLine__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1);
    }

    private Map<String, String> createPrereqs() {
        Map<String, String> ids = new LinkedHashMap<>();
        Map<String, Object> d_currency = new LinkedHashMap<>();
        d_currency.put("code", "CNY");
        d_currency.put("name", "元");
        ids.put("currency", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdCurrency__save", ApiRequest.build(Map.of("data", d_currency))).getData()).get("id")));
        Map<String, Object> d_uom = new LinkedHashMap<>();
        d_uom.put("code", "PCS");
        d_uom.put("name", "个");
        ids.put("uom", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdUoM__save", ApiRequest.build(Map.of("data", d_uom))).getData()).get("id")));
        Map<String, Object> d_material = new LinkedHashMap<>();
        d_material.put("code", "PL-MAT");
        d_material.put("name", "价格清单测试物料");
        d_material.put("materialType", "GOODS");
        d_material.put("uoMId", ids.get("uom"));
        d_material.put("status", "ACTIVE");
        ids.put("material", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdMaterial__save", ApiRequest.build(Map.of("data", d_material))).getData()).get("id")));
        return ids;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(opType, action, request));
    }
}
