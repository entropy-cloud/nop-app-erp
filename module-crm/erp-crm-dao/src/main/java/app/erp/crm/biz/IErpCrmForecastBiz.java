
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmForecast;

/**
 * 销售预测业务接口。除标准 CRUD 外，定义 {@link #refreshForecast} 聚合引擎。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md}（预测重新计算流程）。
 */
public interface IErpCrmForecastBiz extends ICrudBiz<ErpCrmForecast> {

    /**
     * 刷新指定期间的预测：聚合 commit/upside/best-case/weighted + 层级 rollup + 重建商机级 ForecastLine 快照。
     * 仅 OPEN 期间可刷新；FROZEN/CLOSED 抛 {@code ERR_FORECAST_PERIOD_NOT_OPEN}。
     */
    @BizMutation
    void refreshForecast(@Name("periodId") Long periodId, IServiceContext context);
}
