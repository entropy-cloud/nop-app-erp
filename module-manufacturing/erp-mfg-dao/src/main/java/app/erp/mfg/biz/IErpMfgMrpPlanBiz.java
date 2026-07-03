
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgMrpPlan;

public interface IErpMfgMrpPlanBiz extends ICrudBiz<ErpMfgMrpPlan>{

    /**
     * 运行 MRP：整合独立需求（销售订单/安全库存/手工）→ BOM 多级展开 → 净需求 → 按期分单 → 生成计划订单行。
     * 计划状态 DRAFT→RUNNING→COMPLETED。权威：{@code docs/design/manufacturing/mrp.md}。
     */
    @BizMutation
    ErpMfgMrpPlan runMrp(@Name("planId") Long planId, IServiceContext context);
}
