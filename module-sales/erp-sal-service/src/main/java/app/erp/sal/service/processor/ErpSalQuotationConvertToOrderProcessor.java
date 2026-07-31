package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation convertToOrder per-mutation Processor（R6.5，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含报价单转订单编排（加载 → 转化就绪 → 未过期 → 未重复转化 → 建单 → 标记已接受）。
 * 共享 protected helper 单一真相源在 {@link ErpSalQuotationProcessor}，经 {@code @Inject facade} 同包 protected 可达。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpSalQuotationConvertToOrderProcessor {

    @Inject
    ErpSalQuotationProcessor facade;

    public ErpSalOrder convertToOrder(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateReadyForConvert(quotation, context);
        requireNotExpired(quotation, context);
        validateNotAlreadyConverted(quotation, context);
        ErpSalOrder order = createOrderFromQuotation(quotation, context);
        markQuotationAccepted(quotationId, context);
        return order;
    }

    protected ErpSalQuotation requireQuotation(String quotationId, IServiceContext context) {
        return facade.requireQuotation(quotationId, context);
    }

    protected void validateReadyForConvert(ErpSalQuotation quotation, IServiceContext context) {
        facade.validateReadyForConvert(quotation, context);
    }

    protected void requireNotExpired(ErpSalQuotation quotation, IServiceContext context) {
        facade.requireNotExpired(quotation, context);
    }

    protected void validateNotAlreadyConverted(ErpSalQuotation quotation, IServiceContext context) {
        facade.validateNotAlreadyConverted(quotation, context);
    }

    protected ErpSalOrder createOrderFromQuotation(ErpSalQuotation quotation, IServiceContext context) {
        return facade.createOrderFromQuotation(quotation, context);
    }

    protected void markQuotationAccepted(String quotationId, IServiceContext context) {
        facade.markQuotationAccepted(quotationId, context);
    }
}
