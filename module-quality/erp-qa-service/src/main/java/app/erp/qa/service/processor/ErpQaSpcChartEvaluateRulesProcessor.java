package app.erp.qa.service.processor;

import app.erp.qa.service.spc.SpcRuleEngine;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaSpcChart evaluateRules per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含控制图失控规则评估编排（委托 {@link SpcRuleEngine}）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpQaSpcChartEvaluateRulesProcessor {

    @Inject
    SpcRuleEngine spcRuleEngine;

    public Integer evaluateRules(Long chartId, IServiceContext context) {
        return spcRuleEngine.evaluate(chartId, context);
    }
}
