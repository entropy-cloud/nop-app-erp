
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.ArApAgingRow;
import app.erp.fin.dao.entity.ErpFinArApItem;

import java.time.LocalDate;
import java.util.List;

public interface IErpFinArApItemBiz extends ICrudBiz<ErpFinArApItem>{

    /**
     * 查询指定往来单位在某方向（RECEIVABLE/PAYABLE）下尚未结清（status≠SETTLED/CANCELLED）的辅助账项，
     * 供核销匹配与账龄分析使用。按业务日期升序返回。
     *
     * @param partnerId  往来单位 ID
     * @param direction  方向（10=应收 RECEIVABLE，20=应付 PAYABLE）
     */
    @BizQuery
    List<ErpFinArApItem> findOpenItemsByPartner(@Name("partnerId") Long partnerId,
                                                @Name("direction") Integer direction,
                                                IServiceContext context);

    /**
     * 账龄分析（{@code ar-ap-reconciliation.md §账龄分析}）：按往来单位聚合未核销辅助账，
     * 按 {@code erp-fin.ar-aging-base}(RECEIVABLE) / {@code erp-fin.ap-aging-base}(PAYABLE)
     * 配置的基准日（invoice_date/due_date）计算账龄区间（0-30/31-60/61-90/91-180/180+）。
     *
     * @param direction  方向（10=应收，20=应付），决定采用哪个账龄基准配置
     * @param asOfDate   账龄计算截止日（通常为今天）；为 null 时取当天
     */
    @BizQuery
    List<ArApAgingRow> aging(@Name("direction") Integer direction,
                             @Name("asOfDate") LocalDate asOfDate,
                             IServiceContext context);
}
