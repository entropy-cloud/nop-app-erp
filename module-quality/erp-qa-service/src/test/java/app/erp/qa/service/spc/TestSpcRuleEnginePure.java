package app.erp.qa.service.spc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 判异规则引擎纯函数单测（plan 2026-07-07-0305-2 Phase 3 Exit Criteria）。
 *
 * <p>{@link SpcRuleEngine#evaluateRules} 为纯函数，独立构造违规序列验证 4 条规则命中、ruleSet 子集过滤、
 * 受控序列 violatedRules 为空。
 */
public class TestSpcRuleEnginePure {

    private final SpcRuleEngine engine = new SpcRuleEngine();

    private static final BigDecimal CL = new BigDecimal("10");
    private static final BigDecimal UCL = new BigDecimal("13");
    private static final BigDecimal LCL = new BigDecimal("7");

    private static final Set<String> ALL_RULES = Set.of("1", "2", "3", "4");

    @Test
    public void rule1_singlePointBeyond3Sigma() {
        // 单点超出 UCL=13
        java.util.List<BigDecimal> means = toList("10", "10", "10", "20", "10");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, ALL_RULES);
        assertEquals(Set.of("1"), result.get(3));
    }

    @Test
    public void rule2_nineConsecutiveSameSide() {
        // 9 点全部在 cl 上方（=11）
        java.util.List<BigDecimal> means = toList(
                "11", "11", "11", "11", "11", "11", "11", "11", "11");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, ALL_RULES);
        // 第 8 下标（共 9 点）起算：i>=8 触发
        for (int i = 0; i < 9; i++) {
            assertNotNull(result.get(i), "i=" + i + " 应触发规则 2");
            assertEquals(Set.of("2"), result.get(i));
        }
    }

    @Test
    public void rule3_sixMonotonicIncreasing() {
        // 6 点严格递增（10→11→12→13→14→15，14、15 超 UCL 也会同时触发规则 1）
        java.util.List<BigDecimal> means = toList("10", "11", "12", "13", "14", "15");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, ALL_RULES);
        // i=5 触发规则 3（i>=5）
        assertNotNull(result.get(5));
        assertTrue(result.get(5).contains("3"), "应至少触发规则 3，实际: " + result.get(5));
    }

    @Test
    public void rule4_fourteenAlternating() {
        // 14 点交替上下：9,11,9,11,9,11,9,11,9,11,9,11,9,11
        java.util.List<BigDecimal> means = toList(
                "9", "11", "9", "11", "9", "11", "9", "11", "9", "11", "9", "11", "9", "11");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, ALL_RULES);
        // i=13 触发规则 4
        assertNotNull(result.get(13));
        // 规则 4 是主要触发，可能与其他规则同时触发，断言包含 4
        assertEquals(true, result.get(13).contains("4"));
    }

    @Test
    public void controlledSeriesHasNoViolations() {
        // 受控序列：10 点随机分布在 cl 附近，无规则命中
        java.util.List<BigDecimal> means = toList(
                "10.1", "9.9", "10.2", "9.8", "10.0", "10.1", "9.9", "10.0", "10.1", "9.9");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, ALL_RULES);
        assertEquals(true, result.isEmpty() || result.values().stream().allMatch(Set::isEmpty),
                "受控序列无违规，实际: " + result);
    }

    @Test
    public void ruleSetSubsetFiltersRules() {
        // 单点超 3σ 但 ruleSet 仅启用规则 2
        java.util.List<BigDecimal> means = toList("10", "10", "10", "20");
        Set<String> onlyRule2 = Set.of("2");
        java.util.Map<Integer, Set<String>> result = engine.evaluateRules(means, CL, UCL, LCL, onlyRule2);
        assertEquals(true, result.isEmpty() || result.values().stream().allMatch(Set::isEmpty),
                "ruleSet=2 时规则 1 不触发");
    }

    @Test
    public void emptyOrNullMeansReturnsEmpty() {
        assertEquals(true, engine.evaluateRules(null, CL, UCL, LCL, ALL_RULES).isEmpty());
        assertEquals(true, engine.evaluateRules(java.util.Collections.emptyList(), CL, UCL, LCL, ALL_RULES).isEmpty());
    }

    @Test
    public void parseRuleSetHandlesCsv() {
        assertEquals(Set.of("1", "2", "3", "4"), SpcRuleEngine.parseRuleSet("1,2,3,4"));
        assertEquals(Set.of("1", "3"), SpcRuleEngine.parseRuleSet("1, 3"));
        assertEquals(true, SpcRuleEngine.parseRuleSet("").isEmpty());
        assertEquals(true, SpcRuleEngine.parseRuleSet(null).isEmpty());
    }

    private java.util.List<BigDecimal> toList(String... values) {
        return Stream.of(values).map(BigDecimal::new).collect(java.util.stream.Collectors.toList());
    }
}
