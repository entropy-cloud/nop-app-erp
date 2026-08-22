package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.scheduling.ShiftAttendanceCalculator;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * ErpHrAttendance clockOut per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含当日签退编排（UC-HR-06）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * 共享 helper 单一真相源在 {@link AbstractErpHrAttendanceProcessor}。
 *
 * <p>跨天签退回退（RC-R1.6，P1-RC-013）：今日无可用签到记录时，回退探查昨日
 * （员工昨日存在跨天排班[夜班]且昨日记录已签到）→ 对昨日记录执行 clockOut
 * （shift-scheduling.md §4.2/§九.6 跨天班次归属开始日期）。
 */
public class ErpHrAttendanceClockOutProcessor extends AbstractErpHrAttendanceProcessor {

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    @Inject
    IErpHrShiftBiz shiftBiz;

    public ErpHrAttendance clockOut(String employeeId, IServiceContext context) {
        LocalDate today = CoreMetrics.today();
        ErpHrAttendance attendance = findAttendance(employeeId, today);
        if (attendance != null && attendance.getClockIn() != null) {
            // 今日记录可用（已签到）→ 既有路径优先（回退仅在 today 无可用记录时触发）
            return doClockOut(attendance);
        }

        // 今日无记录或记录未签到 → 跨天回退探查：昨日记录 + 昨日跨天排班（夜班）双条件
        LocalDate yesterday = today.minusDays(1);
        ErpHrAttendance yesterdayAttendance = findAttendance(employeeId, yesterday);
        if (yesterdayAttendance != null && yesterdayAttendance.getClockIn() != null
                && hasCrossDayAssignment(employeeId, yesterday, context)) {
            return doClockOut(yesterdayAttendance);
        }

        throw new NopException(ErpHrErrors.ERR_NOT_CLOCKED_IN)
                .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
    }

    /**
     * 判断员工在指定日期是否存在跨天班次排班（夜班）。孤儿 assignment（shift 关联为空）保守判不命中
     * 回退，防 {@code isCrossDayShift(null)} NPE（对齐 {@link ErpHrShiftCalcAttendanceProcessor}
     * null shift 跳过范式）。
     */
    protected boolean hasCrossDayAssignment(String employeeId, LocalDate assignmentDate, IServiceContext context) {
        ErpHrShiftAssignment assignment = assignmentBiz.findByEmployeeAndDate(employeeId, assignmentDate, context);
        if (assignment == null) {
            return false;
        }
        ErpHrShift shift = assignment.getShift();
        if (shift == null) {
            // shift 关联未随查询加载时，经 I*Biz 管道读取（跨实体访问规则：I*Biz 优先，避免 daoFor）
            shift = shiftBiz.get(String.valueOf(assignment.getShiftId()), false, context);
        }
        if (shift == null) {
            return false;
        }
        return ShiftAttendanceCalculator.isCrossDayShift(shift);
    }

    protected ErpHrAttendance doClockOut(ErpHrAttendance attendance) {
        attendance.setClockOut(CoreMetrics.currentTimestamp());
        attendance.setWorkHours(computeWorkHours(attendance.getClockIn().toLocalDateTime(), attendance.getClockOut().toLocalDateTime()));
        saveOrUpdateAttendance(attendance);
        return attendance;
    }
}
