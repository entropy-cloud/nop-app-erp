package app.erp.prj.service.processor;

import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.cost.ExpenseCostAggregator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjCostCollection refreshExpenseCost per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含费用报销归集刷新（config-gated，关闭时返回 0）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjCostCollectionRefreshExpenseCostProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ExpenseCostAggregator expenseCostAggregator;

    public BigDecimal refreshExpenseCost(String projectId, IServiceContext context) {
        if (!ErpPrjConfigs.expenseAggregationEnabled()) {
            return BigDecimal.ZERO;
        }
        return expenseCostAggregator.refreshExpenseCost(projectId);
    }
}
