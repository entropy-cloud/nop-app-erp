package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation confirmCustomerAccepted per-mutation Processor（R6.5，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含报价单客户接受确认编排（加载 → 非作废 → 状态迁移 → 未过期 → 标记 isAccepted）。
 * 共享 protected helper 单一真相源在 {@link ErpSalQuotationProcessor}，经 {@code @Inject facade} 同包 protected 可达。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpSalQuotationConfirmCustomerAcceptedProcessor {

    @Inject
    ErpSalQuotationProcessor facade;

    public ErpSalQuotation confirmCustomerAccepted(String quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateNotCancelled(quotation, context);
        validateTransitionForConfirm(quotation, context);
        requireNotExpired(quotation, context);
        doConfirmCustomerAccepted(quotation, context);
        return quotation;
    }

    protected ErpSalQuotation requireQuotation(String quotationId, IServiceContext context) {
        return facade.requireQuotation(quotationId, context);
    }

    protected void validateNotCancelled(ErpSalQuotation quotation, IServiceContext context) {
        facade.validateNotCancelled(quotation, context);
    }

    protected void validateTransitionForConfirm(ErpSalQuotation quotation, IServiceContext context) {
        facade.validateTransitionForConfirm(quotation, context);
    }

    protected void requireNotExpired(ErpSalQuotation quotation, IServiceContext context) {
        facade.requireNotExpired(quotation, context);
    }

    protected void doConfirmCustomerAccepted(ErpSalQuotation quotation, IServiceContext context) {
        facade.doConfirmCustomerAccepted(quotation, context);
    }
}
