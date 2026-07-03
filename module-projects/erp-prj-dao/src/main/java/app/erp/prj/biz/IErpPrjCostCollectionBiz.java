package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjCostCollection;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;

/**
 * 项目成本归集 Biz 契约。CRUD 之上承载费用报销归集接入：
 * <ul>
 *   <li>{@link #refreshExpenseCost(Long, IServiceContext)}：projects 驱动只读聚合——
 *       经 {@code IErpFinExpenseClaimBiz} 只读查已审核报销单（行 projectId 命中），
 *       projects 自写 {@code erp_prj_cost_collection} 行（对齐 data-dependency-matrix
 *       §3.2/§4.2：finance 从不写业务表，归集由 projects 触发）。</li>
 * </ul>
 *
 * <p>费用归集受 {@code erp-prj.expense-aggregation-enabled}（默认 true）config-gated。
 * 关闭时 {@code refreshExpenseCost} 直接返回 0。
 */
public interface IErpPrjCostCollectionBiz extends ICrudBiz<ErpPrjCostCollection> {

    /**
     * 刷新项目的费用报销归集。返回本次新增的归集金额合计。
     */
    @BizMutation
    BigDecimal refreshExpenseCost(@Name("projectId") Long projectId, IServiceContext context);
}
