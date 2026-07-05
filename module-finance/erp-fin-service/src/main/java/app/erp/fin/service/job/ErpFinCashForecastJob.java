package app.erp.fin.service.job;

import app.erp.fin.biz.IErpFinCashForecastBiz;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import io.nop.api.core.time.CoreMetrics;

/**
 * 定时现金预测刷新 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日凌晨）。
 *
 * <p>实际执行门控：{@code erp-fin.cash-forecast-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时按 config-gated 预测窗口派生 [today, today+windowDays] 调
 * {@link IErpFinCashForecastBiz#refreshForecast}（聚合未核销 AR/AP + 票据到期项，先清区间再写入）。
 */
public class ErpFinCashForecastJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpFinCashForecastJob.class);

    @Inject
    IErpFinCashForecastBiz cashForecastBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时按预测窗口刷新现金预测。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-fin-cash-forecast-skipped: cron config empty (erp-fin.cash-forecast-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        LocalDate fromDate = CoreMetrics.today();
        LocalDate toDate = fromDate.plusDays(resolveForecastWindowDays());
        try {
            int generated = runRefreshForecast(fromDate, toDate, ctx);
            LOG.info("erp-fin-cash-forecast-done: from={} to={} rows={}", fromDate, toDate, generated);
        } catch (Exception e) {
            LOG.error("erp-fin-cash-forecast-failed: from={} to={}", fromDate, toDate, e);
        }
    }

    protected int runRefreshForecast(LocalDate fromDate, LocalDate toDate, IServiceContext ctx) {
        Integer generated = cashForecastBiz.refreshForecast(fromDate, toDate, ctx);
        return generated == null ? 0 : generated;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpFinConstants.CONFIG_CASH_FORECAST_CRON, "");
    }

    protected int resolveForecastWindowDays() {
        String raw = AppConfig.var(ErpFinConstants.CONFIG_CASH_FORECAST_WINDOW_DAYS,
                String.valueOf(ErpFinConstants.DEFAULT_CASH_FORECAST_WINDOW_DAYS));
        if (StringHelper.isEmpty(raw)) {
            return ErpFinConstants.DEFAULT_CASH_FORECAST_WINDOW_DAYS;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return ErpFinConstants.DEFAULT_CASH_FORECAST_WINDOW_DAYS;
        }
    }
}
