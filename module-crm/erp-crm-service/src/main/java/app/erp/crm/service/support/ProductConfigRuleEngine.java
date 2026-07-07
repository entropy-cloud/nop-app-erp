package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmConfigRule;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.util.SourceLocation;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.xlang.api.ExprEvalAction;
import io.nop.xlang.api.XLang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CPQ 配置规则引擎（plan 2026-07-07-1430-2 §Phase 1）。
 *
 * <p>纯函数式：{@link #evaluate(Map, List)} 不依赖 IoC 容器，输入 {@code selectedFeatures} +
 * 规则列表（已由调用方按 {@code configuratorId} 加载），输出每个目标特征的标记结果。
 *
 * <p>对齐 {@code docs/design/crm/cpq.md §1 配置规则引擎}：
 * <ul>
 *   <li>{@code sourceFeatureCode}/{@code sourceFeatureValue} 匹配选中特征时按 {@code ruleType}
 *       标记目标特征（REQUIRED 必选 / EXCLUDED 禁用 / RECOMMENDED 推荐 / OPTIONAL 可选）。</li>
 *   <li>{@code conditionExpression} 不为空时优先评估（XLang 表达式，返回 boolean），
 *       命中则按 {@code ruleType} 标记目标特征，跳过单行 source 匹配判定。</li>
 *   <li>规则按 {@code sequence} 升序遍历；EXCLUDED 同特征会覆盖 RECOMMENDED/OPTIONAL（禁用优先）。</li>
 * </ul>
 *
 * <p>XLang 表达式可访问 {@code selectedFeatures}（Map），例：
 * {@code selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'}。
 */
public class ProductConfigRuleEngine {

    /**
     * 评估配置规则，返回目标特征的标记结果。
     *
     * @param selectedFeatures 选中特征映射：{@code featureCode → featureValue}（不可空，可空 map）
     * @param rules            配置规则列表（已按 {@code configuratorId} 过滤）
     * @return {@code targetFeatureCode → EvaluationResult}；无规则命中返回空 map
     */
    public Map<String, EvaluationResult> evaluate(Map<String, String> selectedFeatures, List<ErpCrmConfigRule> rules) {
        Map<String, String> features = selectedFeatures == null ? Collections.emptyMap() : selectedFeatures;
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ErpCrmConfigRule> ordered = new ArrayList<>(rules);
        ordered.sort(Comparator.comparing(r -> r.getSequence() == null ? Integer.MAX_VALUE : r.getSequence()));

        Map<String, EvaluationResult> results = new LinkedHashMap<>();
        for (ErpCrmConfigRule rule : ordered) {
            boolean match;
            String expr = rule.getConditionExpression();
            if (expr != null && !expr.trim().isEmpty()) {
                match = evalCondition(expr, features);
            } else {
                match = matchSource(rule, features);
            }
            if (!match) {
                continue;
            }
            applyRule(rule, results);
        }
        return results;
    }

    protected boolean matchSource(ErpCrmConfigRule rule, Map<String, String> features) {
        String sourceCode = rule.getSourceFeatureCode();
        if (sourceCode == null || sourceCode.isEmpty()) {
            // 无 source 条件视为恒真（仅按 ruleType 标记目标，对齐 design 伪代码允许无条件规则）
            return true;
        }
        String selected = features.get(sourceCode);
        if (selected == null) {
            return false;
        }
        String expected = rule.getSourceFeatureValue();
        return expected == null || Objects.equals(norm(selected), norm(expected));
    }

    protected boolean evalCondition(String expr, Map<String, String> features) {
        try {
            IEvalScope scope = XLang.newEvalScope();
            scope.setLocalValue(null, "selectedFeatures", features);
            ExprEvalAction action = XLang.newCompileTool()
                    .allowUnregisteredScopeVar(true)
                    .compileFullExpr(
                            SourceLocation.fromClass(ProductConfigRuleEngine.class), expr);
            Object result = action.invoke(scope);
            return Boolean.TRUE.equals(result);
        } catch (NopException e) {
            throw e.param("conditionExpression", expr);
        }
    }

    protected void applyRule(ErpCrmConfigRule rule, Map<String, EvaluationResult> results) {
        String targetCode = rule.getTargetFeatureCode();
        if (targetCode == null || targetCode.isEmpty()) {
            return;
        }
        String ruleType = rule.getRuleType();
        String targetValue = rule.getTargetFeatureValue();
        EvaluationResult existing = results.get(targetCode);

        // EXCLUDED 禁用优先：一旦标记 EXCLUDED 不被后续 REQUIRED/RECOMMENDED 覆盖
        if (existing != null && ErpCrmConstants.CONFIG_RULE_TYPE_EXCLUDED.equals(existing.getMark())) {
            return;
        }
        if (ErpCrmConstants.CONFIG_RULE_TYPE_EXCLUDED.equals(ruleType) && existing != null) {
            existing.setMark(ErpCrmConstants.CONFIG_RULE_TYPE_EXCLUDED);
            existing.addTriggerRule(rule);
            return;
        }
        if (existing == null) {
            EvaluationResult result = new EvaluationResult();
            result.setFeatureCode(targetCode);
            result.setFeatureValue(targetValue);
            result.setMark(ruleType != null ? ruleType : ErpCrmConstants.CONFIG_RULE_TYPE_OPTIONAL);
            result.addTriggerRule(rule);
            results.put(targetCode, result);
        } else {
            existing.addTriggerRule(rule);
            if (ruleType != null) {
                existing.setMark(ruleType);
            }
            if (targetValue != null) {
                existing.setFeatureValue(targetValue);
            }
        }
    }

    protected String norm(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 单条规则评估结果（目标特征 → 标记 + 触发规则列表）。
     */
    public static class EvaluationResult {
        private String featureCode;
        private String featureValue;
        private String mark;
        private final List<ErpCrmConfigRule> triggerRules = new ArrayList<>();

        public String getFeatureCode() {
            return featureCode;
        }

        public void setFeatureCode(String featureCode) {
            this.featureCode = featureCode;
        }

        public String getFeatureValue() {
            return featureValue;
        }

        public void setFeatureValue(String featureValue) {
            this.featureValue = featureValue;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(String mark) {
            this.mark = mark;
        }

        public List<ErpCrmConfigRule> getTriggerRules() {
            return triggerRules;
        }

        public void addTriggerRule(ErpCrmConfigRule rule) {
            triggerRules.add(rule);
        }
    }
}
