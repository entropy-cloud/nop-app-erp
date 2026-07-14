package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvCostAdjustBiz;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.service.processor.ErpInvCostAdjustProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 成本调整单 BizModel（Facade）。域动作 {@code applyCostAdjust}/{@code reverseCostAdjust} 委托
 * {@link ErpInvCostAdjustProcessor}（protected step 方法，下游可逐 step 覆盖）。标准 5 审批动作
 * 经 xbiz 脚本委托同一 Processor。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §成本调整}、
 * {@code docs/plans/2026-07-05-2352-3-inventory-cost-adjustment.md}。
 */
@BizModel("ErpInvCostAdjust")
public class ErpInvCostAdjustBizModel extends CrudBizModel<ErpInvCostAdjust> implements IErpInvCostAdjustBiz {

    @Inject
    ErpInvCostAdjustProcessor costAdjustProcessor;

    public ErpInvCostAdjustBizModel() {
        setEntityName(ErpInvCostAdjust.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvCostAdjust applyCostAdjust(@Name("id") Long id, IServiceContext context) {
        return costAdjustProcessor.applyCostAdjust(id, context);
    }

    @Override
    @BizMutation
    public ErpInvCostAdjust reverseCostAdjust(@Name("id") Long id, IServiceContext context) {
        return costAdjustProcessor.reverseCostAdjust(id, context);
    }
}
