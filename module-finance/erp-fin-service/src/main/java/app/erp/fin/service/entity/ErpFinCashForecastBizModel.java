
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinCashForecastBiz;
import app.erp.fin.dao.entity.ErpFinCashForecast;
import app.erp.fin.service.processor.ErpFinCashForecastRefreshForecastProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * 现金预测 BizModel（{@code treasury.md §现金预测派生}）。{@link #refreshForecast} 委派
 * {@link ErpFinCashForecastRefreshForecastProcessor}（聚合 ArApItem 未核销到期项 + 票据到期项）。
 * nop-job 定时调度归 Follow-up。
 */
@BizModel("ErpFinCashForecast")
public class ErpFinCashForecastBizModel extends CrudBizModel<ErpFinCashForecast> implements IErpFinCashForecastBiz {

    @Inject
    ErpFinCashForecastRefreshForecastProcessor refreshForecastProcessor;

    public ErpFinCashForecastBizModel() {
        setEntityName(ErpFinCashForecast.class.getName());
    }

    @Override
    @BizMutation
    public Integer refreshForecast(@Name("fromDate") LocalDate fromDate,
                                   @Name("toDate") LocalDate toDate,
                                   IServiceContext context) {
        return refreshForecastProcessor.refreshForecast(fromDate, toDate, context);
    }
}
