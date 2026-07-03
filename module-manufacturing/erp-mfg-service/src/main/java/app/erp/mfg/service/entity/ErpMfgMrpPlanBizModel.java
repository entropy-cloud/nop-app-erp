
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpPlanBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.service.mrp.DemandAggregator;
import app.erp.mfg.service.mrp.MrpEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMfgMrpPlan")
public class ErpMfgMrpPlanBizModel extends CrudBizModel<ErpMfgMrpPlan> implements IErpMfgMrpPlanBiz {
    @Inject
    DemandAggregator demandAggregator;
    @Inject
    MrpEngine mrpEngine;

    public ErpMfgMrpPlanBizModel() {
        setEntityName(ErpMfgMrpPlan.class.getName());
    }

    public void setDemandAggregator(DemandAggregator demandAggregator) {
        this.demandAggregator = demandAggregator;
    }

    public void setMrpEngine(MrpEngine mrpEngine) {
        this.mrpEngine = mrpEngine;
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlan runMrp(@Name("planId") Long planId, IServiceContext context) {
        mrpEngine.runMrp(planId, demandAggregator.aggregate(planId));
        return get(String.valueOf(planId), false, context);
    }
}
