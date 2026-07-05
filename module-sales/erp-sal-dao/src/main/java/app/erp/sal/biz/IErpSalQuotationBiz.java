
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;

/**
 * 销售报价单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间（已知缺口，不改 ORM）。
 */
public interface IErpSalQuotationBiz extends ICrudBiz<ErpSalQuotation>, IApprovableBiz<ErpSalQuotation> {

    @BizMutation
    ErpSalQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalQuotation confirmCustomerAccepted(@Name("quotationId") Long quotationId, IServiceContext context);

    @BizMutation
    ErpSalOrder convertToOrder(@Name("quotationId") Long quotationId, IServiceContext context);
}
