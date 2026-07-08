
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

@BizModel("ErpHrAttendance")
public class ErpHrAttendanceBizModel extends CrudBizModel<ErpHrAttendance> implements IErpHrAttendanceBiz{
    public ErpHrAttendanceBizModel(){
        setEntityName(ErpHrAttendance.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrAttendance> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrAttendance entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

}
