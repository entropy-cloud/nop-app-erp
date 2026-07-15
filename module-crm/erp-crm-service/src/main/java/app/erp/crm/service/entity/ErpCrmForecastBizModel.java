package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmForecastBiz;
import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.service.support.ForecastAggregator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.List;

/**
 * 销售预测 BizModel。{@link #refreshForecast} 委托 {@link ForecastAggregator} 聚合引擎
 * （commit/upside/best-case/weighted 分类 + 商机级 ForecastLine 快照 + 团队→公司层级 rollup）。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md}。
 */
@BizModel("ErpCrmForecast")
public class ErpCrmForecastBizModel extends CrudBizModel<ErpCrmForecast> implements IErpCrmForecastBiz {

    @Inject
    ForecastAggregator forecastAggregator;

    public ErpCrmForecastBizModel() {
        setEntityName(ErpCrmForecast.class.getName());
    }

    @Override
    @BizMutation
    public void refreshForecast(@Name("periodId") Long periodId, IServiceContext context) {
        forecastAggregator.refreshForecast(periodId, context);
    }

    

}
