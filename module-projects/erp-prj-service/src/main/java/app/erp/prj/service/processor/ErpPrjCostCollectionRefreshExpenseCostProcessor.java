package app.erp.prj.service.processor;

import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpPrjCostCollection refreshExpenseCost per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含费用报销归集刷新（config-gated，关闭时返回 0）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjCostCollectionRefreshExpenseCostProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ExpenseCostAggregator expenseCostAggregator;

    public BigDecimal refreshExpenseCost(Long projectId, IServiceContext context) {
        if (!ErpPrjConfigs.expenseAggregationEnabled()) {
            return BigDecimal.ZERO;
        }
        return expenseCostAggregator.refreshExpenseCost(projectId);
    }
}
