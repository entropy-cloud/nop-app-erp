
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
}
