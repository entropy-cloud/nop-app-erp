
package app.erp.fin.biz;

import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

/**
 * 期末结账编排契约（{@code period-close.md §期末结账步骤}）。承载期间状态机四态推进
 * （OPEN→CLOSING→CLOSED→CLOSED_FINAL）、前置检查、五模块（AR/AP/INV/AST/GL）按序关账编排、
 * 损益结转/汇兑重估/折旧集成，以及反结账。
 *
 * <p>由会计期间聚合根 Biz {@link IErpFinAccountingPeriodBiz} 继承实现（结账是期间实体的操作）。
 */
public interface IErpFinPeriodCloseBiz {

    /**
     * 期末结账前置检查：扫描本期未过账凭证、未核销应收应付，产出检查报告（不阻断）。
     * 是否阻断由 {@code closePeriod} 按 {@code erp-fin.auto-post-on-close} 决定。
     */
    @BizQuery
    PeriodPreCheckReport preCheck(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 结账：前置检查 → 期末处理（折旧/汇兑重估/损益结转）→ 模块按序关账 → 期间 OPEN→CLOSED。
     * 要求期间当前为 OPEN。
     */
    @BizMutation
    ErpFinAccountingPeriod closePeriod(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 最终锁定：期间 CLOSED→CLOSED_FINAL。要求期间当前为 CLOSED。
     */
    @BizMutation
    ErpFinAccountingPeriod finalizePeriod(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 反结账：期间 CLOSED_FINAL→OPEN，冲销本期结转/汇兑（及条件折旧）凭证，回开各模块。
     * 受 {@code erp-fin.reverse-close-approval-required} 配置门控。
     */
    @BizMutation
    ErpFinAccountingPeriod reverseClose(@Name("periodId") Long periodId, IServiceContext context);
}
