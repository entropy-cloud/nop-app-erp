
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalReceiptBiz;
import app.erp.sal.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.processor.ErpSalReceiptProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 收款单 BizModel（Facade）。审批状态机 + RECEIPT 过账 + 域级核销编排委托
 * {@link ErpSalReceiptProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpSalReceipt")
public class ErpSalReceiptBizModel extends CrudBizModel<ErpSalReceipt> implements IErpSalReceiptBiz {

    @Inject
    ErpSalReceiptProcessor receiptProcessor;

    public ErpSalReceiptBizModel() {
        setEntityName(ErpSalReceipt.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReceipt submit(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.submit(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt withdrawSubmit(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.withdrawSubmit(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt approve(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.approve(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reject(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.reject(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseApprove(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.reverseApprove(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.cancel(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt settle(@Name("receiptId") Long receiptId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return receiptProcessor.settle(receiptId, allocations, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return receiptProcessor.reverseSettlement(receiptId, invoiceId, context);
    }
}
