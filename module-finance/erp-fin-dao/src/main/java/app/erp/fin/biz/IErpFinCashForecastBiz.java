
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

import app.erp.fin.dao.entity.ErpFinCashForecast;

/**
 * 现金预测 Biz 契约（{@code treasury.md}）。CRUD 之外承载手动触发的批量聚合方法：
 * {@link #refreshForecast} 聚合 AR/AP 辅助账未核销到期项 + 票据到期项，写入 {@link ErpFinCashForecast}
 * （先清区间再写入）。nop-job 定时调度归 Follow-up（{@code treasury.md §规则5} 已明确派生机制）。
 */
public interface IErpFinCashForecastBiz extends ICrudBiz<ErpFinCashForecast> {

    /**
     * 批量刷新现金预测：聚合 ArApItem 未核销到期项（INFLOW=应收到期/OUTFLOW=应付到期）
     * + 票据到期项（应收票据到期 INFLOW/应付票据到期 OUTFLOW），先清区间再写入。
     *
     * @return 区间内新生成的预测行数
     */
    @BizMutation
    Integer refreshForecast(@Name("fromDate") LocalDate fromDate,
                            @Name("toDate") LocalDate toDate,
                            IServiceContext context);
}
