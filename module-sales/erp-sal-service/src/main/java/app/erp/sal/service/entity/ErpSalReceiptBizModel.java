
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalReceiptBiz;
import app.erp.md.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.processor.ErpSalReceiptCancelProcessor;
import app.erp.sal.service.processor.ErpSalReceiptReverseSettlementProcessor;
import app.erp.sal.service.processor.ErpSalReceiptSettleProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 收款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalReceiptProcessor#onSubmit}/{@link ErpSalReceiptProcessor#onApproved}/{@link ErpSalReceiptProcessor#onReverseApproved}。
 */
@BizModel("ErpSalReceipt")
public class ErpSalReceiptBizModel extends AbstractErpCrudBizModel<ErpSalReceipt> implements IErpSalReceiptBiz {

    @Inject
    ErpSalReceiptSettleProcessor settleProcessor;

    @Inject
    ErpSalReceiptReverseSettlementProcessor reverseSettlementProcessor;

    @Inject
    ErpSalReceiptCancelProcessor cancelProcessor;

    public ErpSalReceiptBizModel() {
        setEntityName(ErpSalReceipt.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReceipt cancel(@Name("receiptId") String receiptId, IServiceContext context) {
        return cancelProcessor.cancel(receiptId, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt settle(@Name("receiptId") String receiptId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return settleProcessor.settle(receiptId, allocations, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseSettlement(@Name("receiptId") String receiptId,
                                           @Name("invoiceId") String invoiceId,
                                           IServiceContext context) {
        return reverseSettlementProcessor.reverseSettlement(receiptId, invoiceId, context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
