
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsContractBiz;
import app.erp.cs.dao.entity.ErpCsContract;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

@BizModel("ErpCsContract")
public class ErpCsContractBizModel extends CrudBizModel<ErpCsContract> implements IErpCsContractBiz{
    public ErpCsContractBizModel(){
        setEntityName(ErpCsContract.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCsContract> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCsContract entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

}
