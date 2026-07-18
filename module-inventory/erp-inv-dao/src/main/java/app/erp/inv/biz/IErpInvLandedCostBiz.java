
package app.erp.inv.biz;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 到岸成本单聚合根契约（plan 2026-07-10-1100-3）。CRUD 之外承载：
 * <ul>
 *   <li>{@link #approve} —— 审核编排：分摊计算 → 创建成本调整单更新成本层 → 生成 GL 凭证。</li>
 *   <li>{@link #allocate} —— 分摊预览（只读 query，不落库）：前端编辑页预览分摊结果。</li>
 *   <li>{@link #generateFreightLandedCost} —— 按采购入库单 code + 运费金额自动创建 DRAFT 到岸成本单（plan 2026-07-11-2329-1 path-2）。</li>
 *   <li>{@link #reverseApprove} —— 红冲已审核已过账到岸成本单（plan 2026-07-18-1745-2）：
 *       红冲 LANDED_COST 凭证 + 反向应用 LANDED_COST_SUPPLEMENT 成本层 + 翻 posted=false / approveStatus=REJECTED / docStatus=CANCELLED。</li>
 * </ul>
 */
public interface IErpInvLandedCostBiz extends ICrudBiz<ErpInvLandedCost> {

    @BizMutation
    ErpInvLandedCost approve(@Name("id") Long id, IServiceContext context);

    @BizQuery
    List<Map<String, Object>> allocate(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpInvLandedCost generateFreightLandedCost(@Name("receiveCode") String receiveCode,
                                               @Name("freightAmount") BigDecimal freightAmount,
                                               @Name("freightCurrencyId") Long freightCurrencyId,
                                               @Name("freightExchangeRate") BigDecimal freightExchangeRate,
                                               IServiceContext context);

    @BizMutation
    ErpInvLandedCost reverseApprove(@Name("id") Long id, IServiceContext context);
}
