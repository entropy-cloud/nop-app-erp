
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrTimesheet;

public interface IErpHrTimesheetBiz extends ICrudBiz<ErpHrTimesheet>{

    /**
     * 提交工时表：DRAFT→SUBMITTED。提交后进入审批流程。
     */
    @BizMutation
    ErpHrTimesheet submit(@Name("timesheetId") Long timesheetId, IServiceContext context);
}
