
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrTimesheet;

public interface IErpHrTimesheetBiz extends ICrudBiz<ErpHrTimesheet>{

    /**
     * 提交工时表：DRAFT/REJECTED→SUBMITTED。提交时重算 totalHours 并做 24h 日工时终检。
     * REJECTED 为修改后重新提交路径（use-cases.md UC-HR-03 基本流程 5）。
     */
    @BizMutation
    ErpHrTimesheet submit(@Name("timesheetId") String timesheetId, IServiceContext context);

    /**
     * 审批通过：SUBMITTED→APPROVED。
     */
    @BizMutation
    ErpHrTimesheet approve(@Name("timesheetId") String timesheetId, IServiceContext context);

    /**
     * 驳回：SUBMITTED→REJECTED，reason 必填并写入 remark。
     */
    @BizMutation
    ErpHrTimesheet reject(@Name("timesheetId") String timesheetId, @Name("reason") String reason,
                          IServiceContext context);
}
