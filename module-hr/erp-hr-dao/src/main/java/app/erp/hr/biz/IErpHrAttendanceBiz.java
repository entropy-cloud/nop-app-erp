
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrAttendance;

import java.time.LocalDate;

public interface IErpHrAttendanceBiz extends ICrudBiz<ErpHrAttendance>{

    /**
     * 员工签到（UC-HR-06）。创建/更新当日考勤 clockIn=now。
     * 重复签到以最后一次为准（last-wins），覆盖 clockIn；若 clockOut 已存在则重算 workHours。
     */
    @BizMutation
    @SingleSession
    ErpHrAttendance clockIn(@Name("employeeId") Long employeeId, IServiceContext context);

    /**
     * 员工签退（UC-HR-06）。更新当日考勤 clockOut=now + 计算 workHours。
     * 若当日未签到则抛 ERR_NOT_CLOCKED_IN。
     */
    @BizMutation
    @SingleSession
    ErpHrAttendance clockOut(@Name("employeeId") Long employeeId, IServiceContext context);

    /**
     * 查询某员工当日考勤状态。
     */
    @BizQuery
    ErpHrAttendance getTodayAttendance(@Name("employeeId") Long employeeId, IServiceContext context);

    /**
     * 手工补卡签到（UC-HR-06⑮ 设备故障时支持手工补卡，RC-R1.7）。
     * HR 专员角色为员工补录指定日期（可历史日期）的 clockIn：
     * 定位员工+日期考勤记录（无则新建，businessDate=补卡日期）→ 写 clockIn + source=MANUAL + remark=reason；
     * clockOut 已存在时按 computeWorkHours 重算 workHours。reason 必填（空抛 ERR_MAKEUP_REASON_REQUIRED）。
     * 补卡是绕过打卡时序守卫的受控通道：直接写目标时间戳，不触发 clockIn/clockOut 时序守卫。
     */
    @BizMutation
    @SingleSession
    ErpHrAttendance makeUpClockIn(@Name("employeeId") Long employeeId,
                                  @Name("date") LocalDate date,
                                  @Name("clockTime") java.sql.Timestamp clockTime,
                                  @Name("reason") String reason,
                                  IServiceContext context);

    /**
     * 手工补卡签退（UC-HR-06⑮ 设备故障时支持手工补卡，RC-R1.7）。
     * 语义同 makeUpClockIn，写 clockOut；clockIn 已存在时重算 workHours。
     */
    @BizMutation
    @SingleSession
    ErpHrAttendance makeUpClockOut(@Name("employeeId") Long employeeId,
                                   @Name("date") LocalDate date,
                                   @Name("clockTime") java.sql.Timestamp clockTime,
                                   @Name("reason") String reason,
                                   IServiceContext context);
}
