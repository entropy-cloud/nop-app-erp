
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmForecastPeriod;

/**
 * 预测期间业务接口。除标准 CRUD 外，定义期间状态机（{@link #freeze} / {@link #closePeriod}）。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md §状态机}：{@code OPEN → FROZEN}（锁定不再重算）/ {@code OPEN → CLOSED}（关闭后算准确率）。
 */
public interface IErpCrmForecastPeriodBiz extends ICrudBiz<ErpCrmForecastPeriod> {

    /**
     * 冻结期间（OPEN → FROZEN），锁定预测数据不再重算。
     */
    @BizMutation
    ErpCrmForecastPeriod freeze(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 关闭期间（OPEN → CLOSED），触发预测准确率计算（config-gated {@code erp-crm.forecast.accuracy-auto-compute}）。
     */
    @BizMutation
    ErpCrmForecastPeriod closePeriod(@Name("periodId") Long periodId, IServiceContext context);
}
