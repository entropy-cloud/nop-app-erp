package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

/**
 * 项目损益汇总 Biz 契约。CRUD 之上承载损益汇总计算引擎：
 * <ul>
 *   <li>{@link #refreshPnl(Long, LocalDate, LocalDate, IServiceContext)}：按项目+期间聚合 Billing 收入 +
 *       CostCollection 四类成本 → {@link ErpPrjProjectPnl}（毛利/毛利率/EAC），幂等（同期间重算清旧重建）。</li>
 *   <li>{@link #getProjectPnl(Long, IServiceContext)}：返回最新 {@code calcStatus=CALCULATED} 汇总快照。</li>
 * </ul>
 *
 * <p>对齐 {@code profitability.md §关键流程 1}：汇总计算可手工触发或经 nop-job（{@code erp-prj-pnl-calc}）周期触发。
 */
public interface IErpPrjProjectPnlBiz extends ICrudBiz<ErpPrjProjectPnl> {

    /**
     * 刷新项目损益汇总。聚合指定期间内 Billing 收入与 CostCollection 四类成本到 {@link ErpPrjProjectPnl}，
     * 计算毛利/毛利率/完工预测成本（EAC）。同期间重算幂等（清旧重建行）。
     *
     * @param projectId  项目 ID
     * @param periodFrom 汇总期间起（含）；为 null 时取项目 startDate
     * @param periodTo   汇总期间止（含）；为 null 时取今天
     * @param context    服务上下文
     * @return 计算后的损益汇总快照
     */
    @BizMutation
    ErpPrjProjectPnl refreshPnl(@Name("projectId") Long projectId,
                                @Name("periodFrom") LocalDate periodFrom,
                                @Name("periodTo") LocalDate periodTo,
                                IServiceContext context);

    /**
     * 查询项目最新已计算的损益汇总快照（{@code calcStatus=CALCULATED}，按 periodTo 倒序取首条）。
     */
    @BizQuery
    ErpPrjProjectPnl getProjectPnl(@Name("projectId") Long projectId, IServiceContext context);
}
