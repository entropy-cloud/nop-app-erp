
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;

public interface IErpMfgMrpPlanLineBiz extends ICrudBiz<ErpMfgMrpPlanLine>{

    /**
     * 释放采购建议行为采购订单（{@link app.erp.pur.dao.entity.ErpPurOrder}）。回写 isFirmed/convertedBillCode，
     * 全部行释放后 MrpPlan→FIRMED。幂等：已 firmed 行重复释放拒绝。权威：{@code docs/design/manufacturing/mrp.md §建议单释放}。
     *
     * @param supplierId 供应商（必填，ErpPurOrder.supplierId 为 ORM 必填）
     * @param currencyId 币种（必填，ErpPurOrder.currencyId 为 ORM 必填）
     */
    @BizMutation
    ErpMfgMrpPlanLine releasePurchaseRequest(@Name("planLineId") Long planLineId,
                                             @Name("supplierId") Long supplierId,
                                             @Name("currencyId") Long currencyId,
                                             IServiceContext context);

    /**
     * 释放工单建议行为工单（{@link app.erp.mfg.dao.entity.ErpMfgWorkOrder}）。回写 isFirmed/convertedBillCode，
     * 全部行释放后 MrpPlan→FIRMED。幂等：已 firmed 行重复释放拒绝。
     */
    @BizMutation
    ErpMfgMrpPlanLine releaseWorkRequest(@Name("planLineId") Long planLineId, IServiceContext context);
}
