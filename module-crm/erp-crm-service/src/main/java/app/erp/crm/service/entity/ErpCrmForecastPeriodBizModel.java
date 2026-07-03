package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmForecastPeriodBiz;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.ForecastAggregator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 预测期间 BizModel。期间状态机：{@code OPEN → FROZEN}（锁定不再重算）/ {@code OPEN → CLOSED}（关闭后触发准确率计算，
 * config-gated {@code erp-crm.forecast.accuracy-auto-compute}）。FROZEN/CLOSED 为终态，拒绝状态回退。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md §状态机}。
 */
@BizModel("ErpCrmForecastPeriod")
public class ErpCrmForecastPeriodBizModel extends CrudBizModel<ErpCrmForecastPeriod>
        implements IErpCrmForecastPeriodBiz {

    @Inject
    ForecastAggregator forecastAggregator;

    public ErpCrmForecastPeriodBizModel() {
        setEntityName(ErpCrmForecastPeriod.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmForecastPeriod freeze(@Name("periodId") Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId, context);
        requireOpen(period);
        period.setStatus(ErpCrmConstants.FORECAST_PERIOD_STATUS_FROZEN);
        dao().updateEntity(period);
        return period;
    }

    @Override
    @BizMutation
    public ErpCrmForecastPeriod closePeriod(@Name("periodId") Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId, context);
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

    protected ErpCrmForecastPeriod requirePeriod(Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = get(String.valueOf(periodId), false, context);
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
}
