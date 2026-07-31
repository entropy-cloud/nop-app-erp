package app.erp.crm.service.processor;

import app.erp.crm.service.support.ForecastAggregator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmForecast refreshForecast per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含销售预测刷新编排，委托 {@link ForecastAggregator} 聚合引擎（commit/upside/best-case/weighted 分类 +
 * 商机级 ForecastLine 快照 + 团队→公司层级 rollup）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmForecastRefreshForecastProcessor {

    @Inject
    ForecastAggregator forecastAggregator;

    public void refreshForecast(Long periodId, IServiceContext context) {
        forecastAggregator.refreshForecast(periodId, context);
    }
}
