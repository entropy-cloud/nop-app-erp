
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
import io.nop.biz.crud.EntityData;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz {

    @Inject
    IErpHrTimesheetLineBiz timesheetLineBiz;

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
    public ErpHrTimesheet submit(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        String status = timesheet.getStatus();
        boolean draft = Objects.equals(status, ErpHrConstants.TIMESHEET_STATUS_DRAFT);
        boolean rejected = Objects.equals(status, ErpHrConstants.TIMESHEET_STATUS_REJECTED);
        if (!draft && !rejected) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status);
        }
        timesheet.setTotalHours(sumHoursByTimesheet(timesheetId, context));
        checkDailyHoursLimitForTimesheet(timesheetId, context);
        timesheet.setStatus(ErpHrConstants.TIMESHEET_STATUS_SUBMITTED);
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    @Override
    @BizMutation
    public ErpHrTimesheet approve(@Name("timesheetId") Long timesheetId, IServiceContext context) {
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        if (!Objects.equals(timesheet.getStatus(), ErpHrConstants.TIMESHEET_STATUS_SUBMITTED)) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, timesheet.getStatus());
        }
        timesheet.setStatus(ErpHrConstants.TIMESHEET_STATUS_APPROVED);
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    @Override
    @BizMutation
    public ErpHrTimesheet reject(@Name("timesheetId") Long timesheetId, @Name("reason") String reason,
                                 IServiceContext context) {
        if (StringHelper.isBlank(reason)) {
            throw new NopException(ErpHrErrors.ERR_TIMESHEET_REJECT_REASON_REQUIRED)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId);
        }
        ErpHrTimesheet timesheet = requireEntity(String.valueOf(timesheetId), null, context);
        if (!Objects.equals(timesheet.getStatus(), ErpHrConstants.TIMESHEET_STATUS_SUBMITTED)) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, timesheet.getStatus());
        }
        timesheet.setStatus(ErpHrConstants.TIMESHEET_STATUS_REJECTED);
        timesheet.setRemark(reason);
        updateEntity(timesheet, null, context);
        return timesheet;
    }

    // ---------- helpers ----------

    private BigDecimal sumHoursByTimesheet(Long timesheetId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("timesheetId", timesheetId));
        return ErpHrTimesheetLineBizModel.sumHours(timesheetLineBiz.findList(q, null, context));
    }

    private void checkDailyHoursLimitForTimesheet(Long timesheetId, IServiceContext context) {
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
