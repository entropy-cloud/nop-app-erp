package app.erp.fin.service.processor;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod reverseClose per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含反结账编排（{@code period-close.md §反结账流程}）；共享 protected helper 单一真相源在
 * {@link ErpFinAccountingPeriodProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinAccountingPeriodReverseCloseProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;

    public ErpFinAccountingPeriod reverseClose(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        facade.assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, "反结账");

        if (facade.isReverseCloseApprovalRequired()) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_CLOSE_APPROVAL_REQUIRED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }

        // 年度结转反结账门控：若该期间为年末且次年期间已创建，阻止反结账（须先删次年期间）。
        if (facade.isYearEnd(period) && period.getYear() != null && facade.hasNextYearPeriods(period.getYear() + 1)) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_NEXT_YEAR, period.getYear() + 1);
        }

        // 先回开期间为 OPEN，使红冲可经引擎过账（resolveOpenPeriod 要求 OPEN）。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);

        // 冲销本期结转 / 汇兑 / 年度结转（及条件折旧）凭证（红字）。
        facade.reverseCloseVoucher(period, ErpFinAccountingPeriodProcessor.PL_BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.PERIOD_CLOSE, context);
        facade.reverseCloseVoucher(period, ErpFinAccountingPeriodProcessor.FX_BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS, context);
        if (facade.isYearEnd(period)) {
            facade.reverseCloseVoucher(period, ErpFinAccountingPeriodProcessor.ANNUAL_BILL_CODE_PREFIX + period.getCode(),
                    ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS, context);
        }
        if (facade.isAutoDepreciationOnClose()) {
            facade.reverseDepreciation(period, context);
        }

        // 回开各模块状态。
        ErpFinAccountingPeriodStatus status = facade.findOrCreatePeriodStatus(period);
        facade.reopenModules(status);
        facade.orm().flushSession();
        return period;
    }
}
