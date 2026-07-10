
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrLeaveRequest;

public interface IErpHrLeaveRequestBiz extends ICrudBiz<ErpHrLeaveRequest>{

    /**
     * 提交休假申请（DRAFT → SUBMITTED）。校验余额充足 + 日期不重叠。
     */
    @BizMutation
    @SingleSession
    ErpHrLeaveRequest submit(@Name("id") String id, IServiceContext context);

    /**
     * 审批通过休假申请（SUBMITTED → APPROVED）。设置审批人/审批时间 + 激活排班联动钩子。
     */
    @BizMutation
    @SingleSession
    ErpHrLeaveRequest approve(@Name("id") String id, IServiceContext context);

    /**
     * 驳回休假申请（SUBMITTED → REJECTED）。
     */
    @BizMutation
    ErpHrLeaveRequest reject(@Name("id") String id, IServiceContext context);

    /**
     * 取消已批准的休假申请（APPROVED → CANCELLED）。回退排班联动标记。
     */
    @BizMutation
    @SingleSession
    ErpHrLeaveRequest cancel(@Name("id") String id, IServiceContext context);

    /**
     * 查询某员工某休假类型某福利年度的剩余额度。
     *
     * @return 剩余天数 = entitledDays + carriedForwardDays − Σ approved durationDays
     */
    @BizQuery
    java.math.BigDecimal getBalance(@Name("employeeId") Long employeeId,
                                    @Name("leaveType") String leaveType,
                                    @Name("fiscalYear") Integer fiscalYear,
                                    IServiceContext context);
}
