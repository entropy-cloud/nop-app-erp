
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurPaymentBiz;
import app.erp.pur.biz.SettlementAllocation;
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
 * 付款单 BizModel（Facade）。审批状态机 + PAYMENT 过账 + 域级核销编排委托
 * {@link ErpPurPaymentProcessor}（protected step 方法，下游可逐 step 覆盖）。
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
    public ErpPurPayment submit(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.submit(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment withdrawSubmit(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.withdrawSubmit(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment approve(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.approve(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment reject(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.reject(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseApprove(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.reverseApprove(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.cancel(paymentId, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment settle(@Name("paymentId") Long paymentId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return paymentProcessor.settle(paymentId, allocations, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return paymentProcessor.reverseSettlement(paymentId, invoiceId, context);
    }
}
