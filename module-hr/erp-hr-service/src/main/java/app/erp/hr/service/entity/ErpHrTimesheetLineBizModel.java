
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
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
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpHrTimesheetLine")
public class ErpHrTimesheetLineBizModel extends CrudBizModel<ErpHrTimesheetLine> implements IErpHrTimesheetLineBiz{

    @Inject
    IErpHrTimesheetBiz timesheetBiz;

    public ErpHrTimesheetLineBizModel(){
        setEntityName(ErpHrTimesheetLine.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrTimesheetLine> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        checkDailyHoursLimit(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpHrTimesheetLine> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        checkDailyHoursLimit(entityData.getEntity(), context);
    }

    @Override
    protected void afterEntityChange(ErpHrTimesheetLine entity, IServiceContext context) {
        super.afterEntityChange(entity, context);
        if (entity.getTimesheetId() != null) {
            recomputeParentTotalHours(entity.getTimesheetId(), context);
        }
    }

    // ---------- helpers ----------

    private void recomputeParentTotalHours(String timesheetId, IServiceContext context) {
        ErpHrTimesheet timesheet = timesheetBiz.requireEntity(timesheetId, null, context);
        orm().flushSession();
        QueryBean q = new QueryBean();
        q.addFilter(eq("timesheetId", timesheetId));
        List<ErpHrTimesheetLine> lines = findList(q, null, context);
        timesheet.setTotalHours(sumHours(lines));
        timesheetBiz.updateEntity(timesheet, null, context);
    }

    private void checkDailyHoursLimit(ErpHrTimesheetLine line, IServiceContext context) {
        if (line.getHours() == null || line.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", line.getEmployeeId()), eq("workDate", line.getWorkDate())));
        List<ErpHrTimesheetLine> sameDay = findList(q, null, context);
        BigDecimal total = dailyTotalFor(line, sameDay);
        if (total.compareTo(BigDecimal.valueOf(ErpHrConstants.MAX_DAILY_HOURS)) > 0) {
            throw new NopException(ErpHrErrors.ERR_TIMESHEET_DAILY_HOURS_EXCEEDED)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, line.getEmployeeId())
                    .param(ErpHrErrors.ARG_WORK_DATE, line.getWorkDate())
                    .param(ErpHrErrors.ARG_TOTAL_HOURS, total);
        }
    }

    static BigDecimal sumHours(List<ErpHrTimesheetLine> lines) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpHrTimesheetLine line : lines) {
            if (line.getHours() != null) {
                sum = sum.add(line.getHours());
            }
        }
        return sum;
    }

    static BigDecimal dailyTotalFor(ErpHrTimesheetLine line, List<ErpHrTimesheetLine> sameDayLines) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpHrTimesheetLine sameDay : sameDayLines) {
            if (line.getId() != null && line.getId().equals(sameDay.getId())) {
                continue;
            }
            if (sameDay.getHours() != null) {
                sum = sum.add(sameDay.getHours());
            }
        }
        if (line.getHours() != null) {
            sum = sum.add(line.getHours());
        }
        return sum;
    }
}
