package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.scheduling.ShiftAttendanceCalculator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpHrShift calcAttendance per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含考勤派生字段计算：读 ShiftAssignment 标准班次 vs ErpHrAttendance 实际打卡，计算迟到/早退/缺勤，
 * 结果写 ErpHrAttendance，同步 ErpHrShiftAssignment.status（shift-scheduling.md §4.1/§4.2）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrShiftProcessor}。
 */
public class ErpHrShiftCalcAttendanceProcessor extends AbstractErpHrShiftProcessor {

    public ErpHrAttendance calcAttendance(String employeeId, LocalDate assignmentDate, IServiceContext context) {
        ErpHrShiftAssignment assignment = assignmentBiz.findByEmployeeAndDate(employeeId, assignmentDate, context);
        if (assignment == null) {
            return null;
        }
        ErpHrShift shift = assignment.getShift();
        if (shift == null) {
            shift = daoProvider.daoFor(ErpHrShift.class).getEntityById(assignment.getShiftId());
        }
        ErpHrAttendance attendance = findAttendanceByDate(employeeId, assignmentDate, context);

        boolean isLeave = assignment.getLeaveRequestId() != null;
        if (isLeave) {
            attendance = upsertAttendanceForLeave(attendance, employeeId, assignmentDate, assignment, context);
            assignment.setIsAbsent(true);
            assignment.setAbsenceReason(ErpHrConstants.ABSENCE_REASON_LEAVE);
            assignment.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
            updateAssignmentStatus(assignment);
            return attendance;
        }

        java.time.LocalDateTime clockIn = attendance != null && attendance.getClockIn() != null ? attendance.getClockIn().toLocalDateTime() : null;
        boolean absentByNoClock = shift != null
                && ShiftAttendanceCalculator.isAbsentByNoClockIn(shift, clockIn);
        if (absentByNoClock) {
            attendance = upsertAttendanceForAbsent(attendance, employeeId, assignmentDate, context);
            assignment.setIsAbsent(true);
            assignment.setAbsenceReason(ErpHrConstants.ABSENCE_REASON_LATE_NOT_CLOCKED);
            assignment.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
            updateAssignmentStatus(assignment);
            return attendance;
        }

        int lateMinutes = 0;
        int earlyLeaveMinutes = 0;
        if (attendance != null && shift != null) {
            lateMinutes = ShiftAttendanceCalculator.calcLateMinutes(shift, attendance.getClockIn() != null ? attendance.getClockIn().toLocalDateTime() : null, assignmentDate);
            earlyLeaveMinutes = ShiftAttendanceCalculator.calcEarlyLeaveMinutes(shift, attendance.getClockOut() != null ? attendance.getClockOut().toLocalDateTime() : null, assignmentDate);
            attendance.setLateMinutes(lateMinutes);
            attendance.setEarlyLeaveMinutes(earlyLeaveMinutes);
            attendance.setIsAbsent(false);
            updateAttendance(attendance);
        }
        // 同步 assignment 状态：有打卡记录视为 PRESENT，否则保持 SCHEDULED
        if (attendance != null && attendance.getClockIn() != null) {
            assignment.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_PRESENT);
            assignment.setIsAbsent(false);
            assignment.setActualStartTime(attendance.getClockIn());
            assignment.setActualEndTime(attendance.getClockOut());
            updateAssignmentStatus(assignment);
        }
        return attendance;
    }

    protected ErpHrAttendance findAttendanceByDate(String employeeId, LocalDate date, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        return attendanceBiz.findFirst(q, null, context);
    }
}
