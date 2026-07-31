package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.drp.DrpDemandAggregator;
import app.erp.drp.service.drp.DrpEngine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpDrpPlan runDrp per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 DRP 净需求计算编排入口（plan DRAFT→COMPUTED）：聚合需求 + 委派 {@link DrpEngine#runDrp} + 回读计划。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpPlanRunDrpProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DrpEngine drpEngine;

    @Inject
    DrpDemandAggregator demandAggregator;

    public ErpDrpPlan runDrp(Long planId, IServiceContext context) {
        drpEngine.runDrp(planId, demandAggregator.aggregate(planId));
        return requirePlan(planId);
    }

    // ---------- 内部辅助 ----------

    protected ErpDrpPlan requirePlan(Long planId) {
        ErpDrpPlan plan = dao().getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_NOT_FOUND)
                    .param(ErpDrpErrors.ARG_DRP_PLAN_ID, planId);
        }
        return plan;
    }

    private IEntityDao<ErpDrpPlan> dao() {
        return daoProvider.daoFor(ErpDrpPlan.class);
    }
}
