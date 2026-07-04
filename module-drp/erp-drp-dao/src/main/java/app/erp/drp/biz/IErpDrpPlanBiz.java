package app.erp.drp.biz;

import app.erp.drp.dao.entity.ErpDrpPlan;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpDrpPlanBiz extends ICrudBiz<ErpDrpPlan> {

    /**
     * 运行 DRP 净需求计算：计划状态 DRAFT→COMPUTED，生成 SUGGESTED 明细行。
     */
    @BizMutation
    ErpDrpPlan runDrp(@Name("planId") Long planId, IServiceContext context);

    /**
     * 批准 DRP 计划：计划状态 COMPUTED→APPROVED，明细行 SUGGESTED→APPROVED（由计划主管审批）。
     */
    @BizMutation
    ErpDrpPlan approvePlan(@Name("planId") Long planId, IServiceContext context);

    /**
     * 重置计划为 DRAFT：清除既有 SUGGESTED 行，供调参后重新计算。
     */
    @BizMutation
    ErpDrpPlan resetToDraft(@Name("planId") Long planId, IServiceContext context);
}
