
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalInvoice;

/**
 * 销售发票业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置客户启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED；成功触发 AR_INVOICE 过账（借应收 / 贷收入 / 贷销项税，
 *       {@code posted=true}）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置红字冲销已过账凭证）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalInvoiceBiz extends ICrudBiz<ErpSalInvoice> {

    @BizMutation
    ErpSalInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpSalInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpSalInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpSalInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpSalInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context);

    @BizMutation
    ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context);
}
