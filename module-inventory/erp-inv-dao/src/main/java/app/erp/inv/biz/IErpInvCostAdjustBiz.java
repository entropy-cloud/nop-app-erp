
package app.erp.inv.biz;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 成本调整单聚合根契约（plan 2026-07-05-2352-3）。CRUD 之外承载两个域动作：
 * <ul>
 *   <li>{@link #applyCostAdjust} —— 执行成本调整（更新余额/层/流水 + 标准成本重估发布 + 过账）。</li>
 *   <li>{@link #reverseCostAdjust} —— 红冲回退（回退余额/层 + 红字凭证）。</li>
 * </ul>
 * 标准 5 审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）经 xbiz 脚本委托 Processor。
 */
public interface IErpInvCostAdjustBiz extends ICrudBiz<ErpInvCostAdjust> {

    @BizMutation
    ErpInvCostAdjust applyCostAdjust(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpInvCostAdjust reverseCostAdjust(@Name("id") Long id, IServiceContext context);
}
