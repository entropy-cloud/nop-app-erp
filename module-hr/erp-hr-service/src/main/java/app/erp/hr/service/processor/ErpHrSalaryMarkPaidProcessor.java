package app.erp.hr.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpHrSalary markPaid per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含发放标记（审批轴 APPROVED 守卫 + 支付轴 PENDING 守卫 + 业财过账派发 + PAID 翻转 + 落库），薪酬/会计过账语义不变（payroll.md §六/§七）。
 * 固定状态判断委托实体级 StateMachine Bean（契约 §4/§7）：支付轴经
 * {@code ErpHrSalaryPaymentStateMachine.assertCanMarkPaid}（非法边 common 码 → 领域
 * {@link ErpHrErrors#ERR_SALARY_ILLEGAL_STATUS_TRANSITION} + cause 保留）；审批轴交叉守卫经
 * {@code ErpHrSalaryApprovalStateMachine.assertCanMarkPaid}（一致性，非迁移边）。
 * 动态业务守卫（SALARY_PAYMENT 过账、银行文件生成）保留原位。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryMarkPaidProcessor extends AbstractErpHrSalaryProcessor {

    @Inject
    SalaryPostingDispatcher postingDispatcher;

    public ErpHrSalary markPaid(Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        try {
            approvalStateMachine.assertCanMarkPaid(salary.getApproveStatus());
        } catch (NopException e) {
            throw illegalApproveTransition(salaryId, salary.getApproveStatus(), e);
        }
        try {
            paymentStateMachine.assertCanMarkPaid(salary.getPaymentStatus());
        } catch (NopException e) {
            throw illegalPaymentTransition(salaryId, salary.getPaymentStatus(), e);
        }
        postingDispatcher.tryPostPayment(salary);
        salary = requireSalary(salaryId, context);
        salary.setPaymentStatus(paymentStateMachine.markPaidTargetStatus());
        salary.setPaymentDate(CoreMetrics.today());
        salaryDao().updateEntity(salary);
        return salary;
    }

    private static NopException illegalApproveTransition(Long salaryId, String currentStatus, NopException cause) {
        return new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                .param(ErpHrErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, cause.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
    }

    private static NopException illegalPaymentTransition(Long salaryId, String currentStatus, NopException cause) {
        return new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                .param(ErpHrErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, cause.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
    }
}
