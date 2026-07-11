
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurInvoiceBiz;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.processor.ErpPurInvoiceProcessor;
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
 * 采购发票 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel）在本类完成
 * Long→String 转换后委托 Processor。
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
    public ErpPurInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return invoiceProcessor.cancel(String.valueOf(invoiceId), context);
    }

    @BizLoader(forType = ErpPurInvoice.class)
    public List<String> supplierName(@ContextSource List<ErpPurInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpPurInvoice inv : invoices) {
            result.add(inv.getSupplier() != null ? inv.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurInvoice.class)
    public List<String> currencyName(@ContextSource List<ErpPurInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpPurInvoice inv : invoices) {
            result.add(inv.getCurrency() != null ? inv.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurInvoice.class)
    public List<String> orgName(@ContextSource List<ErpPurInvoice> invoices) {
        orm().batchLoadProps(invoices, Collections.singleton("org"));
        List<String> result = new ArrayList<>(invoices.size());
        for (ErpPurInvoice inv : invoices) {
            result.add(inv.getOrg() != null ? inv.getOrg().getName() : null);
        }
        return result;
    }
}
