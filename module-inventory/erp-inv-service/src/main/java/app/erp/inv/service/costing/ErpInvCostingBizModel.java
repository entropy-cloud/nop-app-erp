package app.erp.inv.service.costing;

import app.erp.inv.biz.CostingRecloseReport;
import app.erp.inv.biz.IErpInvCostingBiz;
import app.erp.inv.service.costing.ErpInvCostingReclosePeriodCostsProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * 存货成本核算服务 BizModel（Facade）。承载期末成本兜底重算（{@code period-close.md §步骤2}）。
 *
 * <p>{@code @BizModel("ErpInvCosting")} 为独立服务型 BizObject（非实体聚合），由 finance 期末结账经
 * {@code IBizObjectManager.getBizObject("ErpInvCosting")} 跨模块解析调用（finance→inventory R，DAG 合法）。
 *
 * <p>{@link #reclosePeriodCosts} 编排已提取为独立 per-mutation Processor
 * {@link ErpInvCostingReclosePeriodCostsProcessor}（R6.9，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 本 Facade 仅负责入口/事务/委托（{@code @BizMutation} 钉事务边界）。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}、{@code docs/design/finance/period-close.md §步骤2}。
 */
@BizModel("ErpInvCosting")
public class ErpInvCostingBizModel implements IErpInvCostingBiz {

    @Inject
    ErpInvCostingReclosePeriodCostsProcessor reclosePeriodCostsProcessor;

    public ErpInvCostingBizModel() {
    }

    @Override
    @BizMutation
    public CostingRecloseReport reclosePeriodCosts(@Name("periodId") Long periodId,
                                                   @Name("startDate") LocalDate startDate,
                                                   @Name("endDate") LocalDate endDate,
                                                   IServiceContext context) {
        return reclosePeriodCostsProcessor.reclosePeriodCosts(periodId, startDate, endDate, context);
    }
}
