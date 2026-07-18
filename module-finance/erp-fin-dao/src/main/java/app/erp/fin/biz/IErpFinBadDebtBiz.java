
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.dto.BadDebtProvisionReversalResult;
import app.erp.fin.dao.entity.ErpFinBadDebt;

import java.math.BigDecimal;

/**
 * 坏账单聚合根 Biz。CRUD 之外，承载坏账核销/收回审批状态机与期末计提/释放入口（{@code bad-debt.md}）：
 * <ul>
 *   <li>{@link #writeOff} / {@link #recover} —— 创建核销/恢复坏账单（审批门控）</li>
 *   <li>{@link #submit} / {@link #approve} / {@link #reject} —— 三轴审批状态机</li>
 *   <li>{@link #runBadDebtProvision} —— 期末账龄分桶法计提/释放（必需准备 vs Allowance 账面）</li>
 *   <li>{@link #reverseBadDebtProvision} —— 反向坏账准备计提/释放红冲闭环（plan 2026-07-18-2251-2）</li>
 * </ul>
 *
 * <p>Facade 仅负责入口/事务/委托；编排（审批状态机 + ArApItem 变异 + 凭证生成）委托
 * {@code ErpFinBadDebtProcessor}，计提/释放委托 {@code BadDebtProvisionService}。
 */
public interface IErpFinBadDebtBiz extends ICrudBiz<ErpFinBadDebt> {

    /**
     * 坏账核销：创建 WRITE_OFF 坏账单（金额 = 源 AR 辅助账项 openAmount）。
     * 审批门控 {@code erp-fin.bad-debt-write-off-require-approval}（默认 true）：关闭时创建即自动审批执行。
     */
    @BizMutation
    ErpFinBadDebt writeOff(@Name("arApItemId") Long arApItemId,
                           @Name("reason") String reason,
                           IServiceContext context);

    /**
     * 坏账收回恢复：创建 RECOVERY 坏账单（恢复已核销项）。审批门控同 {@link #writeOff}。
     */
    @BizMutation
    ErpFinBadDebt recover(@Name("arApItemId") Long arApItemId,
                          @Name("reason") String reason,
                          IServiceContext context);

    /** 提交审核：UNSUBMITTED → SUBMITTED。 */
    @BizMutation
    ErpFinBadDebt submit(@Name("id") Long id, IServiceContext context);

    /** 审核通过：SUBMITTED → APPROVED，执行 ArApItem 变异 + 凭证生成。 */
    @BizMutation
    ErpFinBadDebt approve(@Name("id") Long id, IServiceContext context);

    /**
     * 反审核（红冲闭环）：APPROVED → REJECTED，红冲 BAD_DEBT_WRITE_OFF/RECOVERY 凭证 + 回退 ArApItem 状态对称
     * （writeOff: WRITTEN_OFF→OPEN；recovery: OPEN→WRITTEN_OFF）。
     *
     * <p>owner doc：{@code docs/design/finance/treasury.md §坏账} + {@code bad-debt.md §步骤3/4a} 红冲路径
     * （plan {@code 2026-07-18-1745-3} 落地）。{@code ErpFinBadDebt} 无 {@code useWorkflow} tagSet，故不经 xwf
     * 反向，DIRECT 路径调 {@code FinPostingExecutor.reverse(badDebt.code, BAD_DEBT_WRITE_OFF|RECOVERY)}。
     */
    @BizMutation
    ErpFinBadDebt reverseApprove(@Name("id") Long id, IServiceContext context);

    /** 驳回：SUBMITTED → REJECTED。 */
    @BizMutation
    ErpFinBadDebt reject(@Name("id") Long id, IServiceContext context);

    /**
     * 期末坏账准备计提/释放（账龄分桶法）。必需准备 &gt; Allowance 账面 → 补提 BAD_DEBT_RESERVE；
     * 必需准备 &lt; 账面 → 释放 BAD_DEBT_RELEASE；相等 → 无动作。
     */
    @BizMutation
    BadDebtProvisionResult runBadDebtProvision(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 反向坏账准备计提/释放红冲闭环（{@code bad-debt.md §步骤2b 反向红冲}，plan 2026-07-18-2251-2）。
     *
     * <p>反向指定期间全部 BAD_DEBT_RESERVE/RELEASE 已过账未冲销 NORMAL 凭证（覆盖多次 {@link #runBadDebtProvision}
     * 累积——{@code CloseVoucherWriter} 无幂等检查，多次调用累积多张凭证）→ 调
     * {@code FinPostingExecutor.reverse(billCode, businessType)} 原子红冲。
     *
     * <p>守卫：(1) period.status=CLOSED_FINAL 抛 {@code ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED}；
     * (2) 未找到任何 BAD_DEBT_RESERVE/RELEASE 已过账未冲销凭证抛 {@code ERR_BAD_DEBT_PROVISION_NOT_FOUND}。
     *
     * <p>对称 {@link #runBadDebtProvision}：单 periodId 入参；返回 {@link BadDebtProvisionReversalResult}
     * 含红冲凭证数量 + 反向金额合计。反向后调用方可再调 {@link #runBadDebtProvision} 重提
     * （{@code getAllowanceBalance} 基于红冲后状态重算）。
     */
    @BizMutation
    BadDebtProvisionReversalResult reverseBadDebtProvision(@Name("periodId") Long periodId, IServiceContext context);
}
