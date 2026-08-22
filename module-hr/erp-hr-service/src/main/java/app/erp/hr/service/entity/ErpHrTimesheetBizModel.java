
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.biz.IErpHrTimesheetLineBiz;
import app.erp.hr.dao.entity.ErpHrTimesheet;
import app.erp.hr.dao.entity.ErpHrTimesheetLine;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.statemachine.ErpHrTimesheetStateMachine;
import io.nop.biz.crud.EntityData;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz {

    @Inject
    IErpHrTimesheetLineBiz timesheetLineBiz;
    @Inject
    ErpHrTimesheetStateMachine timesheetStateMachine;

    public ErpHrTimesheetBizModel(){
        setEntityName(ErpHrTimesheet.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrTimesheet> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrTimesheet entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpHrTimesheet submit(@Name("timesheetId") String timesheetId, IServiceContext context) {
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        String status = timesheet.getStatus();
        // 固定来源态/目标态判断委托 ErpHrTimesheetStateMachine（Bean 矩阵权威，契约 §4/§7）：
        // submit 仅 DRAFT/REJECTED 合法。Bean 抛 common 层码，此处映射领域 ERR_HR_TIMESHEET_ILLEGAL_TRANSITION
        // （common 码作 cause）。
        try {
            timesheetStateMachine.assertCanSubmit(status);
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION, e)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status);
        }
        // 动态副作用保留原位：提交时重算 totalHours + 24h 跨表校验（非固定状态判断，归 BizModel）
        timesheet.setTotalHours(sumHoursByTimesheet(timesheetId, context));
        checkDailyHoursLimitForTimesheet(timesheetId, context);
        timesheet.setStatus(timesheetStateMachine.submitTargetStatus());
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    @Override
    @BizMutation
    public ErpHrTimesheet approve(@Name("timesheetId") String timesheetId, IServiceContext context) {
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        // 固定来源态/目标态判断委托 ErpHrTimesheetStateMachine（Bean 矩阵权威，契约 §4/§7）：
        // approve 仅 SUBMITTED 合法。Bean 抛 common 层码，此处映射领域 ERR_HR_TIMESHEET_ILLEGAL_TRANSITION
        // （common 码作 cause）。
        try {
            timesheetStateMachine.assertCanApprove(timesheet.getStatus());
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION, e)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, timesheet.getStatus());
        }
        timesheet.setStatus(timesheetStateMachine.approveTargetStatus());
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    @Override
    @BizMutation
    public ErpHrTimesheet reject(@Name("timesheetId") String timesheetId, @Name("reason") String reason,
                                 IServiceContext context) {
        // 动态业务校验保留原位：reason 必填守卫（非固定状态判断，归 BizModel）
        if (StringHelper.isBlank(reason)) {
            throw new NopException(ErpHrErrors.ERR_TIMESHEET_REJECT_REASON_REQUIRED)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId);
        }
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        // 固定来源态/目标态判断委托 ErpHrTimesheetStateMachine（Bean 矩阵权威，契约 §4/§7）：
        // reject 仅 SUBMITTED 合法。Bean 抛 common 层码，此处映射领域 ERR_HR_TIMESHEET_ILLEGAL_TRANSITION
        // （common 码作 cause）。
        try {
            timesheetStateMachine.assertCanReject(timesheet.getStatus());
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION, e)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, timesheet.getStatus());
        }
        timesheet.setStatus(timesheetStateMachine.rejectTargetStatus());
        // 动态副作用保留原位：reason 写入 remark（非固定状态判断，归 BizModel）
        timesheet.setRemark(reason);
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    // ---------- helpers ----------

    private BigDecimal sumHoursByTimesheet(String timesheetId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("timesheetId", timesheetId));
        return ErpHrTimesheetLineBizModel.sumHours(timesheetLineBiz.findList(q, null, context));
    }

    private void checkDailyHoursLimitForTimesheet(String timesheetId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("timesheetId", timesheetId));
        List<ErpHrTimesheetLine> lines = timesheetLineBiz.findList(q, null, context);
        for (ErpHrTimesheetLine line : lines) {
            checkDailyHoursLimit(line, context);
        }
    }

    private void checkDailyHoursLimit(ErpHrTimesheetLine line, IServiceContext context) {
        if (line.getHours() == null || line.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", line.getEmployeeId()), eq("workDate", line.getWorkDate())));
        List<ErpHrTimesheetLine> sameDay = timesheetLineBiz.findList(q, null, context);
        BigDecimal total = ErpHrTimesheetLineBizModel.dailyTotalFor(line, sameDay);
        if (total.compareTo(BigDecimal.valueOf(ErpHrConstants.MAX_DAILY_HOURS)) > 0) {
            throw new NopException(ErpHrErrors.ERR_TIMESHEET_DAILY_HOURS_EXCEEDED)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, line.getEmployeeId())
                    .param(ErpHrErrors.ARG_WORK_DATE, line.getWorkDate())
                    .param(ErpHrErrors.ARG_TOTAL_HOURS, total);
        }
    }
}
