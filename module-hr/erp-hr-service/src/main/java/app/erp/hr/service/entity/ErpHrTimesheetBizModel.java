
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.dao.entity.ErpHrTimesheet;
import app.erp.hr.service.ErpHrErrors;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Objects;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz {
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
        if (!Objects.equals(timesheet.getStatus(), "DRAFT")) {
            throw new NopException(ErpHrErrors.ERR_HR_TIMESHEET_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_TIMESHEET_ID, timesheetId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, timesheet.getStatus());
        }
        timesheet.setStatus("SUBMITTED");
        updateEntity(timesheet, null, context);
        return timesheet;
    }
}
