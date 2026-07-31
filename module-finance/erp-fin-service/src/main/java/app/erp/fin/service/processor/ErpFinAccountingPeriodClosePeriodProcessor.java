package app.erp.fin.service.processor;

import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.annualclose.AnnualCloseService;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod closePeriod per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含期末结账编排（{@code period-close.md §期末结账步骤 / §年度结转规则}）；共享 protected helper 单一真相源在
 * {@link ErpFinAccountingPeriodProcessor}。前置检查复用 {@link ErpFinAccountingPeriodPreCheckProcessor}，
 * 年度结转期间生成复用 {@link ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类（含 {@link #closeAnnual} 单步）。
 */
public class ErpFinAccountingPeriodClosePeriodProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;
    @Inject
    ErpFinAccountingPeriodPreCheckProcessor preCheckProcessor;
    @Inject
    ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor generateNextYearPeriodsProcessor;
    @Inject
    AnnualCloseService annualCloseService;

    public ErpFinAccountingPeriod closePeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        facade.assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_OPEN, "结账");

        PeriodPreCheckReport report = preCheckProcessor.preCheck(periodId, context);
        // Allowance shortfall 是独立硬阻断——不受 auto-post-on-close 影响（bad-debt.md §期末 allowance 充足性门控）。
        if (report.hasAllowanceShortfall()) {
            throw new NopException(ErpFinErrors.ERR_PRE_CHECK_BLOCKED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_ISSUE_COUNT, 1);
        }
        // 未核销 AR-AP 为结构化提示（hasReminders），不阻断结账（owner doc §结账前置检查「未核销=提示」）。
        // 未过账凭证 + 未处置异常：auto-post-on-close=false 时阻断（安全默认），=true 时降级为提示。
        if (!facade.isAutoPostOnClose() && report.hasIssues()) {
            throw new NopException(ErpFinErrors.ERR_PRE_CHECK_BLOCKED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_ISSUE_COUNT, report.issueCount());
        }

        ErpFinAccountingPeriodStatus status = facade.findOrCreatePeriodStatus(period);
        // 模块按序关账：AR→AP→INV→AST→GL。INV 内运行存货成本兜底重算，AST 内运行折旧，GL 内运行汇兑重估→损益结转（均在期间仍 OPEN 时）。
        facade.advanceModule(status, ErpFinAccountingPeriodProcessor.Module.AR);
        facade.advanceModule(status, ErpFinAccountingPeriodProcessor.Module.AP);
        facade.closeInvModule(period, status, context);
        facade.closeAssetModule(period, status, context);
        facade.closeGlModule(period, status, context);

        // 年度结转分支（period-close.md §年度结转规则）：12 月/年末结账时，常规月度结账后追加——
        // 辅助账对账门控 → 本年利润→未分配利润结转 → populate 次年年初余额 → 触发次年期间创建。
        // config-gated erp-fin.annual-close-enabled。
        if (facade.isAnnualCloseEnabled() && facade.isYearEnd(period)) {
            closeAnnual(period, status, context);
        }

        // 期末凭证生成完成（期间仍 OPEN）后，状态簿记：CLOSING→CLOSED。flush 落库。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSING);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED);
        period.setClosedAt(CoreMetrics.currentTimestamp());
        period.setClosedBy(facade.currentUserId());
        facade.orm().flushSession();
        return period;
    }

    /**
     * 年度结转追加步骤（{@code period-close.md §年度结转规则} 步骤3-5）。各 step 为 protected 供下游覆盖：
     * <ol>
     *   <li>{@link #assertAuxiliaryReconciles}——辅助账跨年对账门控（config-gated）；</li>
     *   <li>{@link AnnualCloseService#executeAnnualClose}——本年利润→未分配利润结转 + 次年年初余额 populate；</li>
     *   <li>{@link ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor#generateNextYearPeriods}——次年 12 期间创建（config-gated 是否自动触发）。</li>
     * </ol>
     * 执行顺序：先创建次年期间（使 populate 年初余额有目标期间），再执行结转与 populate。
     */
    protected void closeAnnual(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                               IServiceContext context) {
        // 步骤4 对账门控（结转前校验辅助账与总账一致）。
        assertAuxiliaryReconciles(period);
        // 步骤5 次年期间创建（先于 populate，使年初余额有目标期间）。
        if (facade.isAutoGenerateNextYearPeriods() && period.getYear() != null) {
            generateNextYearPeriodsProcessor.generateNextYearPeriods(period.getYear() + 1, context);
        }
        // 步骤3 本年利润→未分配利润 + 步骤4 年初余额 populate。
        annualCloseService.executeAnnualClose(period, context);
    }

    protected void assertAuxiliaryReconciles(ErpFinAccountingPeriod period) {
        if (facade.isAuxiliaryReconGateEnabled()) {
            annualCloseService.assertAuxiliaryReconciles(period);
        }
    }
}
