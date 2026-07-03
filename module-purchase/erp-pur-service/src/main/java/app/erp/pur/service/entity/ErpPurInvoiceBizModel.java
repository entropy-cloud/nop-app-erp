
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurInvoiceBiz;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.processor.ErpPurInvoiceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 采购发票 BizModel（Facade）。审批状态机 + 三单匹配 + AP_INVOICE 过账编排委托
 * {@link ErpPurInvoiceProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpPurInvoice")
public class ErpPurInvoiceBizModel extends CrudBizModel<ErpPurInvoice> implements IErpPurInvoiceBiz {

    @Inject
    ErpPurInvoiceProcessor invoiceProcessor;

    public ErpPurInvoiceBizModel() {
        setEntityName(ErpPurInvoice.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurInvoice submit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.submit(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpPurInvoice withdrawSubmit(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.withdrawSubmit(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpPurInvoice approve(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.approve(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpPurInvoice reject(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.reject(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpPurInvoice reverseApprove(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.reverseApprove(invoiceId, context);
    }

    @Override
    @BizMutation
    public ErpPurInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.cancel(invoiceId, context);
    }
}
