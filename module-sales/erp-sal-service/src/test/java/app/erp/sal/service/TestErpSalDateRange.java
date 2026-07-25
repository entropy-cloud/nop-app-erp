package app.erp.sal.service;

import app.erp.sal.dao.entity.ErpSalPriceList;
import app.erp.sal.dao.entity.ErpSalPriceListLine;
import app.erp.sal.dao.entity.ErpSalPricingRule;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * C3 日期范围有效性模式 sales 定价 3 实体集成测试
 * （plan 2026-07-26-0315-1 Phase 2，docs/design/date-ranged-validity-pattern.md §7）。
 *
 * <p>覆盖 3 实体的 C3 日期范围校验（经 GraphQL {@code __save} 路径触发 {@code defaultPrepareSave} 钩子）：
 * <ol>
 *   <li>{@code ErpSalPriceList}（PRIORITY 策略）—— 同 customerGroupCode + partnerId 维度允许重叠（仅 warn），不阻断保存</li>
 *   <li>{@code ErpSalPriceListLine}（MUTEX 策略）—— 同 priceListId + materialId 维度区间重叠拒绝</li>
 *   <li>{@code ErpSalPricingRule}（STACKABLE 混合策略）—— 双非 stackable 重叠拒绝；任一方 stackable=true 允许</li>
 * </ol>
 *
 * <p>每个实体覆盖：正路径（不重叠/允许重叠通过）+ 负路径（重叠拒绝）+ 边界（相邻日通过）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDateRange extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ErpSalPriceListLine (MUTEX) ----------

    @Test
    public void priceListLine_overlapSameDimensionRejected() {
        Map<String, String> pre = seedPriceListPrereqs();
        String headId = savePriceListOk(pre, "PL-LINE-1", "VIP", null);

        // 第一行 [2026-01-01..2026-06-30]
        savePriceListLineOk(headId, pre.get("material"), "2026-01-01", "2026-06-30");

        // 第二行同 priceListId + materialId 重叠 [2026-04-01..2026-12-31] → 拒绝
        ApiResponse<?> resp = savePriceListLine(headId, pre.get("material"), "2026-04-01", "2026-12-31");
        assertNotEquals(0, resp.getStatus(),
                "同 priceListId+materialId 重叠应被拒绝（status=" + resp.getStatus() + "）");
        assertEquals(ErpSalErrors.ERR_SAL_PRICE_LIST_LINE_OVERLAP.getErrorCode(), resp.getCode(),
                "错误码应为 ERR_SAL_PRICE_LIST_LINE_OVERLAP");
    }

    @Test
    public void priceListLine_adjacentDayPasses() {
        Map<String, String> pre = seedPriceListPrereqs();
        String headId = savePriceListOk(pre, "PL-LINE-2", "VIP", null);

        // 第一行 [2026-01-01..2026-06-30]，第二行相邻 [2026-07-01..2026-12-31] → 通过
        savePriceListLineOk(headId, pre.get("material"), "2026-01-01", "2026-06-30");
        savePriceListLineOk(headId, pre.get("material"), "2026-07-01", "2026-12-31");
    }

    @Test
    public void priceListLine_differentMaterialOverlapPasses() {
        Map<String, String> pre = seedPriceListPrereqs();
        String headId = savePriceListOk(pre, "PL-LINE-3", "VIP", null);
        // 不同 materialId 同区间 → 通过（不同维度）
        savePriceListLineOk(headId, pre.get("material"), "2026-01-01", "2026-12-31");
        String otherMaterial = seedMaterial("C3-OTHER-MAT");
        savePriceListLineOk(headId, otherMaterial, "2026-01-01", "2026-12-31");
    }

    // ---------- ErpSalPriceList (PRIORITY warn-only) ----------

    @Test
    public void priceList_priorityOverlapAllowed() {
        // PRIORITY 策略：同维度允许重叠（仅 warn-on-ambiguity）—— 不应阻断保存
        Map<String, String> pre = seedPriceListPrereqs();
        // 第一清单 customerGroupCode=VIP, partnerId=null, priority=50
        savePriceListOk(pre, "PL-PRIORITY-1", "VIP", null, 50,
                "2026-01-01", "2026-12-31");
        // 第二清单同维度同优先级，区间重叠 → 应通过（PRIORITY 允许重叠；warn 由日志输出，不抛异常）
        Map<String, Object> data = priceListPayload(pre, "PL-PRIORITY-2", "VIP", null, 50,
                "2026-03-01", "2026-09-30");
        ApiResponse<?> resp = rpc(mutation, "ErpSalPriceList__save", ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(),
                "PRIORITY 策略允许同维度重叠（warn-only），不应阻断保存（status=" + resp.getStatus()
                        + " code=" + resp.getCode() + "）");
    }

    @Test
    public void priceList_differentCustomerGroupOverlapPasses() {
        // 不同 customerGroupCode → 不同维度，区间重叠也通过
        Map<String, String> pre = seedPriceListPrereqs();
        savePriceListOk(pre, "PL-PRIORITY-3", "VIP", null, 100, "2026-01-01", "2026-12-31");
        savePriceListOk(pre, "PL-PRIORITY-4", "RETAIL", null, 100, "2026-01-01", "2026-12-31");
    }

    // ---------- ErpSalPricingRule (STACKABLE 混合策略) ----------

    @Test
    public void pricingRule_bothNonStackableOverlapRejected() {
        // 双方 stackable=false 且区间重叠 → 拒绝
        savePricingRuleOk("RULE-S-1", "PERCENT_DISCOUNT", "ORDER", false,
                100, "2026-01-01T00:00:00", "2026-06-30T23:59:59");
        ApiResponse<?> resp = savePricingRule("RULE-S-2", "PERCENT_DISCOUNT", "ORDER", false,
                200, "2026-04-01T00:00:00", "2026-12-31T23:59:59");
        assertNotEquals(0, resp.getStatus(),
                "双非 stackable 同维度重叠应被拒绝（status=" + resp.getStatus() + "）");
        assertEquals(ErpSalErrors.ERR_SAL_PRICING_RULE_OVERLAP.getErrorCode(), resp.getCode(),
                "错误码应为 ERR_SAL_PRICING_RULE_OVERLAP");
    }

    @Test
    public void pricingRule_candidateStackableAllowsOverlap() {
        // candidate stackable=true 与非 stackable 既有重叠 → 允许
        savePricingRuleOk("RULE-S-3", "AMOUNT_OFF", "ORDER", false,
                100, "2026-01-01T00:00:00", "2026-12-31T23:59:59");
        savePricingRuleOk("RULE-S-4", "AMOUNT_OFF", "ORDER", true,
                200, "2026-04-01T00:00:00", "2026-09-30T23:59:59");
    }

    @Test
    public void pricingRule_bothStackableAllowsOverlap() {
        // 双方 stackable=true 且重叠 → 允许（并行叠加）
        savePricingRuleOk("RULE-S-5", "GIFT", "ORDER", true,
                100, "2026-01-01T00:00:00", "2026-12-31T23:59:59");
        savePricingRuleOk("RULE-S-6", "GIFT", "ORDER", true,
                200, "2026-03-01T00:00:00", "2026-09-30T23:59:59");
    }

    @Test
    public void pricingRule_adjacentRangePasses() {
        // 双方 stackable=false 但相邻 → 通过
        savePricingRuleOk("RULE-S-7", "PERCENT_DISCOUNT", "ORDER", false,
                100, "2026-01-01T00:00:00", "2026-06-30T23:59:59");
        savePricingRuleOk("RULE-S-8", "PERCENT_DISCOUNT", "ORDER", false,
                200, "2026-07-01T00:00:00", "2026-12-31T23:59:59");
    }

    @Test
    public void pricingRule_differentRuleTypeOverlapPasses() {
        // 不同 ruleType → 不同维度，即使双非 stackable + 重叠也通过
        savePricingRuleOk("RULE-S-9", "PERCENT_DISCOUNT", "ORDER", false,
                100, "2026-01-01T00:00:00", "2026-12-31T23:59:59");
        savePricingRuleOk("RULE-S-10", "AMOUNT_OFF", "ORDER", false,
                200, "2026-01-01T00:00:00", "2026-12-31T23:59:59");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(opType, action, request));
    }

    private Map<String, String> seedPriceListPrereqs() {
        String suffix = Long.toString(System.nanoTime() % 100000L);
        Map<String, String> ids = new LinkedHashMap<>();
        ormTemplate.runInSession(() -> {
            app.erp.md.dao.entity.ErpMdCurrency c = new app.erp.md.dao.entity.ErpMdCurrency();
            c.setCode("C3C" + suffix);
            c.setName("C3 测试币种");
            c.setIsActive(true);
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class).saveEntity(c);
            ids.put("currency", String.valueOf(c.getId()));
        });
        ids.put("material", seedMaterial("C3M" + suffix));
        return ids;
    }

    private String seedMaterial(String code) {
        // 经 GraphQL save 物料（保证 notGenCode 引用一致）
        Map<String, Object> uom = new LinkedHashMap<>();
        uom.put("code", "PC" + code);
        uom.put("name", "个");
        ApiResponse<?> uomResp = rpc(mutation, "ErpMdUoM__save", ApiRequest.build(Map.of("data", uom)));
        String uomId = String.valueOf(((Map<?, ?>) uomResp.getData()).get("id"));

        Map<String, Object> mat = new LinkedHashMap<>();
        mat.put("code", code);
        mat.put("name", "C3-" + code);
        mat.put("materialType", "GOODS");
        mat.put("uoMId", uomId);
        mat.put("status", "ACTIVE");
        ApiResponse<?> matResp = rpc(mutation, "ErpMdMaterial__save", ApiRequest.build(Map.of("data", mat)));
        return String.valueOf(((Map<?, ?>) matResp.getData()).get("id"));
    }

    private Map<String, Object> priceListPayload(Map<String, String> pre, String code,
                                                   String customerGroup, Long partnerId,
                                                   int priority, String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("name", "C3-" + code);
        data.put("currencyId", pre.get("currency"));
        data.put("customerGroupCode", customerGroup);
        if (partnerId != null) {
            data.put("partnerId", partnerId);
        }
        data.put("priority", priority);
        data.put("isActive", true);
        data.put("validFrom", validFrom);
        if (validTo != null) {
            data.put("validTo", validTo);
        }
        return data;
    }

    private String savePriceListOk(Map<String, String> pre, String code, String customerGroup, Long partnerId) {
        return savePriceListOk(pre, code, customerGroup, partnerId, 100,
                "2026-01-01", "2026-12-31");
    }

    private String savePriceListOk(Map<String, String> pre, String code, String customerGroup, Long partnerId,
                                    int priority, String validFrom, String validTo) {
        ApiResponse<?> resp = rpc(mutation, "ErpSalPriceList__save",
                ApiRequest.build(Map.of("data", priceListPayload(pre, code, customerGroup, partnerId,
                        priority, validFrom, validTo))));
        assertEquals(0, resp.getStatus(),
                "priceList save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode()
                        + " body=" + resp.toString());
        return String.valueOf(((Map<?, ?>) resp.getData()).get("id"));
    }

    private ApiResponse<?> savePriceListLine(String headId, String materialId,
                                              String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("priceListId", headId);
        data.put("materialId", materialId);
        data.put("unitPrice", 88);
        data.put("minQuantity", 0);
        data.put("validFrom", validFrom);
        if (validTo != null) {
            data.put("validTo", validTo);
        }
        return rpc(mutation, "ErpSalPriceListLine__save", ApiRequest.build(Map.of("data", data)));
    }

    @SuppressWarnings("unchecked")
    private void savePriceListLineOk(String headId, String materialId,
                                      String validFrom, String validTo) {
        ApiResponse<?> resp = savePriceListLine(headId, materialId, validFrom, validTo);
        assertEquals(0, resp.getStatus(),
                "priceListLine save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode()
                        + " body=" + resp.toString());
    }

    private Map<String, Object> pricingRulePayload(String code, String ruleType, String targetType,
                                                     boolean stackable, int priority,
                                                     String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ruleCode", code);
        data.put("ruleName", "C3-" + code);
        data.put("ruleType", ruleType);
        data.put("targetType", targetType);
        data.put("discountPercent", ruleType.equals("PERCENT_DISCOUNT") ? 10 : null);
        data.put("priority", priority);
        data.put("stackable", stackable);
        data.put("isActive", true);
        data.put("validFrom", validFrom);
        if (validTo != null) {
            data.put("validTo", validTo);
        }
        return data;
    }

    private ApiResponse<?> savePricingRule(String code, String ruleType, String targetType,
                                            boolean stackable, int priority,
                                            String validFrom, String validTo) {
        return rpc(mutation, "ErpSalPricingRule__save",
                ApiRequest.build(Map.of("data", pricingRulePayload(code, ruleType, targetType,
                        stackable, priority, validFrom, validTo))));
    }

    private void savePricingRuleOk(String code, String ruleType, String targetType,
                                    boolean stackable, int priority,
                                    String validFrom, String validTo) {
        ApiResponse<?> resp = savePricingRule(code, ruleType, targetType, stackable, priority, validFrom, validTo);
        assertEquals(0, resp.getStatus(),
                "pricingRule save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode()
                        + " body=" + resp.toString());
    }
}
