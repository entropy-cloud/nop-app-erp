package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmConfigRule;
import app.erp.crm.service.support.ProductConfigRuleEngine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPQ 配置规则引擎单元测试（plan 2026-07-07-1430-2 §Phase 3）。
 *
 * <p>{@link ProductConfigRuleEngine#evalCondition} 走 XLang 表达式评估，
 * 需 {@link JunitAutoTestCase} 启动 CoreInitialization（与 {@code TestErpCrmBundlePricingCrudSmoke} 一致）。
 *
 * <p>覆盖：四 ruleType 标记（REQUIRED/EXCLUDED/RECOMMENDED/OPTIONAL）、conditionExpression 优先、
 * 规则数上限（与 BizModel 钩子的集成由 {@code TestErpCrmCpqGenerateQuote} 覆盖）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestProductConfigRuleEngine extends JunitAutoTestCase {

    private final ProductConfigRuleEngine engine = new ProductConfigRuleEngine();

    @Test
    public void testRequiredRule() {
        ErpCrmConfigRule rule = newConfigRule(null, "REQUIRED", "CPU_TYPE", "INTEL_XEON", "HEATSINK", "HEAVY_DUTY", 10);
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(rule));
        assertEquals(1, result.size(), "命中规则应标记一个目标特征");
        assertEquals("REQUIRED", result.get("HEATSINK").getMark(), "目标特征标记为 REQUIRED");
        assertEquals("HEAVY_DUTY", result.get("HEATSINK").getFeatureValue(), "目标特征值被记录");
    }

    @Test
    public void testExcludedOverridesRecommended() {
        ErpCrmConfigRule recommended = newConfigRule(null, "RECOMMENDED", "CPU_TYPE", "INTEL_XEON",
                "GPU", "HIGH_END", 10);
        ErpCrmConfigRule excluded = newConfigRule(null, "EXCLUDED", "CPU_TYPE", "INTEL_XEON",
                "GPU", null, 20);
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(recommended, excluded));
        assertEquals("EXCLUDED", result.get("GPU").getMark(),
                "EXCLUDED 应覆盖 RECOMMENDED（禁用优先）");
    }

    @Test
    public void testExcludedNotOverriddenByLaterRecommended() {
        ErpCrmConfigRule excluded = newConfigRule(null, "EXCLUDED", "CPU_TYPE", "INTEL_XEON",
                "GPU", null, 10);
        ErpCrmConfigRule recommended = newConfigRule(null, "RECOMMENDED", "CPU_TYPE", "INTEL_XEON",
                "GPU", "HIGH_END", 20);
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(excluded, recommended));
        assertEquals("EXCLUDED", result.get("GPU").getMark(),
                "EXCLUDED 后续不被 RECOMMENDED 覆盖");
    }

    @Test
    public void testSourceMismatchNoMatch() {
        ErpCrmConfigRule rule = newConfigRule(null, "REQUIRED", "CPU_TYPE", "INTEL_XEON",
                "HEATSINK", "HEAVY_DUTY", 10);
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "AMD_EPYC"); // 不匹配 INTEL_XEON
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(rule));
        assertTrue(result.isEmpty(), "sourceFeatureValue 不匹配则不标记");
    }

    @Test
    public void testConditionExpressionPriority() {
        // conditionExpression 优先于单行 source 匹配
        ErpCrmConfigRule rule = newConfigRule(null, "REQUIRED", "CPU_TYPE", "AMD_EPYC",
                "HEATSINK", "HEAVY_DUTY", 10);
        rule.setConditionExpression("selectedFeatures.CPU_TYPE == 'INTEL_XEON'");
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(rule));
        // 单行 source 不匹配，但 conditionExpression 匹配 → 命中
        assertEquals(1, result.size(), "conditionExpression 匹配应命中（优先于 source 单行）");
        assertEquals("REQUIRED", result.get("HEATSINK").getMark());
    }

    @Test
    public void testConditionExpressionFalse() {
        ErpCrmConfigRule rule = newConfigRule(null, "REQUIRED", null, null,
                "HEATSINK", "HEAVY_DUTY", 10);
        rule.setConditionExpression("selectedFeatures.MEMORY == '128GB'");
        Map<String, String> features = new HashMap<>();
        features.put("MEMORY", "64GB"); // 不满足条件
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(rule));
        assertTrue(result.isEmpty(), "conditionExpression 不匹配则不标记");
    }

    @Test
    public void testSequenceOrdering() {
        // 多规则按 sequence 排序，同目标特征累加触发规则
        ErpCrmConfigRule r1 = newConfigRule(null, "RECOMMENDED", "CPU_TYPE", "INTEL_XEON",
                "COOLER", "STOCK", 30);
        ErpCrmConfigRule r2 = newConfigRule(null, "REQUIRED", "CPU_TYPE", "INTEL_XEON",
                "POWER_SUPPLY", "800W", 10);
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of(r1, r2));
        assertEquals(2, result.size(), "两规则各自标记目标");
        assertTrue(result.containsKey("COOLER") && result.containsKey("POWER_SUPPLY"),
                "两个目标特征均被标记");
    }

    @Test
    public void testEmptyRulesNoMatch() {
        Map<String, String> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(features, List.of());
        assertTrue(result.isEmpty(), "无规则输入返回空");
    }

    @Test
    public void testNullFeaturesSafe() {
        ErpCrmConfigRule rule = newConfigRule(null, "REQUIRED", "CPU_TYPE", "INTEL_XEON",
                "HEATSINK", "HEAVY_DUTY", 10);
        Map<String, ProductConfigRuleEngine.EvaluationResult> result =
                engine.evaluate(null, List.of(rule));
        assertTrue(result.isEmpty(), "selectedFeatures 为空则 source 不匹配");
        assertFalse(engine.evaluate(new HashMap<>(), null).size() > 0, "rules 为 null 安全返回");
    }

    private ErpCrmConfigRule newConfigRule(Long id, String ruleType, String sourceCode, String sourceValue,
                                           String targetCode, String targetValue, int sequence) {
        ErpCrmConfigRule rule = new ErpCrmConfigRule();
        rule.setId(id);
        rule.setRuleType(ruleType);
        rule.setSourceFeatureCode(sourceCode);
        rule.setSourceFeatureValue(sourceValue);
        rule.setTargetFeatureCode(targetCode);
        rule.setTargetFeatureValue(targetValue);
        rule.setSequence(sequence);
        return rule;
    }
}
