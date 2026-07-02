package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

/**
 * 存货成本核算服务接口。提供期末成本兜底重算（{@code period-close.md §步骤2}），供 finance 期末结账
 * 经 {@code IBizObjectManager} 跨模块调用（finance→inventory R，DAG 合法）。
 *
 * <p>声明于 dao 层使 finance（{@code erp-fin-service}）可编译依赖 {@code app-erp-inventory-dao} 获得本接口，
 * 运行期 impl（{@code ErpInvCostingBizModel}）由 {@code app-erp-all} 注入；单域 finance 测试无 inv-service 时
 * 经 {@code IBizObjectManager} 解析失败→配置门控告警跳过（对齐 assets 折旧门控范式）。
 */
public interface IErpInvCostingBiz {

    /**
     * 期末成本兜底重算：扫描 {@code [startDate, endDate]} 期间内已过账（DONE）的 FIFO 移动单，
     * 对成本层缺失的入库补建 {@code ErpInvCostLayer}、对 COGS 异常（{@code ledger.unitCost} 空/零）的出库
     * 按 FIFO 重算并刷新 {@code ErpInvStockLedger.unitCost/totalCost}。
     *
     * <p>日期窗口由调用方（finance）从会计期间解析后传入——inventory 不反向依赖 finance（DAG：finance→inventory R）。
     * {@code periodId} 仅用于报告标识。
     *
     * @return 补算报告（扫描单数 + 补算入库层/出库流水条数）
     */
    @BizMutation
    CostingRecloseReport reclosePeriodCosts(@Name("periodId") Long periodId,
                                            @Name("startDate") LocalDate startDate,
                                            @Name("endDate") LocalDate endDate,
                                            IServiceContext context);
}
