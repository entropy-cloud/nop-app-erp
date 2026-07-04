
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrShift;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

/**
 * 班次模板 + 考勤计算聚合根 Biz（shift-scheduling.md §一/§四）。
 *
 * <p>除标准 CRUD 外承载 {@link #calcAttendance}：读 ErpHrShiftAssignment 标准班次 vs ErpHrAttendance 实际打卡，
 * 计算 lateMinutes/earlyLeaveMinutes/isAbsent（结果写 ErpHrAttendance 已有列），同步 ShiftAssignment.status。
 */
public interface IErpHrShiftBiz extends ICrudBiz<ErpHrShift> {

    /**
     * 计算某员工某日的考勤派生字段（shift-scheduling.md §4.1/§4.2）。
     *
     * <p>读 ShiftAssignment.shift（startTime/endTime/graceLateMinutes/graceEarlyLeaveMinutes）
     * vs Attendance.clockIn/clockOut；准时/迟到/严重迟到/早退/缺勤；结果写入 ErpHrAttendance
     * （lateMinutes/earlyLeaveMinutes/isAbsent），并同步 ErpHrShiftAssignment.status
     * （SCHEDULED→PRESENT 或 ABSENT）。
     *
     * @return 计算后的考勤记录；若当日无排班则返回 null
     */
    @BizMutation
    @SingleSession
    ErpHrAttendance calcAttendance(@Name("employeeId") Long employeeId,
                                   @Name("assignmentDate") LocalDate assignmentDate,
                                   IServiceContext context);

    /**
     * 查询某员工某日的考勤记录（可空）。
     */
    @BizQuery
    ErpHrAttendance findAttendanceByDate(@Name("employeeId") Long employeeId,
                                         @Name("date") LocalDate date,
                                         IServiceContext context);

    /**
     * 休假审批通过联动（shift-scheduling.md §6.1）。
     *
     * <p>检索该员工休假日期范围内的排班，标记 isAbsent=true/absenceReason=LEAVE/
     * leaveRequestId/status=ABSENT。
     */
    @BizMutation
    @SingleSession
    void onLeaveApproved(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context);

    /**
     * 休假取消联动（shift-scheduling.md §6.2）。
     *
     * <p>解除上述标记：status→SCHEDULED（如有打卡则回 PRESENT 由 calcAttendance 重算），
     * isAbsent=false/absenceReason=null/leaveRequestId=null。
     */
    @BizMutation
    @SingleSession
    void onLeaveCancelled(@Name("leaveRequestId") Long leaveRequestId, IServiceContext context);
}
