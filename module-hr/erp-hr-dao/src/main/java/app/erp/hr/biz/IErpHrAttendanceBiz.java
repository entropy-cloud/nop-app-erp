
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrAttendance;

public interface IErpHrAttendanceBiz extends ICrudBiz<ErpHrAttendance>{

    /**
     * 员工签到（UC-HR-06）。创建/更新当日考勤 clockIn=now。
     * 若当日已签到则抛 ERR_ALREADY_CLOCKED_IN。
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
}
