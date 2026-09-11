package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.payroll.PayrollCalculator;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ErpHrSalary runPayroll per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含全员批量薪酬核算（活跃员工遍历 + 跳过已有非作废薪酬 + PayrollCalculator 计算 + 落库）。
 *
 * <p>P1-CK-hr2-003：逐员工失败隔离（use-cases.md UC-HR-04「员工缺失合同或薪资配置时跳过并告警」）——
 * 循环体 try/catch 收集失败员工，批后单次告警通知（镜像 {@code SalaryPostingDispatcher.dispatchFailureAlert}
 * 的 null-tolerant + degraded 范式），不再整批回滚。返回类型维持 {@code List<ErpHrSalary>}
 * （GraphQL/xbiz 契约与集成测试 C17 锚定）；失败清单仅经日志与通知可见（owner doc 注记边界）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryRunPayrollProcessor extends AbstractErpHrSalaryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpHrSalaryRunPayrollProcessor.class);

    /** 批量核算跳过告警事件（无注册模板时经 notify 内部降级，不阻塞批量）。 */
    static final String NOTIFY_EVENT_SALARY_CALCULATION_SKIPPED = "hr.salary-calculation-skipped";

    @Inject
    PayrollCalculator payrollCalculator;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public List<ErpHrSalary> runPayroll(int year, int month, IServiceContext context) {
        List<ErpHrEmployee> activeEmployees = findActiveEmployees();
        List<ErpHrSalary> result = new ArrayList<>();
        List<String> failedEmployeeIds = new ArrayList<>();
        for (ErpHrEmployee emp : activeEmployees) {
            if (existsNonVoidSalary(emp.getId(), year, month, context)) {
                continue;
            }
            try {
                ErpHrSalary salary = payrollCalculator.calculate(emp.getId(), year, month);
                salaryDao().saveEntity(salary);
                result.add(salary);
            } catch (Exception e) {
                // P1-CK-hr2-003：单员工配置缺失不拖垮整批（UC-HR-04），失败收集后批后告警
                failedEmployeeIds.add(emp.getId());
                LOG.warn("Payroll calculation skipped for employee {} in {}-{}: {}",
                        emp.getId(), year, month, e.getMessage());
            }
        }
        if (!failedEmployeeIds.isEmpty()) {
            dispatchSkipAlert(year, month, failedEmployeeIds, context);
        }
        return result;
    }

    /** 批量核算跳过告警（镜像 dispatchFailureAlert：notificationBiz 缺失或通知失败均降级不阻塞）。 */
    private void dispatchSkipAlert(int year, int month, List<String> failedEmployeeIds, IServiceContext context) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("year", year);
        ctx.put("month", month);
        ctx.put("failedEmployeeIds", String.join(",", failedEmployeeIds));
        ctx.put("failedCount", failedEmployeeIds.size());
        try {
            notificationBiz.notify(NOTIFY_EVENT_SALARY_CALCULATION_SKIPPED, ctx, context);
        } catch (Exception notifyErr) {
            LOG.warn("Payroll skip alert dispatch failed (degraded): year={}, month={}, reason={}",
                    year, month, notifyErr.getMessage());
        }
    }
}
