
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalInvoiceBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.service.processor.ErpSalInvoiceCancelProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 销售发票 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@code ErpSalInvoiceProcessor}。
 * 非审批动作（cancel）经 per-mutation {@link ErpSalInvoiceCancelProcessor}。
 */
@BizModel("ErpSalInvoice")
public class ErpSalInvoiceBizModel extends CrudBizModel<ErpSalInvoice> implements IErpSalInvoiceBiz {

    @Inject
    ErpSalInvoiceCancelProcessor cancelProcessor;

    public ErpSalInvoiceBizModel() {
        setEntityName(ErpSalInvoice.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalInvoice cancel(@Name("invoiceId") Long invoiceId, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(invoiceId), context);
    }

}
