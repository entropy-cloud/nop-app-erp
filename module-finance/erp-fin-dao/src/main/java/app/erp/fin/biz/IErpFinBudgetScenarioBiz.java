
package app.erp.fin.biz;

import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 预算方案聚合根 Biz（{@code budget.md}）。CRUD 之外，承载预算方案状态机：
 * <ul>
 *   <li>{@link #submit} —— DRAFT → SUBMITTED</li>
 *   <li>{@link #approve} —— SUBMITTED → APPROVED，生成 postingType=BUDGET 的预算影子凭证</li>
 *   <li>{@link #reject} —— SUBMITTED → REJECTED → DRAFT</li>
 *   <li>{@link #cancel} —— APPROVED → CANCELLED，红冲原 BUDGET 凭证</li>
 * </ul>
 *
 * <p>Facade 仅负责入口/事务/委托；编排（状态机 + 预算凭证生成）委托 {@code ErpFinBudgetScenarioProcessor}。
 */
public interface IErpFinBudgetScenarioBiz extends ICrudBiz<ErpFinBudgetScenario> {

    /** 提交审核：DRAFT/REJECTED → SUBMITTED。 */
    @BizMutation
    ErpFinBudgetScenario submit(@Name("id") Long id, IServiceContext context);

    /** 审核通过：SUBMITTED → APPROVED，生成 BUDGET 影子凭证。 */
    @BizMutation
    ErpFinBudgetScenario approve(@Name("id") Long id, IServiceContext context);

    /** 驳回：SUBMITTED → REJECTED。 */
    @BizMutation
    ErpFinBudgetScenario reject(@Name("id") Long id, IServiceContext context);

    /** 作废：APPROVED → CANCELLED，红冲原 BUDGET 凭证。 */
    @BizMutation
    ErpFinBudgetScenario cancel(@Name("id") Long id, IServiceContext context);

    /**
     * 滚动预算自动复制（A2，plan 2026-07-21-1206-2，budget.md §滚动预算自动复制引擎）。
     *
     * <p>按策略将源方案（必须 APPROVED）的 BudgetLine 复制至 newFiscalYear 新方案，periodId 按
     * (newFiscalYear - source.fiscalYear) 偏移重映射；写 RollforwardLog；返回新方案（DRAFT 状态）。
     *
     * @param id            源方案 ID
     * @param newFiscalYear 目标年度
     * @param strategy      复制策略（FIXED_PERCENTAGE / ZERO_BASED / INCREMENTAL）；null 用 config 缺省
     * @param context       服务上下文
     */
    @BizMutation
    ErpFinBudgetScenario rollForward(@Name("id") Long id,
                                     @Name("newFiscalYear") Integer newFiscalYear,
                                     @Name("strategy") String strategy,
                                     IServiceContext context);

    /**
     * 预算结转（A2，plan 2026-07-21-1206-2，budget.md §结转规则引擎）。
     *
     * <p>按规则将源方案（必须 APPROVED）的预算结转至 targetScenarioId 目标方案（必须 DRAFT）。
     * 结转生成 BUDGET 凭证写入目标方案；源方案状态置 CLOSED（终态）；写 CarryForwardLog。
     *
     * @param id              源方案 ID
     * @param targetScenarioId 目标方案 ID
     * @param rule            结转规则（REMAINING_FULL / REMAINING_RATIO / USED_FULL / NONE）；null 用 config 缺省
     * @param context         服务上下文
     */
    @BizMutation
    ErpFinBudgetScenario carryForward(@Name("id") Long id,
                                      @Name("targetScenarioId") Long targetScenarioId,
                                      @Name("rule") String rule,
                                      IServiceContext context);
}
