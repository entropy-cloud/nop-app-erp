package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpDrpPlanBiz;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.processor.ErpDrpPlanApprovePlanProcessor;
import app.erp.drp.service.processor.ErpDrpPlanResetToDraftProcessor;
import app.erp.drp.service.processor.ErpDrpPlanRunDrpProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 计划 BizModel。薄委派层（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）：
 * {@link #runDrp}/{@link #resetToDraft}/{@link #approvePlan} 各委派独立自包含 Processor（编排位置迁移，业务语义不变）。
 */
@BizModel("ErpDrpPlan")
public class ErpDrpPlanBizModel extends CrudBizModel<ErpDrpPlan> implements IErpDrpPlanBiz {

    @Inject
    ErpDrpPlanRunDrpProcessor runDrpProcessor;
    @Inject
    ErpDrpPlanResetToDraftProcessor resetToDraftProcessor;
    @Inject
    ErpDrpPlanApprovePlanProcessor approvePlanProcessor;

    public ErpDrpPlanBizModel() {
        setEntityName(ErpDrpPlan.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpDrpPlan> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpDrpPlan entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpDrpPlan runDrp(@Name("planId") Long planId, IServiceContext context) {
        return runDrpProcessor.runDrp(planId, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan resetToDraft(@Name("planId") Long planId, IServiceContext context) {
        return resetToDraftProcessor.resetToDraft(planId, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan approvePlan(@Name("planId") Long planId, IServiceContext context) {
        return approvePlanProcessor.approvePlan(planId, context);
    }
}
