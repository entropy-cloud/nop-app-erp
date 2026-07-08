
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

@BizModel("ErpHrEmploymentContract")
public class ErpHrEmploymentContractBizModel extends CrudBizModel<ErpHrEmploymentContract> implements IErpHrEmploymentContractBiz{
    public ErpHrEmploymentContractBizModel(){
        setEntityName(ErpHrEmploymentContract.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrEmploymentContract> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrEmploymentContract entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

}
