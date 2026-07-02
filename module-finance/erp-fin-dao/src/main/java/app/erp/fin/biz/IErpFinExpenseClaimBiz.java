
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;

/**
 * 费用报销单 Biz 契约。CRUD 之外承载三轴审批状态机（对齐 {@code expense-claim.md §状态机}
 * 与 finance 域审批形状）：submit/withdrawSubmit/approve/reject/reverseApprove/cancel。
 *
 * <p>审核通过（approve）触发 EXPENSE_CLAIM 业财过账（凭证 + 员工应付辅助账），
 * 并按配置 {@code erp-fin.advance-auto-offset-on-expense} 抵扣同员工未还借款（复用核销机制）。
 * 反审核/作废对已过账单据红字冲销凭证并回滚抵扣核销。
 */
public interface IErpFinExpenseClaimBiz extends ICrudBiz<ErpFinExpenseClaim> {

    @BizMutation
    ErpFinExpenseClaim submit(@Name("claimId") Long claimId, IServiceContext context);

    @BizMutation
    ErpFinExpenseClaim withdrawSubmit(@Name("claimId") Long claimId, IServiceContext context);

    @BizMutation
    ErpFinExpenseClaim approve(@Name("claimId") Long claimId, IServiceContext context);

    @BizMutation
    ErpFinExpenseClaim reject(@Name("claimId") Long claimId, IServiceContext context);

    @BizMutation
    ErpFinExpenseClaim reverseApprove(@Name("claimId") Long claimId, IServiceContext context);

    @BizMutation
    ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context);
}
