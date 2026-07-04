
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.scheduling.ShiftAttendanceCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 班次模板 + 考勤计算聚合根 BizModel（shift-scheduling.md §一/§四）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展 {@link #calcAttendance} 跨实体读 ShiftAssignment/Attendance 计算迟到/早退/缺勤，结果写 Attendance 已有列。
 *
 * <p>跨实体访问经注入 {@link IErpHrShiftAssignmentBiz} / {@link IErpHrAttendanceBiz}（同域 I*Biz），
 * 不直接操作 daoProvider（除了 ErpHrAttendance 的 findExistingByDate 辅助）。
 */
@BizModel("ErpHrShift")
public class ErpHrShiftBizModel extends CrudBizModel<ErpHrShift> implements IErpHrShiftBiz {

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;
    @Inject
    IErpHrAttendanceBiz attendanceBiz;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;

    public ErpHrShiftBizModel() {
        setEntityName(ErpHrShift.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrAttendance calcAttendance(@Name("employeeId") Long employeeId,
                                          @Name("assignmentDate") LocalDate assignmentDate,
                                          IServiceContext context) {
        ErpHrShiftAssignment assignment = assignmentBiz.findByEmployeeAndDate(employeeId, assignmentDate, context);
        if (assignment == null) {
            return null;
        }
        ErpHrShift shift = assignment.getShift();
        if (shift == null) {
            shift = get(String.valueOf(assignment.getShiftId()), false, context);
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

        java.time.LocalDateTime clockIn = attendance != null ? attendance.getClockIn() : null;
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
            lateMinutes = ShiftAttendanceCalculator.calcLateMinutes(shift, attendance.getClockIn(), assignmentDate);
            earlyLeaveMinutes = ShiftAttendanceCalculator.calcEarlyLeaveMinutes(shift, attendance.getClockOut(), assignmentDate);
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

    @Override
    @BizQuery
    public ErpHrAttendance findAttendanceByDate(@Name("employeeId") Long employeeId,
                                                @Name("date") LocalDate date,
                                                IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        return attendanceBiz.findFirst(q, null, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public void onLeaveApproved(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeaveRequest(leaveRequestId, context);
        List<ErpHrShiftAssignment> assignments = findAssignmentsByEmployeeRange(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate(), context);
        for (ErpHrShiftAssignment a : assignments) {
            a.setIsAbsent(true);
            a.setAbsenceReason(ErpHrConstants.ABSENCE_REASON_LEAVE);
            a.setLeaveRequestId(leaveRequestId);
            a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
            updateAssignmentStatus(a);
        }
    }

    @Override
    @BizMutation
    @SingleSession
    public void onLeaveCancelled(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = requireLeaveRequest(leaveRequestId, context);
        List<ErpHrShiftAssignment> assignments = findAssignmentsByEmployeeRange(
                leave.getEmployeeId(), leave.getStartDate(), leave.getEndDate(), context);
        for (ErpHrShiftAssignment a : assignments) {
            // 仅解除由该休假标记的（leaveRequestId 匹配）
            if (leaveRequestId.equals(a.getLeaveRequestId())) {
                a.setIsAbsent(false);
                a.setAbsenceReason(null);
                a.setLeaveRequestId(null);
                a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
                updateAssignmentStatus(a);
            }
        }
    }

    ErpHrLeaveRequest requireLeaveRequest(Long leaveRequestId, IServiceContext context) {
        ErpHrLeaveRequest leave = leaveRequestBiz.get(String.valueOf(leaveRequestId), false, context);
        if (leave == null) {
            throw new NopException(ErpHrErrors.ERR_LEAVE_REQUEST_NOT_FOUND)
                    .param(ErpHrErrors.ARG_LEAVE_REQUEST_ID, leaveRequestId);
        }
        return leave;
    }

    // ---------- helpers ----------

    ErpHrAttendance upsertAttendanceForAbsent(ErpHrAttendance existing, Long employeeId, LocalDate date,
                                              IServiceContext context) {
        if (existing == null) {
            existing = newAttendance(employeeId, date, context);
        }
        existing.setIsAbsent(true);
        existing.setLateMinutes(0);
        existing.setEarlyLeaveMinutes(0);
        saveOrUpdateAttendance(existing);
        return existing;
    }

    ErpHrAttendance upsertAttendanceForLeave(ErpHrAttendance existing, Long employeeId, LocalDate date,
                                             ErpHrShiftAssignment assignment,
                                             IServiceContext context) {
        if (existing == null) {
            existing = newAttendance(employeeId, date, context);
        }
        existing.setIsAbsent(true);
        existing.setLeaveRequestId(assignment.getLeaveRequestId());
        existing.setLateMinutes(0);
        existing.setEarlyLeaveMinutes(0);
        saveOrUpdateAttendance(existing);
        return existing;
    }

    ErpHrAttendance newAttendance(Long employeeId, LocalDate date, IServiceContext context) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        ErpHrAttendance a = new ErpHrAttendance();
        a.setEmployeeId(employeeId);
        a.setDate(date);
        a.setIsAbsent(false);
        a.setLateMinutes(0);
        a.setEarlyLeaveMinutes(0);
        dao.saveEntity(a);
        return a;
    }

    void updateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        dao.saveOrUpdateEntity(attendance);
    }

    void saveOrUpdateAttendance(ErpHrAttendance attendance) {
        IEntityDao<ErpHrAttendance> dao = daoProvider().daoFor(ErpHrAttendance.class);
        dao.saveOrUpdateEntity(attendance);
    }

    void updateAssignmentStatus(ErpHrShiftAssignment assignment) {
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider().daoFor(ErpHrShiftAssignment.class);
        dao.updateEntity(assignment);
    }

    /**
     * 内部便利方法：查询某员工日期范围内的排班（供 Phase 4 休假联动使用）。
     */
    public List<ErpHrShiftAssignment> findAssignmentsByEmployeeRange(Long employeeId,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate,
                                                                     IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                dateBetween("assignmentDate", startDate, endDate)));
        return assignmentBiz.findList(q, null, context);
    }
}
