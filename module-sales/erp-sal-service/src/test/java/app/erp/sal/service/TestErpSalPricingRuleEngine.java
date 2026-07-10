package app.erp.sal.service;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPricingRule;
import app.erp.sal.service.support.ErpSalPricingRuleEngine;

import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售促销规则引擎单元测试（UC-SAL-11 定价引擎 §Phase 2）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：PERCENT_DISCOUNT 行级、AMOUNT_OFF 头级满减、GIFT 买赠、PRICE_OVERRIDE 覆盖、
 * stackable 叠加、非叠加排他、期间外不匹配、优先级。
 */
public class TestErpSalPricingRuleEngine extends BaseTestCase {

    private final ErpSalPricingRuleEngine engine = new ErpSalPricingRuleEngine();

    private static final Long MATERIAL_ID = 7001L;
    private static final Long CUSTOMER_ID = 8001L;
    private static final Long CURRENCY_ID = 6401L;

    @Test
    public void testPercentDiscountLine() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule rule = newRule("PERCENT_DISCOUNT", "LINE", 100);
        rule.setMaterialId(MATERIAL_ID);
        rule.setDiscountPercent(new BigDecimal("10"));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        assertEquals(1, result.getAppliedRules().size());
        ErpSalOrderLine modified = result.getModifiedLines().get(0);
        assertEquals(0, modified.getDiscountRate().compareTo(new BigDecimal("10")));
        assertEquals(0, modified.getDiscountAmount().compareTo(new BigDecimal("100")));
        assertEquals("PROMOTION", modified.getPricingSource());
    }

    @Test
    public void testAmountOffOrderMeetsThreshold() {
        ErpSalOrder order = newOrder();
        order.setTotalAmount(new BigDecimal("2000"));
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("2000"), new BigDecimal("1")));

        ErpSalPricingRule rule = newRule("AMOUNT_OFF", "ORDER", 100);
        rule.setMinOrderAmount(new BigDecimal("1000"));
        rule.setDiscountAmount(new BigDecimal("200"));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        assertEquals(1, result.getAppliedRules().size());
        assertEquals(0, result.getOrderDiscountAmount().compareTo(new BigDecimal("200")));
    }

    @Test
    public void testAmountOffOrderBelowThreshold() {
        ErpSalOrder order = newOrder();
        order.setTotalAmount(new BigDecimal("500"));
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("500"), new BigDecimal("1")));

        ErpSalPricingRule rule = newRule("AMOUNT_OFF", "ORDER", 100);
        rule.setMinOrderAmount(new BigDecimal("1000"));
        rule.setDiscountAmount(new BigDecimal("200"));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        // 规则被匹配但满减不生效（未达门槛），但仍计入 appliedRules（因 targetType=ORDER 已评估）
        assertEquals(0, result.getOrderDiscountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testGiftLine() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("2")));

        ErpSalPricingRule rule = newRule("GIFT", "LINE", 100);
        rule.setGiftMaterialId(MATERIAL_ID);
        rule.setGiftQuantity(new BigDecimal("1"));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        assertEquals(2, result.getModifiedLines().size());
        ErpSalOrderLine gift = result.getModifiedLines().get(1);
        assertEquals(0, gift.getUnitPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, gift.getQuantity().compareTo(BigDecimal.ONE));
        assertEquals("PROMOTION", gift.getPricingSource());
        assertTrue(result.getGiftRuleIds().contains(rule.getId()));
    }

    @Test
    public void testPriceOverrideLine() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("5")));

        ErpSalPricingRule rule = newRule("PRICE_OVERRIDE", "LINE", 100);
        rule.setMaterialId(MATERIAL_ID);
        rule.setPriceOverride(new BigDecimal("85"));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        ErpSalOrderLine modified = result.getModifiedLines().get(0);
        assertEquals(0, modified.getUnitPrice().compareTo(new BigDecimal("85")));
        // gross 500, new gross 425, discount = 75
        assertEquals(0, modified.getDiscountAmount().compareTo(new BigDecimal("75")));
        assertEquals("PROMOTION", modified.getPricingSource());
    }

    @Test
    public void testStackableRulesBothApplied() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule rule1 = newRule("PERCENT_DISCOUNT", "LINE", 100);
        rule1.setMaterialId(MATERIAL_ID);
        rule1.setDiscountPercent(new BigDecimal("10"));
        rule1.setStackable(true);

        ErpSalPricingRule rule2 = newRule("PRICE_OVERRIDE", "LINE", 200);
        rule2.setMaterialId(MATERIAL_ID);
        rule2.setPriceOverride(new BigDecimal("90"));
        rule2.setStackable(true);

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule1, rule2));

        assertEquals(2, result.getAppliedRules().size());
    }

    @Test
    public void testNonStackableOnlyFirstApplied() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule rule1 = newRule("PERCENT_DISCOUNT", "LINE", 100);
        rule1.setMaterialId(MATERIAL_ID);
        rule1.setDiscountPercent(new BigDecimal("10"));
        rule1.setStackable(false);

        ErpSalPricingRule rule2 = newRule("PRICE_OVERRIDE", "LINE", 200);
        rule2.setMaterialId(MATERIAL_ID);
        rule2.setPriceOverride(new BigDecimal("90"));
        rule2.setStackable(false);

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule1, rule2));

        assertEquals(1, result.getAppliedRules().size());
        assertEquals("PERCENT_DISCOUNT", result.getAppliedRules().get(0).getRuleType());
    }

    @Test
    public void testPriorityLowerWins() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule high = newRule("PRICE_OVERRIDE", "LINE", 200);
        high.setMaterialId(MATERIAL_ID);
        high.setPriceOverride(new BigDecimal("80"));
        high.setStackable(false);

        ErpSalPricingRule low = newRule("PRICE_OVERRIDE", "LINE", 50);
        low.setMaterialId(MATERIAL_ID);
        low.setPriceOverride(new BigDecimal("90"));
        low.setStackable(false);

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(high, low));

        assertEquals(1, result.getAppliedRules().size());
        assertEquals(50, result.getAppliedRules().get(0).getPriority());
        // low priority=50 wins → price=90
        assertEquals(0, result.getModifiedLines().get(0).getUnitPrice().compareTo(new BigDecimal("90")));
    }

    @Test
    public void testPeriodOutsideNoMatch() {
        ErpSalOrder order = newOrder();
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule rule = newRule("PERCENT_DISCOUNT", "LINE", 100);
        rule.setMaterialId(MATERIAL_ID);
        rule.setDiscountPercent(new BigDecimal("10"));
        // expired in the past
        rule.setValidTo(java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(2020, 1, 1, 0, 0)));

        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, null, List.of(rule));

        assertTrue(result.getAppliedRules().isEmpty());
    }

    @Test
    public void testCustomerGroupMatch() {
        ErpSalOrder order = newOrder();
        order.setCustomerId(CUSTOMER_ID);
        List<ErpSalOrderLine> lines = new ArrayList<>();
        lines.add(newLine(MATERIAL_ID, new BigDecimal("100"), new BigDecimal("10")));

        ErpSalPricingRule rule = newRule("PERCENT_DISCOUNT", "LINE", 100);
        rule.setMaterialId(MATERIAL_ID);
        rule.setDiscountPercent(new BigDecimal("15"));
        rule.setCustomerGroupCode("VIP");

        // customerGroup="VIP" matches
        ErpSalPricingRuleEngine.EvaluationResult result =
                engine.evaluate(order, lines, "VIP", List.of(rule));
        assertEquals(1, result.getAppliedRules().size());

        // customerGroup="OTHER" does not match
        ErpSalPricingRuleEngine.EvaluationResult result2 =
                engine.evaluate(order, lines, "OTHER", List.of(rule));
        assertTrue(result2.getAppliedRules().isEmpty());
    }

    // ---------- helpers ----------

    private ErpSalOrder newOrder() {
        ErpSalOrder order = new ErpSalOrder();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCurrencyId(CURRENCY_ID);
        return order;
    }

    private ErpSalOrderLine newLine(Long materialId, BigDecimal unitPrice, BigDecimal quantity) {
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setId(System.nanoTime());
        line.setOrderId(1L);
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setUnitPrice(unitPrice);
        line.setQuantity(quantity);
        line.setAmount(unitPrice.multiply(quantity));
        return line;
    }

    private ErpSalPricingRule newRule(String ruleType, String targetType, int priority) {
        ErpSalPricingRule rule = new ErpSalPricingRule();
        rule.setId(System.nanoTime());
        rule.setRuleCode("RULE-" + ruleType + "-" + priority);
        rule.setRuleName("Test " + ruleType);
        rule.setRuleType(ruleType);
        rule.setTargetType(targetType);
        rule.setPriority(priority);
        rule.setIsActive(true);
        rule.setStackable(false);
        return rule;
    }
}
