package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

/**
 * ErpHrAttendance clockOut per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含当日签退编排（UC-HR-06）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * 共享 helper 单一真相源在 {@link AbstractErpHrAttendanceProcessor}。
 */
public class ErpHrAttendanceClockOutProcessor extends AbstractErpHrAttendanceProcessor {

    public ErpHrAttendance clockOut(Long employeeId, IServiceContext context) {
        LocalDate today = CoreMetrics.today();
        ErpHrAttendance attendance = findAttendance(employeeId, today);
        if (attendance == null || attendance.getClockIn() == null) {
            throw new NopException(ErpHrErrors.ERR_NOT_CLOCKED_IN)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        attendance.setClockOut(CoreMetrics.currentTimestamp());
        attendance.setWorkHours(computeWorkHours(attendance.getClockIn().toLocalDateTime(), attendance.getClockOut().toLocalDateTime()));
        saveOrUpdateAttendance(attendance);
        return attendance;
    }
}
