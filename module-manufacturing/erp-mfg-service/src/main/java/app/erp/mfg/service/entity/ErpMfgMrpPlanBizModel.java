
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMrpPlanBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.service.processor.ErpMfgMrpPlanRunMrpProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * MRP 计划 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * {@code runMrp}（@BizMutation）委托 {@link ErpMfgMrpPlanRunMrpProcessor}（R6.2 per-mutation 拆分）。
 */
@BizModel("ErpMfgMrpPlan")
public class ErpMfgMrpPlanBizModel extends CrudBizModel<ErpMfgMrpPlan> implements IErpMfgMrpPlanBiz {
    @Inject
    ErpMfgMrpPlanRunMrpProcessor runMrpProcessor;

    public ErpMfgMrpPlanBizModel() {
        setEntityName(ErpMfgMrpPlan.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgMrpPlan runMrp(@Name("planId") Long planId, IServiceContext context) {
        return runMrpProcessor.runMrp(planId, context);
    }
}
