package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

/**
 * ErpHrGapAnalysis refreshGapAnalysis per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含差距快照刷新编排（聚合最新 COMPLETED 评估 actualLevel → 清旧重建）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrGapAnalysisProcessor}。
 */
public class ErpHrGapAnalysisRefreshGapAnalysisProcessor extends AbstractErpHrGapAnalysisProcessor {

    public List<ErpHrGapAnalysis> refreshGapAnalysis(String employeeId, IServiceContext context) {
        Map<String, Integer> levels = aggregateLatestAssessment(employeeId, context);
        return doRefreshWithLevels(employeeId, levels, context);
    }
}
