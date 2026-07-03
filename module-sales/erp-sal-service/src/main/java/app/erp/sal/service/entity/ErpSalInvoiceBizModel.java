
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.processor.ErpSalInvoiceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 销售发票 BizModel（Facade）。审批状态机 + AR_INVOICE 过账编排委托
 * {@link ErpSalInvoiceProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpSalInvoice")
public class ErpSalInvoiceBizModel extends CrudBizModel<ErpSalInvoice> implements IErpSalInvoiceBiz {

    @Inject
    ErpSalInvoiceProcessor invoiceProcessor;

    public ErpSalInvoiceBizModel() {
        setEntityName(ErpSalInvoice.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.submit(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpSalInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.withdrawSubmit(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpSalInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.approve(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpSalInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.reject(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpSalInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.reverseApprove(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.cancel(invoiceId, context);
    }
}
