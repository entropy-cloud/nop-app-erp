
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;

import java.math.BigDecimal;

/**
 * 员工借款单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpFinEmployeeAdvanceBiz extends ICrudBiz<ErpFinEmployeeAdvance>, IApprovableBiz<ErpFinEmployeeAdvance> {

    @BizMutation
    ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context);

    /**
     * 员工借款现金还款（{@code EMPLOYEE_ADVANCE_SETTLE} 现金还款路径，plan 2026-07-18-0718-2）。
     *
     * <p>守卫：advance 须存在且 {@code posted=true && approveStatus=APPROVED}；{@code amount > 0}；
     * {@code amount <= outstandingAmount}。更新 {@code settledAmount += amount} / {@code outstandingAmount -= amount}
     * （先持久化字段，对齐 {@code postSettle} 失败不阻断范式），保持 {@code docStatus=APPROVED} 不变（派生投影
     * 表达「已结清」，对齐 owner doc {@code expense-claim.md §还款状态派生}）。委派
     * {@code EmployeeAdvancePostingDispatcher.postCashRepay} 生成现金还款凭证（Dr 1002 / Cr 1221）。
     */
    @BizMutation
    ErpFinEmployeeAdvance cashRepay(@Name("advanceId") Long advanceId,
                                    @Name("amount") BigDecimal amount,
                                    IServiceContext context);
}
