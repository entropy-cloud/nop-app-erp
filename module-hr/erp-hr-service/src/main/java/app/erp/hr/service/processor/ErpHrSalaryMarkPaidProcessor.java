package app.erp.hr.service.processor;

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
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryMarkPaidProcessor extends AbstractErpHrSalaryProcessor {

    @Inject
    SalaryPostingDispatcher postingDispatcher;

    public ErpHrSalary markPaid(Long salaryId, IServiceContext context) {
        ErpHrSalary salary = requireSalary(salaryId, context);
        if (!ErpHrConstants.APPROVE_STATUS_APPROVED.equals(salary.getApproveStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getApproveStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "APPROVED");
        }
        if (!ErpHrConstants.PAYMENT_PENDING.equals(salary.getPaymentStatus())) {
            throw new NopException(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_SALARY_ID, salaryId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, salary.getPaymentStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, "PENDING(paymentStatus)");
        }
        postingDispatcher.tryPostPayment(salary);
        salary = requireSalary(salaryId, context);
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_PAID);
        salary.setPaymentDate(CoreMetrics.today());
        salaryDao().updateEntity(salary);
        return salary;
    }
}
