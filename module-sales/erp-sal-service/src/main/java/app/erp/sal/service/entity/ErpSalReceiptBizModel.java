
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
 * 收款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalReceiptProcessor#onSubmit}/{@link ErpSalReceiptProcessor#onApproved}/{@link ErpSalReceiptProcessor#onReverseApproved}。
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
    public ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.cancel(String.valueOf(receiptId), context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt settle(@Name("receiptId") Long receiptId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return receiptProcessor.settle(String.valueOf(receiptId), allocations, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return receiptProcessor.reverseSettlement(String.valueOf(receiptId), invoiceId, context);
    }
}
