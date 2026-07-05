package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmForecastBiz;
import app.erp.crm.biz.IErpCrmForecastPeriodBiz;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;
import io.nop.api.core.time.CoreMetrics;

/**
 * 定时销售预测重算 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 03:00）。
 *
 * <p>实际执行门控：{@code erp-crm.forecast.recalc-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时查 {@code status=OPEN} 且今天 ∈ [periodStart, periodEnd] 的预测期间，无则 info 跳过，
 * 有则调 {@link IErpCrmForecastBiz#refreshForecast}（聚合 commit/upside/best-case/weighted + 层级 rollup）。
 *
 * <p>期间查询经 {@link IErpCrmForecastPeriodBiz}（ICrudBiz findFirst），符合跨实体 I*Biz 访问约定。
 */
public class ErpCrmForecastRecalcJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCrmForecastRecalcJob.class);

    @Inject
    IErpCrmForecastBiz forecastBiz;
    @Inject
    IErpCrmForecastPeriodBiz forecastPeriodBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时查当前 OPEN 预测期间并刷新。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-crm-forecast-recalc-skipped: cron config empty (erp-crm.forecast.recalc-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        LocalDate today = CoreMetrics.today();
        ErpCrmForecastPeriod period = findOpenPeriod(today, ctx);
        if (period == null) {
            LOG.info("erp-crm-forecast-recalc-skipped: no OPEN period covering today={}", today);
            return;
        }
        try {
            runRefreshForecast(period.getId(), ctx);
            LOG.info("erp-crm-forecast-recalc-done: periodId={} periodLabel={}", period.getId(), period.getLabel());
        } catch (Exception e) {
            LOG.error("erp-crm-forecast-recalc-failed: periodId={}", period.getId(), e);
        }
    }

    protected void runRefreshForecast(Long periodId, IServiceContext ctx) {
        forecastBiz.refreshForecast(periodId, ctx);
    }

    protected ErpCrmForecastPeriod findOpenPeriod(LocalDate today, IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN));
        q.addFilter(le("periodStart", today));
        q.addFilter(ge("periodEnd", today));
        q.setLimit(1);
        return forecastPeriodBiz.findFirst(q, null, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCrmConstants.CONFIG_FORECAST_RECALC_CRON, "");
    }
}
