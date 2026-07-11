
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurPaymentBiz;
import app.erp.md.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.processor.ErpPurPaymentProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 付款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel/settle/reverseSettlement）
 * 在本类完成 Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurPayment")
public class ErpPurPaymentBizModel extends CrudBizModel<ErpPurPayment> implements IErpPurPaymentBiz {

    @Inject
    ErpPurPaymentProcessor paymentProcessor;

    public ErpPurPaymentBizModel() {
        setEntityName(ErpPurPayment.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.cancel(String.valueOf(paymentId), context);
    }

    @Override
    @BizMutation
    public ErpPurPayment settle(@Name("paymentId") Long paymentId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return paymentProcessor.settle(String.valueOf(paymentId), allocations, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return paymentProcessor.reverseSettlement(String.valueOf(paymentId), invoiceId, context);
    }
}
