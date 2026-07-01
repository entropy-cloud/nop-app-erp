
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;

/**
 * 销售报价单业务接口。除标准 CRUD 外，定义审核/客户确认状态机 + 报价→订单转化契约（对齐 {@code docs/design/sales/quotation.md}）。
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间（已知缺口，不改 ORM）。
 */
public interface IErpSalQuotationBiz extends ICrudBiz<ErpSalQuotation> {

    @BizMutation
    ErpSalQuotation submit(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation withdrawSubmit(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation approve(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation reject(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation reverseApprove(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation confirmCustomerAccepted(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalOrder convertToOrder(@Name("quotationId") Long quotationId, IServiceContext context);
}
