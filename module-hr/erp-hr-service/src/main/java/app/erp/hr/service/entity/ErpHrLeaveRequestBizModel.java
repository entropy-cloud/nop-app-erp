
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

@BizModel("ErpHrLeaveRequest")
public class ErpHrLeaveRequestBizModel extends CrudBizModel<ErpHrLeaveRequest> implements IErpHrLeaveRequestBiz{
    public ErpHrLeaveRequestBizModel(){
        setEntityName(ErpHrLeaveRequest.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrLeaveRequest> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrLeaveRequest entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

}
