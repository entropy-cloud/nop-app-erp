
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

import app.erp.contract.dao.entity.ErpCtInvoicePlan;

/**
 * 开票计划业务接口。除标准 CRUD 外，定义 InvoicePlan 触发生成发票契约
 * （对齐 {@code docs/design/contract/state-machine.md} §InvoicePlan 触发）：
 *
 * <ul>
 *   <li>{@link #triggerInvoice}：按合同 contractDirection 经
 *       {@code IErpPurInvoiceBiz}（INBOUND，AP 发票草稿）或 {@code IErpSalInvoiceBiz}（OUTBOUND，AR 发票草稿）
 *       生成发票；成功回写 isInvoiced/invoiceBillCode/invoiceDate。</li>
 *   <li>{@link #triggerDuePlans}：批量查询到期未开票计划，逐行触发（config-gated）。</li>
 *   <li>{@link #generateInvoicePlansByTerm}（RC-R1.33 P1-RC-074）：按生成项批量生成 InvoicePlan
 *       （isInvoiced=false）。生成契约经入参承载 invoiceTerm/planDate（D2/D3 裁决——ORM 中
 *       invoiceTerm 在 Plan 实体而非 Line 实体，Line 无该列；planDate 由调用方显式提供）。</li>
 * </ul>
 *
 * <p>SUSPENDED 合同阻断触发；非 ACTIVE 合同抛
 * {@link io.nop.api.core.exceptions.NopException}；已开票计划抛错。
 */
public interface IErpCtInvoicePlanBiz extends ICrudBiz<ErpCtInvoicePlan> {

    @BizMutation
    ErpCtInvoicePlan triggerInvoice(@Name("planId") String planId, IServiceContext context);

    @BizMutation
    int triggerDuePlans(@Name("contractId") String contractId,
                        @Name("asOfDate") java.time.LocalDate asOfDate,
                        IServiceContext context);

    /**
     * 按生成项批量生成 InvoicePlan（RC-R1.33 P1-RC-074，UC-CT-03 A）。
     * 守卫链：合同 ACTIVE（SUSPENDED 专属拦截）→ 行归属校验（contractLineId ∈ 合同行）→
     * 幂等查重（contractLineId+invoiceTerm+planDate，重复抛
     * {@code ERR_CT_INVOICE_PLAN_DUPLICATE}）。返回生成的计划集合。
     */
    @BizMutation
    List<ErpCtInvoicePlan> generateInvoicePlansByTerm(@Name("contractId") String contractId,
                                                      @Name("items") List<ErpCtInvoicePlanGenerateItem> items,
                                                      IServiceContext context);
}
