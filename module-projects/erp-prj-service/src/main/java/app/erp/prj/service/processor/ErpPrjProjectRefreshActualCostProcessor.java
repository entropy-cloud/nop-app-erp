package app.erp.prj.service.processor;

import app.erp.prj.service.cost.ProjectCostAggregator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpPrjProject refreshActualCost per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含项目实际成本回写。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectRefreshActualCostProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ProjectCostAggregator costAggregator;

    public BigDecimal refreshActualCost(Long projectId, IServiceContext context) {
        return costAggregator.refreshActualCost(projectId);
    }
}
