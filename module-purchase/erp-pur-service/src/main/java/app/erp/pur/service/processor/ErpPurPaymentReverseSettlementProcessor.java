package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.entity.PaymentSettler;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpPurPayment reverseSettlement per-mutation Processor（R6.5，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含付款反向核销编排（加载 → 委派 {@link PaymentSettler}）。共享 protected helper（{@code requirePayment}）
 * 单一真相源在 {@link ErpPurPaymentProcessor}，经 {@code @Inject facade} 同包 protected 可达。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPurPaymentReverseSettlementProcessor {

    @Inject
    ErpPurPaymentProcessor facade;

    @Inject
    PaymentSettler paymentSettler;

    public ErpPurPayment reverseSettlement(String id, Long invoiceId, IServiceContext context) {
        ErpPurPayment payment = requirePayment(id, context);
        return doReverseSettlement(payment, invoiceId, context);
    }

    protected ErpPurPayment requirePayment(String id, IServiceContext context) {
        return facade.requirePayment(id, context);
    }

    protected ErpPurPayment doReverseSettlement(ErpPurPayment payment, Long invoiceId, IServiceContext context) {
        return paymentSettler.reverseSettlement(payment, invoiceId);
    }
}
