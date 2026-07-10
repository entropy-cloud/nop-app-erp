
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
}
