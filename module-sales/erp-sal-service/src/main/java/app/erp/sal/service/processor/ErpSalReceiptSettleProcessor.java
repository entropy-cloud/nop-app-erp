package app.erp.sal.service.processor;

import app.erp.md.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.entity.ReceiptSettler;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.List;

/**
 * ErpSalReceipt settle per-mutation Processor（R6.5，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含收款核销编排（加载 → 委派 {@link ReceiptSettler}）。共享 protected helper（{@code requireReceipt}）
 * 单一真相源在 {@link ErpSalReceiptProcessor}，经 {@code @Inject facade} 同包 protected 可达。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpSalReceiptSettleProcessor {

    @Inject
    ErpSalReceiptProcessor facade;

    @Inject
    ReceiptSettler receiptSettler;

    public ErpSalReceipt settle(String receiptId, List<SettlementAllocation> allocations, IServiceContext context) {
        ErpSalReceipt receipt = requireReceipt(receiptId, context);
        return doSettle(receipt, allocations, context);
    }

    protected ErpSalReceipt requireReceipt(String receiptId, IServiceContext context) {
        return facade.requireReceipt(receiptId, context);
    }

    protected ErpSalReceipt doSettle(ErpSalReceipt receipt, List<SettlementAllocation> allocations,
                                     IServiceContext context) {
        return receiptSettler.settle(receipt, allocations);
    }
}
