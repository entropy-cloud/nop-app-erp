
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

import app.erp.contract.dao.entity.ErpCtConsumptionLine;

/**
 * 消耗计费行业务接口。除标准 CRUD 外，定义消耗计费周期汇总契约
 * （对齐 {@code docs/design/contract/use-cases.md} UC-CT-04，RC-R1.33 P1-RC-075）：
 *
 * <ul>
 *   <li>{@link #periodSummarize}：周期末汇总 ConsumptionLine 总量对比合同行预估总量（line.quantity）；
 *       超量部分生成额外计费 InvoicePlan + 经 triggerInvoice 生成 AP/AR 发票草稿；
 *       消耗量超预估 120% 时经 IErpSysNotificationBiz 派发超量审批通知（D5 契约）。</li>
 * </ul>
 */
public interface IErpCtConsumptionLineBiz extends ICrudBiz<ErpCtConsumptionLine>{

    /**
     * 消耗计费周期汇总（RC-R1.33 P1-RC-075，UC-CT-04 B/C/D）。
     * 行级聚合入口（D6 裁决：lineId 单行 + 合同级可选循环归 successor）。
     * 超量时生成额外 InvoicePlan（amount 按 D6：overQuantity × line.unitPrice，scale 4 HALF_UP）
     * 并复用 triggerInvoice 路径生成 AP/AR 发票草稿；Σ &gt; 预估 × 1.2 时派发超 120% 通知。
     */
    @BizMutation
    ErpCtConsumptionPeriodSummarizeResult periodSummarize(
            @Name("contractLineId") String contractLineId,
            @Name("fromDate") LocalDate fromDate,
            @Name("toDate") LocalDate toDate,
            @Name("invoiceTerm") String invoiceTerm,
            @Name("planDate") LocalDate planDate,
            IServiceContext context);
}
