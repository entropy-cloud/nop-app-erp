package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.entity.ReceiptSettler;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpSalReceipt reverseSettlement per-mutation Processor（R6.5，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含收款反向核销编排（加载 → 委派 {@link ReceiptSettler}）。共享 protected helper（{@code requireReceipt}）
 * 单一真相源在 {@link ErpSalReceiptProcessor}，经 {@code @Inject facade} 同包 protected 可达。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpSalReceiptReverseSettlementProcessor {

    @Inject
    ErpSalReceiptProcessor facade;

    @Inject
    ReceiptSettler receiptSettler;

    public ErpSalReceipt reverseSettlement(String receiptId, Long invoiceId, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return doReverseSettlement(receipt, invoiceId, context);
    }

    protected ErpSalReceipt requireReceipt(String receiptId, IServiceContext context) {
        return facade.requireReceipt(receiptId, context);
    }

    protected ErpSalReceipt doReverseSettlement(ErpSalReceipt receipt, Long invoiceId, IServiceContext context) {
        return receiptSettler.reverseSettlement(receipt, invoiceId);
    }
}
