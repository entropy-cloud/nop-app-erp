package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 项目结算单 Biz 契约。CRUD 之上承载三轴状态机（docStatus/approveStatus/posted）与转固过账
 * （{@code profitability.md §关键流程 2/3}）：
 * <ul>
 *   <li>{@link #createSettlement}：基于最新 PnL 快照创建结算单 + 明细行。</li>
 *   <li>{@link #submit}/{@link #approve}/{@link #reject}/{@link #cancel}：自定义审批状态机
 *       （强制审批 config-gated {@code erp-prj.settlement-require-approval}，镜像 {@code IErpFinBadDebtBiz} 范式）。</li>
 *   <li>{@link #approve} 末尾按 settlementType 分派：FINAL/INTERIM 仅过账；CLOSE 额外转固（建资产卡片 + 凭证）。</li>
 *   <li>{@link #reverseSettlement}：红字冲销凭证 + 回退资产卡片状态。</li>
 * </ul>
 *
 * <p>Facade 仅负责入口/事务/委托；编排（状态机 + 明细构建 + 转固 + 过账）委托
 * {@code ErpPrjProjectSettlementProcessor}。
 */
public interface IErpPrjProjectSettlementBiz extends ICrudBiz<ErpPrjProjectSettlement> {

    /**
     * 创建结算单。基于项目最新 {@code calcStatus=CALCULATED} 损益快照填充 finalRevenue/finalCost/finalProfit，
     * 按来源单据（Billing/CostCollection）生成明细行。
     */
    @BizMutation
    ErpPrjProjectSettlement createSettlement(@Name("projectId") Long projectId,
                                             @Name("settlementType") String settlementType,
                                             IServiceContext context);

    /** 提交审核：UNSUBMITTED → SUBMITTED。 */
    @BizMutation
    ErpPrjProjectSettlement submit(@Name("id") Long id, IServiceContext context);

    /**
     * 审核通过：SUBMITTED → APPROVED，按 settlementType 分派过账（FINAL/INTERIM 仅凭证；CLOSE 额外转固建卡 + 凭证）。
     * 强制审批关闭时（{@code erp-prj.settlement-require-approval=false}）允许 UNSUBMITTED 直接审批。
     */
    @BizMutation
    ErpPrjProjectSettlement approve(@Name("id") Long id, IServiceContext context);

    /** 驳回：SUBMITTED → REJECTED。 */
    @BizMutation
    ErpPrjProjectSettlement reject(@Name("id") Long id, IServiceContext context);

    /** 取消：→ CANCELLED。已过账时先红冲凭证 + 回退卡片。 */
    @BizMutation
    ErpPrjProjectSettlement cancel(@Name("id") Long id, IServiceContext context);

    /**
     * 红字冲销结算单。冲销已过账的 PROJECT_SETTLEMENT 凭证 + 回退资产卡片状态（已转固时）+ {@code posted=false}。
     * 冲销是硬前置，失败向上抛出阻断。
     */
    @BizMutation
    ErpPrjProjectSettlement reverseSettlement(@Name("settlementId") Long settlementId, IServiceContext context);
}
