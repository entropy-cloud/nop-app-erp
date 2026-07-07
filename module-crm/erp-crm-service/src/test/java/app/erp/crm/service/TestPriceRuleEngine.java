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

    private static final Long PRODUCT_ID = 7001L;
    private static final Long CUSTOMER_ID = 8001L;
    private static final Long CURRENCY_ID = 6401L;
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
        promo.setDiscountPercent(10.0);

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
        r.setDiscountPercent(20.0);
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
        r.setDiscountPercent(20.0);
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
        r.setCustomerId(9999L);
        r.setPriceOverride(BigDecimal.valueOf(800));
        PriceRuleEngine.PriceResult result = engine.resolvePrice(PRODUCT_ID, CUSTOMER_ID,
                BigDecimal.TEN, CURRENCY_ID, TODAY, BigDecimal.valueOf(1000), List.of(r));
        assertFalse(result.isMatched(), "customerId 不匹配 → 不命中");
    }

    @Test
    public void testDiscountPercentApplied() {
        ErpCrmPriceRule r = newRule("PROMOTIONAL", 1);
        r.setProductId(PRODUCT_ID);
        r.setDiscountPercent(15.0);
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
        r.setCurrencyId(9999L);
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
