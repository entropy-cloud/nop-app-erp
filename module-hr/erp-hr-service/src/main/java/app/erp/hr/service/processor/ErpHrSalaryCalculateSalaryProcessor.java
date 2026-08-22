package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.payroll.PayrollCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpHrSalary calculateSalary per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单员工薪酬核算（去重校验 + PayrollCalculator 计算 + 落库），薪酬语义不变（payroll.md §五）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryCalculateSalaryProcessor extends AbstractErpHrSalaryProcessor {

    @Inject
    PayrollCalculator payrollCalculator;

    public ErpHrSalary calculateSalary(String employeeId, int year, int month, IServiceContext context) {
        assertNotDuplicated(employeeId, year, month, context);
        ErpHrSalary salary = payrollCalculator.calculate(employeeId, year, month);
        salaryDao().saveEntity(salary);
        return salary;
    }
}
