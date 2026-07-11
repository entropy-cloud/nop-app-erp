
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.md.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;

import java.util.List;

/**
 * 付款单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 * 域级核销（settle/reverseSettlement）为独立非审批动作，保留在本接口。
 */
public interface IErpPurPaymentBiz extends ICrudBiz<ErpPurPayment>, IApprovableBiz<ErpPurPayment> {

    @BizMutation
    ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context);

    @BizMutation
    ErpPurPayment settle(@Name("paymentId") Long paymentId,
                         @Name("allocations") List<SettlementAllocation> allocations,
                         IServiceContext context);

    @BizMutation
    ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                    @Name("invoiceId") Long invoiceId,
                                    IServiceContext context);
}
