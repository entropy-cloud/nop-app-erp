package app.erp.drp.biz;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpDrpLineBiz extends ICrudBiz<ErpDrpLine> {

    /**
     * 释放单条 APPROVED 明细行：按补货类型生成调拨单/采购订单，回写 orderBillType/orderBillCode，DrpLine→ORDERED。
     */
    @BizMutation
    ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context);

    /**
     * 批量释放计划下所有 APPROVED 行；全部行 ORDERED 后计划 APPROVED→EXECUTED。
     * 受配置 {@code erp-inv.drp-auto-generate-order} 控制。
     */
    @BizMutation
    ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context);

    /**
     * 行级审批：SUGGESTED→APPROVED。批准补货建议，进入待释放状态。
     */
    @BizMutation
    ErpDrpLine approveLine(@Name("lineId") Long lineId, IServiceContext context);

    /**
     * 行级驳回：SUGGESTED/APPROVED→CANCELLED。拒绝补货建议（不进入释放流程）。
     */
    @BizMutation
    ErpDrpLine rejectLine(@Name("lineId") Long lineId, IServiceContext context);

    /**
     * 行级作废：SUGGESTED/APPROVED→CANCELLED。
     */
    @BizMutation
    ErpDrpLine cancelLine(@Name("lineId") Long lineId, IServiceContext context);
}
