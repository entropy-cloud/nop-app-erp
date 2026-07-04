
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

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
 * </ul>
 *
 * <p>SUSPENDED 合同阻断触发；非 ACTIVE 合同抛
 * {@link io.nop.api.core.exceptions.NopException}；已开票计划抛错。
 */
public interface IErpCtInvoicePlanBiz extends ICrudBiz<ErpCtInvoicePlan> {

    @BizMutation
    ErpCtInvoicePlan triggerInvoice(@Name("planId") Long planId, IServiceContext context);

    @BizMutation
    int triggerDuePlans(@Name("contractId") Long contractId,
                        @Name("asOfDate") java.time.LocalDate asOfDate,
                        IServiceContext context);
}
