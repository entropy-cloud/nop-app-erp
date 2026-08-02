package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.payroll.PayrollCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * ErpHrSalary runPayroll per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含全员批量薪酬核算（活跃员工遍历 + 跳过已有非作废薪酬 + PayrollCalculator 计算 + 落库），薪酬语义不变（payroll.md §五）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryRunPayrollProcessor extends AbstractErpHrSalaryProcessor {

    @Inject
    PayrollCalculator payrollCalculator;

    public List<ErpHrSalary> runPayroll(int year, int month, IServiceContext context) {
        List<ErpHrEmployee> activeEmployees = findActiveEmployees();
        List<ErpHrSalary> result = new ArrayList<>();
        for (ErpHrEmployee emp : activeEmployees) {
            if (existsNonVoidSalary(emp.getId(), year, month, context)) {
                continue;
            }
            ErpHrSalary salary = payrollCalculator.calculate(emp.getId(), year, month);
            salaryDao().saveEntity(salary);
            result.add(salary);
        }
        return result;
    }
}
