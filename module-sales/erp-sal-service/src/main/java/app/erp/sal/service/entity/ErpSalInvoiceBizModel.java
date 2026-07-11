
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.processor.ErpSalInvoiceProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 销售发票 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalInvoiceProcessor#onSubmit}/{@link ErpSalInvoiceProcessor#onApproved}/{@link ErpSalInvoiceProcessor#onReverseApproved}。
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
    public ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.cancel(String.valueOf(invoiceId), context);
    }

    @BizLoader(forType = ErpSalInvoice.class)
    public List<String> customerName(@ContextSource List<ErpSalInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("customer"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpSalInvoice inv : invoices) {
            result.add(inv.getCustomer() != null ? inv.getCustomer().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalInvoice.class)
    public List<String> currencyName(@ContextSource List<ErpSalInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpSalInvoice inv : invoices) {
            result.add(inv.getCurrency() != null ? inv.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalInvoice.class)
    public List<String> orgName(@ContextSource List<ErpSalInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("org"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpSalInvoice inv : invoices) {
            result.add(inv.getOrg() != null ? inv.getOrg().getName() : null);
        }
        return result;
    }
}
