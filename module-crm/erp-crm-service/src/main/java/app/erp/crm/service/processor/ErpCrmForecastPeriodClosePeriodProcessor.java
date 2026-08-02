package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.ForecastAggregator;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpCrmForecastPeriod closePeriod per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含预测期间关闭编排（OPEN→CLOSED + config-gated 准确率计算）。{@code freeze} mutation 不在本期范围，仍内联于 BizModel。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmForecastPeriodClosePeriodProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ForecastAggregator forecastAggregator;

    public ErpCrmForecastPeriod closePeriod(Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId);
        requireOpen(period);
        period.setStatus(ErpCrmConstants.FORECAST_PERIOD_STATUS_CLOSED);
        dao().updateEntity(period);

        boolean autoCompute = io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_FORECAST_ACCURACY_AUTO_COMPUTE, Boolean.TRUE);
        if (autoCompute) {
            forecastAggregator.computeAccuracy(periodId, context);
        }
        return period;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmForecastPeriod requirePeriod(Long periodId) {
        ErpCrmForecastPeriod period = dao().getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, periodId);
        }
        return period;
    }

    protected void requireOpen(ErpCrmForecastPeriod period) {
        if (!Objects.equals(period.getStatus(), ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN)) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_OPEN)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, period.getId())
                    .param(ErpCrmErrors.ARG_CURRENT_STATUS, period.getStatus())
                    .param(ErpCrmErrors.ARG_EXPECTED_STATUS, ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
        }
    }

    private IEntityDao<ErpCrmForecastPeriod> dao() {
        return daoProvider.daoFor(ErpCrmForecastPeriod.class);
    }
}
