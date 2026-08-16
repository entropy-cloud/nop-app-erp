package app.erp.prj.service.processor;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.cost.BudgetChecker;
import app.erp.prj.service.cost.MaterialCostAggregator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpPrjCostCollection aggregateMaterialCost per-mutation Processor（RC-R1.61 / P1-RC-049，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含跨域物料归集编排：config 门控 → requireReferenceable 单一咽喉守卫（P2-RC-048 协同）→
 * 预算检查（STRICT 拒绝 / WARNING 放行，P1-RC-051 采购路径 merge）→ 聚合器幂等写入。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjCostCollectionAggregateMaterialCostProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpPrjProjectBiz projectBiz;
    @Inject
    BudgetChecker budgetChecker;
    @Inject
    MaterialCostAggregator materialCostAggregator;

    public BigDecimal aggregateMaterialCost(Long projectId, BigDecimal amount, String sourceBillCode,
                                            IServiceContext context) {
        if (!ErpPrjConfigs.materialAggregationEnabled()) {
            return BigDecimal.ZERO;
        }
        if (projectId == null || amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        // 项目状态守卫走单一咽喉 Facade（P2-RC-048：跨域侧同样调 requireReferenceable）
        projectBiz.requireReferenceable(projectId, context);
        // 预算检查在归集行写入前（P1-RC-051 merge：STRICT 抛 ERR_BUDGET_EXCEEDED / WARNING 放行）
        budgetChecker.check(projectId, amount);
        return materialCostAggregator.aggregateMaterial(projectId, amount, sourceBillCode);
    }
}
