
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.pur.dao.entity.ErpPurInvoice;

/**
 * 采购发票业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpPurInvoiceBiz extends ICrudBiz<ErpPurInvoice>, IApprovableBiz<ErpPurInvoice> {

    @BizMutation
    ErpPurInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context);
}
