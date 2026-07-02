
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;

/**
 * 员工借款单 Biz 契约。CRUD 之外承载三轴审批状态机（与报销单同形）：submit/withdrawSubmit/
 * approve/reject/reverseApprove/cancel。
 *
 * <p>审核通过（approve）触发 EMPLOYEE_ADVANCE 业财过账（凭证 + 员工预支应收辅助账）。
 * 借款清算经「报销抵扣」净额核销（复用 {@code IErpFinReconciliationBiz}）+ EMPLOYEE_ADVANCE_SETTLE 凭证实现，
 * 编排在报销审核侧（见 {@code IErpFinExpenseClaimBiz.approve}）。
 */
public interface IErpFinEmployeeAdvanceBiz extends ICrudBiz<ErpFinEmployeeAdvance> {

    @BizMutation
    ErpFinEmployeeAdvance submit(@Name("advanceId") Long advanceId, IServiceContext context);

    @BizMutation
    ErpFinEmployeeAdvance withdrawSubmit(@Name("advanceId") Long advanceId, IServiceContext context);

    @BizMutation
    ErpFinEmployeeAdvance approve(@Name("advanceId") Long advanceId, IServiceContext context);

    @BizMutation
    ErpFinEmployeeAdvance reject(@Name("advanceId") Long advanceId, IServiceContext context);

    @BizMutation
    ErpFinEmployeeAdvance reverseApprove(@Name("advanceId") Long advanceId, IServiceContext context);

    @BizMutation
    ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context);
}
