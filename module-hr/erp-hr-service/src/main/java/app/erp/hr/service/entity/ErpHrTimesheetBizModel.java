
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.dao.entity.ErpHrTimesheet;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import java.util.List;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz{
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

}
