
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinBadDebt;

import java.math.BigDecimal;

/**
 * 坏账单聚合根 Biz。CRUD 之外，承载坏账核销/收回审批状态机与期末计提/释放入口（{@code bad-debt.md}）：
 * <ul>
 *   <li>{@link #writeOff} / {@link #recover} —— 创建核销/恢复坏账单（审批门控）</li>
 *   <li>{@link #submit} / {@link #approve} / {@link #reject} —— 三轴审批状态机</li>
 *   <li>{@link #runBadDebtProvision} —— 期末账龄分桶法计提/释放（必需准备 vs Allowance 账面）</li>
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

    /** 驳回：SUBMITTED → REJECTED。 */
    @BizMutation
    ErpFinBadDebt reject(@Name("id") Long id, IServiceContext context);

    /**
     * 期末坏账准备计提/释放（账龄分桶法）。必需准备 &gt; Allowance 账面 → 补提 BAD_DEBT_RESERVE；
     * 必需准备 &lt; 账面 → 释放 BAD_DEBT_RELEASE；相等 → 无动作。
     */
    @BizMutation
    BadDebtProvisionResult runBadDebtProvision(@Name("periodId") Long periodId, IServiceContext context);
}
