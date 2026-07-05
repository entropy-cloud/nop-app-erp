
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.sal.dao.entity.ErpSalInvoice;

/**
 * 销售发票业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>审批状态机（对齐 {@code docs/design/sales/state-machine.md}）：approve 成功触发 AR_INVOICE 过账
 * （借应收 / 贷收入 / 贷销项税，{@code posted=true}）。reverseApprove 前置红字冲销已过账凭证。
 * 每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalInvoiceBiz extends ICrudBiz<ErpSalInvoice>, IApprovableBiz<ErpSalInvoice> {

    @BizMutation
    ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context);
}
