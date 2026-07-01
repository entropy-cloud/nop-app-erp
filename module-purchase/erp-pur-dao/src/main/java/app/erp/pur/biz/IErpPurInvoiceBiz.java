
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurInvoice;

/**
 * 采购发票业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/purchase/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置供应商启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED；执行三单匹配（{@code three-way-match.md}），失败按严格模式拒绝；
 *       成功触发 AP_INVOICE 过账（借费用/采购 + 借进项税 / 贷应付，{@code posted=true}）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置红字冲销已过账凭证）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpPurInvoiceBiz extends ICrudBiz<ErpPurInvoice> {

    @BizMutation
    ErpPurInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpPurInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpPurInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpPurInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpPurInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpPurInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context);
}
