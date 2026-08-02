package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

/**
 * ErpHrGapAnalysis refreshGapAnalysisWithLevels per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含差距快照刷新编排（规范化调用方预聚合 levels → 清旧重建）。供 {@code completeAssessment} 内部直传避免二次查询。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrGapAnalysisProcessor}。
 */
public class ErpHrGapAnalysisRefreshGapAnalysisWithLevelsProcessor extends AbstractErpHrGapAnalysisProcessor {

    public List<ErpHrGapAnalysis> refreshGapAnalysisWithLevels(Long employeeId,
                                                               Map<Long, Integer> aggregatedLevels,
                                                               IServiceContext context) {
        Map<Long, Integer> normalized = normalizeLevelMap(aggregatedLevels);
        return doRefreshWithLevels(employeeId, normalized, context);
    }
}
