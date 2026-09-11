package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmPriceRule;
import app.erp.crm.service.support.PriceRuleEngine;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPQ 价格规则引擎单元测试（plan 2026-07-07-1430-2 §Phase 3）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：三 ruleType 优先级（CUSTOMER_SPECIFIC > PROMOTIONAL > VOLUME）、priority 平局（数值小者优先）、
 * 期间失效、数量区间边界、无匹配回退标准定价（basePrice）。
 */
public class TestPriceRuleEngine extends BaseTestCase {

    private final PriceRuleEngine engine = new PriceRuleEngine();

    private static final String PRODUCT_ID = "7001";
    private static final String CUSTOMER_ID = "8001";
    private static final String CURRENCY_ID = "6401";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Test
    public void testRuleTypePriorityCustomerSpecificWins() {
        ErpCrmPriceRule volume = newRule("VOLUME", 10);
        volume.setProductId(PRODUCT_ID);
        volume.setMinQuantity(BigDecimal.valueOf(1));
        volume.setMaxQuantity(BigDecimal.valueOf(100));
        volume.setPriceOverride(BigDecimal.valueOf(900));

        ErpCrmPriceRule promo = newRule("PROMOTIONAL", 10);
        promo.setProductId(PRODUCT_ID);
        promo.setDiscountPercent(BigDecimal.valueOf(10.0));

        ErpCrmPriceRule customerSpecific = newRule("CUSTOMER_SPECIFIC", 10);
        customerSpecific.setProductId(PRODUCT_ID);
        customerSpecific.setCustomerId(CUSTOMER_ID);
        customerSpecific.setPriceOverride(BigDecimal.valueOf(800));

        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, CUSTOMER_ID,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000),
                List.of(volume, promo, customerSpecific));
        assertTrue(result.isMatched(), "应命中 CUSTOMER_SPECIFIC");
        assertEquals("CUSTOMER_SPECIFIC", result.getMatchedRule().getRuleType());
        assertEquals(0, result.getFinalPrice().compareTo(BigDecimal.valueOf(800)),
                "priceOverride=800 直接覆盖");
    }

    @Test
    public void testPriorityTieBreakerLowerWins() {
        ErpCrmPriceRule r1 = newRule("VOLUME", 5);
        r1.setProductId(PRODUCT_ID);
        r1.setMinQuantity(BigDecimal.valueOf(1));
        r1.setMaxQuantity(BigDecimal.valueOf(100));
        r1.setPriceOverride(BigDecimal.valueOf(880));

        ErpCrmPriceRule r2 = newRule("VOLUME", 1);
        r2.setProductId(PRODUCT_ID);
        r2.setMinQuantity(BigDecimal.valueOf(1));
        r2.setMaxQuantity(BigDecimal.valueOf(100));
        r2.setPriceOverride(BigDecimal.valueOf(850));

        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r1, r2));
        assertTrue(result.isMatched());
        assertEquals(0, result.getFinalPrice().compareTo(BigDecimal.valueOf(850)),
                "priority=1（小者）优先于 priority=5");
    }

    @Test
    public void testPeriodExpired() {
        ErpCrmPriceRule r = newRule("PROMOTIONAL", 1);
        r.setProductId(PRODUCT_ID);
        r.setDiscountPercent(BigDecimal.valueOf(20.0));
        r.setEffectiveFrom(TODAY.minusDays(10));
        r.setEffectiveTo(TODAY.minusDays(1));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertFalse(result.isMatched(), "已过期规则不匹配");
    }

    @Test
    public void testPeriodActive() {
        ErpCrmPriceRule r = newRule("PROMOTIONAL", 1);
        r.setProductId(PRODUCT_ID);
        r.setDiscountPercent(BigDecimal.valueOf(20.0));
        r.setEffectiveFrom(TODAY.minusDays(1));
        r.setEffectiveTo(TODAY.plusDays(1));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertTrue(result.isMatched(), "在生效期间内匹配");
        assertEquals(0, result.getFinalPrice().compareTo(new BigDecimal("800.0")),
                "1000 × (1 - 20/100) = 800");
    }

    @Test
    public void testQuantityRangeBoundary() {
        ErpCrmPriceRule r = newRule("VOLUME", 1);
        r.setProductId(PRODUCT_ID);
        r.setMinQuantity(BigDecimal.valueOf(5));
        r.setMaxQuantity(BigDecimal.valueOf(10));
        r.setPriceOverride(BigDecimal.valueOf(900));
        // 数量 = minQuantity (5) → 命中
        assertTrue(engine.resolvePrice(PRODUCT_ID, null, BigDecimal.valueOf(5), CURRENCY_ID, TODAY,
                BigDecimal.valueOf(1000), List.of(r)).isMatched(), "数量=下边界 命中");
        // 数量 = maxQuantity (10) → 命中
        assertTrue(engine.resolvePrice(PRODUCT_ID, null, BigDecimal.valueOf(10), CURRENCY_ID, TODAY,
                BigDecimal.valueOf(1000), List.of(r)).isMatched(), "数量=上边界 命中");
        // 数量 < minQuantity (4) → 不命中
        assertFalse(engine.resolvePrice(PRODUCT_ID, null, BigDecimal.valueOf(4), CURRENCY_ID, TODAY,
                BigDecimal.valueOf(1000), List.of(r)).isMatched(), "数量<下边界 不命中");
        // 数量 > maxQuantity (11) → 不命中
        assertFalse(engine.resolvePrice(PRODUCT_ID, null, BigDecimal.valueOf(11), CURRENCY_ID, TODAY,
                BigDecimal.valueOf(1000), List.of(r)).isMatched(), "数量>上边界 不命中");
    }

    @Test
    public void testNoMatchFallbackBasePrice() {
        // 无任何规则匹配时，调用方根据 isMatched=false 自行回退 basePrice
        ErpCrmPriceRule r = newRule("CUSTOMER_SPECIFIC", 1);
        r.setProductId(PRODUCT_ID);
        r.setCustomerId("9999");
        r.setPriceOverride(BigDecimal.valueOf(800));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, CUSTOMER_ID,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertFalse(result.isMatched(), "customerId 不匹配 → 不命中");
    }

    // ===================== P1-CK-crm2-002：productCategory 维度（plan 2026-09-11-2350-1 Phase 1） =====================

    @Test
    public void testCategoryScopedRuleNotMatchWithoutCategoryContext() {
        // 缺陷复现（P1-CK-crm2-002，7 参旧签名=无类别上下文）：类别限定规则（productCategory=SERVER、
        // productId 空）当前 ruleMatchesProduct 对 productId 空恒真 → 退化为全局规则误命中。
        // 修复后：规则类别非空而上下文类别缺失 → fail-closed 不命中。
        ErpCrmPriceRule categoryRule = newRule("PROMOTIONAL", 1);
        categoryRule.setProductCategory("SERVER");
        categoryRule.setPriceOverride(BigDecimal.valueOf(700));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(categoryRule));
        assertFalse(result.isMatched(), "类别限定规则在无类别上下文时不应命中（退化为全局=缺陷）");
    }

    @Test
    public void testCategoryScopedRuleMatchesOnlySameCategory() {
        // 类别限定规则（productId 空、productCategory=SERVER）不应退化为全局规则
        ErpCrmPriceRule categoryRule = newRule("PROMOTIONAL", 1);
        categoryRule.setProductCategory("SERVER");
        categoryRule.setPriceOverride(BigDecimal.valueOf(700));

        // 同类别 → 命中
        PriceRuleEngine.PriceResult hit = engine.resolvePrice(PRODUCT_ID, null, "SERVER",
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(categoryRule));
        assertTrue(hit.isMatched(), "同类别产品应命中类别限定规则");
        // 不同类别 → 不命中（修复前恒命中=退化为全局）
        PriceRuleEngine.PriceResult miss = engine.resolvePrice(PRODUCT_ID, null, "LAPTOP",
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(categoryRule));
        assertFalse(miss.isMatched(), "不同类别产品不应命中类别限定规则");
        // 上下文无类别信息 → 不命中（fail-closed，防止类别限定规则误伤未知类别产品）
        PriceRuleEngine.PriceResult noCtx = engine.resolvePrice(PRODUCT_ID, null, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(categoryRule));
        assertFalse(noCtx.isMatched(), "上下文无类别时类别限定规则不命中");
    }

    @Test
    public void testGlobalRuleUnaffectedByCategoryDimension() {
        // 双空规则（productId/productCategory 均空）仍为全局规则
        ErpCrmPriceRule global = newRule("PROMOTIONAL", 1);
        global.setPriceOverride(BigDecimal.valueOf(850));
        assertTrue(engine.resolvePrice(PRODUCT_ID, null, "SERVER",
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(global)).isMatched(),
                "全局规则与类别维度正交");
        assertTrue(engine.resolvePrice(PRODUCT_ID, null, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(global)).isMatched(),
                "全局规则在无类别上下文时仍命中");
    }

    @Test
    public void testProductAndCategoryBothScopedRequireBothMatch() {
        // productId 与 productCategory 同时限定 → 双条件都须满足
        ErpCrmPriceRule both = newRule("PROMOTIONAL", 1);
        both.setProductId(PRODUCT_ID);
        both.setProductCategory("SERVER");
        both.setPriceOverride(BigDecimal.valueOf(600));
        assertTrue(engine.resolvePrice(PRODUCT_ID, null, "SERVER",
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(both)).isMatched(),
                "产品与类别均匹配 → 命中");
        assertFalse(engine.resolvePrice(PRODUCT_ID, null, "LAPTOP",
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(both)).isMatched(),
                "产品匹配但类别不符 → 不命中");
    }

    @Test
    public void testDiscountPercentApplied() {
        ErpCrmPriceRule r = newRule("PROMOTIONAL", 1);
        r.setProductId(PRODUCT_ID);
        r.setDiscountPercent(BigDecimal.valueOf(15.0));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertTrue(result.isMatched());
        assertEquals(0, result.getFinalPrice().compareTo(new BigDecimal("850.0")),
                "1000 × (1 - 15/100) = 850");
    }

    @Test
    public void testDiscountAmountApplied() {
        ErpCrmPriceRule r = newRule("VOLUME", 1);
        r.setProductId(PRODUCT_ID);
        r.setMinQuantity(BigDecimal.valueOf(1));
        r.setMaxQuantity(BigDecimal.valueOf(100));
        r.setDiscountAmount(BigDecimal.valueOf(100));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertTrue(result.isMatched());
        assertEquals(0, result.getFinalPrice().compareTo(BigDecimal.valueOf(900)),
                "1000 - 100 = 900");
    }

    @Test
    public void testCurrencyMismatch() {
        ErpCrmPriceRule r = newRule("PROMOTIONAL", 1);
        r.setProductId(PRODUCT_ID);
        r.setPriceOverride(BigDecimal.valueOf(900));
        r.setCurrencyId("9999");
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, null,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertFalse(result.isMatched(), "currencyId 不匹配 → 不命中");
    }

    private ErpCrmPriceRule newRule(String ruleType, int priority) {
        ErpCrmPriceRule rule = new ErpCrmPriceRule();
        rule.setRuleType(ruleType);
        rule.setPriority(priority);
        return rule;
    }
}
