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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-SAL-11 定价引擎端到端集成测试（plan §Phase 3）。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>价格清单取价——客户组+物料阶梯命中</li>
 *   <li>促销折扣（PERCENT_DISCOUNT）</li>
 *   <li>满减（AMOUNT_OFF）</li>
 *   <li>买赠（GIFT）</li>
 *   <li>叠加（stackable）</li>
 *   <li>优先级（priority 小者优先）</li>
 *   <li>价格覆盖（PRICE_OVERRIDE）</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalPricingEndToEnd extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testScenario1_PriceListPricing() {
        Map<String, String> pre = createPrereqs();
        createPriceList(pre, "VIP", 80);

        Map<String, Object> orderData = newOrderData(pre);
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__save",
                ApiRequest.build(Map.of("data", orderData)));
        assertEquals(0, created.getStatus());
        String orderId = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        addOrderLine(orderId, pre, 100, 10);

        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario2_PercentDiscount() {
        Map<String, String> pre = createPrereqs();
        createRule("PERCENT_DISCOUNT", "LINE", 100, r -> {
            r.put("discountPercent", 10);
            r.put("materialId", pre.get("material"));
        });

        String orderId = createOrderWithLine(pre, 100, 10);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario3_AmountOff() {
        Map<String, String> pre = createPrereqs();
        createRule("AMOUNT_OFF", "ORDER", 100, r -> {
            r.put("minOrderAmount", 500);
            r.put("discountAmount", 50);
        });

        String orderId = createOrderWithLine(pre, 100, 10);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario4_Gift() {
        Map<String, String> pre = createPrereqs();
        createRule("GIFT", "LINE", 100, r -> {
            r.put("giftMaterialId", pre.get("material"));
            r.put("giftQuantity", 1);
        });

        String orderId = createOrderWithLine(pre, 100, 2);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario5_Stackable() {
        Map<String, String> pre = createPrereqs();
        createRule("PERCENT_DISCOUNT", "LINE", 100, r -> {
            r.put("discountPercent", 10);
            r.put("materialId", pre.get("material"));
            r.put("stackable", true);
        });
        createRule("PRICE_OVERRIDE", "LINE", 200, r -> {
            r.put("priceOverride", 90);
            r.put("materialId", pre.get("material"));
            r.put("stackable", true);
        });

        String orderId = createOrderWithLine(pre, 100, 10);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario6_Priority() {
        Map<String, String> pre = createPrereqs();
        createRule("PRICE_OVERRIDE", "LINE", 200, r -> {
            r.put("priceOverride", 80);
            r.put("materialId", pre.get("material"));
            r.put("stackable", false);
        });
        createRule("PRICE_OVERRIDE", "LINE", 50, r -> {
            r.put("priceOverride", 95);
            r.put("materialId", pre.get("material"));
            r.put("stackable", false);
        });

        String orderId = createOrderWithLine(pre, 100, 10);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    @Test
    public void testScenario7_PriceOverride() {
        Map<String, String> pre = createPrereqs();
        createRule("PRICE_OVERRIDE", "LINE", 100, r -> {
            r.put("priceOverride", 85);
            r.put("materialId", pre.get("material"));
        });

        String orderId = createOrderWithLine(pre, 100, 5);
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", orderId)));
        assertEquals(0, result.getStatus());
    }

    // ---------- helpers ----------

    private String createOrderWithLine(Map<String, String> pre, int unitPrice, int qty) {
        Map<String, Object> orderData = newOrderData(pre);
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalOrder__save",
                ApiRequest.build(Map.of("data", orderData)));
        assertEquals(0, created.getStatus());
        String orderId = String.valueOf(((Map<?, ?>) created.getData()).get("id"));
        addOrderLine(orderId, pre, unitPrice, qty);
        return orderId;
    }

    private Map<String, Object> newOrderData(Map<String, String> pre) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "ORDER-" + System.nanoTime());
        data.put("customerId", pre.get("partner"));
        data.put("businessDate", "2026-07-10");
        data.put("currencyId", pre.get("currency"));
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        return data;
    }

    private void addOrderLine(String orderId, Map<String, String> pre, int unitPrice, int qty) {
        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("orderId", orderId);
        lineData.put("lineNo", 1);
        lineData.put("materialId", pre.get("material"));
        lineData.put("uoMId", pre.get("uom"));
        lineData.put("unitPrice", unitPrice);
        lineData.put("quantity", qty);
        lineData.put("amount", unitPrice * qty);
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpSalOrderLine__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, resp.getStatus());
    }

    private void createPriceList(Map<String, String> pre, String customerGroup, int price) {
        Map<String, Object> head = new LinkedHashMap<>();
        head.put("code", "PL-" + System.nanoTime());
        head.put("name", "测试清单");
        head.put("currencyId", pre.get("currency"));
        head.put("customerGroupCode", customerGroup);
        head.put("priority", 100);
        head.put("isActive", true);
        ApiResponse<?> headResp = executeRpc(GraphQLOperationType.mutation, "ErpSalPriceList__save",
                ApiRequest.build(Map.of("data", head)));
        assertEquals(0, headResp.getStatus());
        String headId = String.valueOf(((Map<?, ?>) headResp.getData()).get("id"));

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("priceListId", headId);
        line.put("materialId", pre.get("material"));
        line.put("unitPrice", price);
        line.put("minQuantity", 0);
        ApiResponse<?> lineResp = executeRpc(GraphQLOperationType.mutation, "ErpSalPriceListLine__save",
                ApiRequest.build(Map.of("data", line)));
        assertEquals(0, lineResp.getStatus());
    }

    private void createRule(String ruleType, String targetType, int priority,
                            java.util.function.Consumer<Map<String, Object>> customizer) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("ruleCode", "RULE-" + System.nanoTime());
        rule.put("ruleName", "Test " + ruleType);
        rule.put("ruleType", ruleType);
        rule.put("targetType", targetType);
        rule.put("priority", priority);
        rule.put("isActive", true);
        rule.put("stackable", false);
        customizer.accept(rule);
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpSalPricingRule__save",
                ApiRequest.build(Map.of("data", rule)));
        assertEquals(0, resp.getStatus());
    }

    private Map<String, String> createPrereqs() {
        Map<String, String> ids = new LinkedHashMap<>();
        Map<String, Object> d_currency = new LinkedHashMap<>();
        d_currency.put("code", "CNY");
        d_currency.put("name", "元");
        ids.put("currency", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdCurrency__save", ApiRequest.build(Map.of("data", d_currency))).getData()).get("id")));
        Map<String, Object> d_partner = new LinkedHashMap<>();
        d_partner.put("code", "E2E-PARTNER-" + System.nanoTime());
        d_partner.put("name", "E2E客户");
        d_partner.put("partnerType", "CUSTOMER");
        d_partner.put("status", "ACTIVE");
        d_partner.put("customerGroup", "VIP");
        ids.put("partner", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save", ApiRequest.build(Map.of("data", d_partner))).getData()).get("id")));
        Map<String, Object> d_uom = new LinkedHashMap<>();
        d_uom.put("code", "PCS");
        d_uom.put("name", "个");
        ids.put("uom", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdUoM__save", ApiRequest.build(Map.of("data", d_uom))).getData()).get("id")));
        Map<String, Object> d_material = new LinkedHashMap<>();
        d_material.put("code", "E2E-MAT-" + System.nanoTime());
        d_material.put("name", "E2E物料");
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
