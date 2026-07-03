
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.processor.ErpSalQuotationProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 销售报价单 BizModel（Facade）。审批/客户确认状态机 + 报价→订单转化编排委托
 * {@link ErpSalQuotationProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpSalQuotation")
public class ErpSalQuotationBizModel extends CrudBizModel<ErpSalQuotation> implements IErpSalQuotationBiz {

    @Inject
    ErpSalQuotationProcessor quotationProcessor;

    public ErpSalQuotationBizModel() {
        setEntityName(ErpSalQuotation.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalQuotation submit(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.submit(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation withdrawSubmit(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.withdrawSubmit(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation approve(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.approve(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation reject(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.reject(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation reverseApprove(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.reverseApprove(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.cancel(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation confirmCustomerAccepted(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.confirmCustomerAccepted(quotationId, context);
    }

    @Override
    @BizMutation
    public ErpSalOrder convertToOrder(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.convertToOrder(quotationId, context);
    }
}
