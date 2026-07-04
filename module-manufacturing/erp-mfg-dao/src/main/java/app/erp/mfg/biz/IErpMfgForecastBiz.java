
package app.erp.mfg.biz;

import app.erp.mfg.dao.entity.ErpMfgForecast;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpMfgForecastBiz extends ICrudBiz<ErpMfgForecast> {

    /**
     * 审批预测：DRAFT→APPROVED。仅 APPROVED 状态的预测行进入 MRP/DRP 引擎消费。
     * 权威：{@code docs/design/manufacturing/mrp.md} §预测来源、plan 2026-07-05-0427-1 §Goals。
     */
    @BizMutation
    ErpMfgForecast approve(@Name("id") String id, IServiceContext context);

    /**
     * 取消预测：DRAFT/APPROVED→CANCELLED（终态）。CANCELLED 行不进入引擎消费。
     */
    @BizMutation
    ErpMfgForecast cancel(@Name("id") String id, IServiceContext context);
}
