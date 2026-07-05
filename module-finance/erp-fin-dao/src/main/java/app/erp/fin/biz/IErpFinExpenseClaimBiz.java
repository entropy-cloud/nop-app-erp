
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;

/**
 * 费用报销单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpFinExpenseClaimBiz extends ICrudBiz<ErpFinExpenseClaim>, IApprovableBiz<ErpFinExpenseClaim> {

    @BizMutation
    ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context);
}
