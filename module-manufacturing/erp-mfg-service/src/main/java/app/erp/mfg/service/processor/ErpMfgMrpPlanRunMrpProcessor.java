package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.service.mrp.DemandAggregator;
import app.erp.mfg.service.mrp.MrpEngine;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpMfgMrpPlan runMrp per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 MRP 运算编排（需求聚合 → MRP 引擎计算 → 重载计划返回）；从 ErpMfgMrpPlanBizModel 内联 @BizMutation 提取。
 */
public class ErpMfgMrpPlanRunMrpProcessor {

    @Inject
    DemandAggregator demandAggregator;
    @Inject
    MrpEngine mrpEngine;
    @Inject
    IDaoProvider daoProvider;

    public ErpMfgMrpPlan runMrp(Long planId, IServiceContext context) {
        mrpEngine.runMrp(planId, demandAggregator.aggregate(planId));
        return planDao().getEntityById(planId);
    }

    protected IEntityDao<ErpMfgMrpPlan> planDao() {
        return daoProvider.daoFor(ErpMfgMrpPlan.class);
    }
}
