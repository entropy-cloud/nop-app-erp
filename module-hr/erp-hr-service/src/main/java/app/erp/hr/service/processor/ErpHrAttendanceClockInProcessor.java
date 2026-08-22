package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

/**
 * ErpHrAttendance clockIn per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含当日打卡签到编排（UC-HR-06）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * 共享 helper 单一真相源在 {@link AbstractErpHrAttendanceProcessor}。
 */
public class ErpHrAttendanceClockInProcessor extends AbstractErpHrAttendanceProcessor {

    public ErpHrAttendance clockIn(String employeeId, IServiceContext context) {
        LocalDate today = CoreMetrics.today();
        ErpHrAttendance attendance = findAttendance(employeeId, today);
        if (attendance == null) {
            attendance = attendanceDao().newEntity();
            attendance.setBusinessDate(today);
            attendance.setEmployeeId(employeeId);
            attendance.setDate(today);
            attendance.setSource(ErpHrConstants.ATTENDANCE_SOURCE_CARD);
            attendance.setIsAbsent(false);
            attendance.setLateMinutes(0);
            attendance.setEarlyLeaveMinutes(0);
        }
        attendance.setClockIn(CoreMetrics.currentTimestamp());
        if (attendance.getClockOut() != null) {
            attendance.setWorkHours(computeWorkHours(
                    attendance.getClockIn().toLocalDateTime(),
                    attendance.getClockOut().toLocalDateTime()));
        }
        saveOrUpdateAttendance(attendance);
        return attendance;
    }
}
