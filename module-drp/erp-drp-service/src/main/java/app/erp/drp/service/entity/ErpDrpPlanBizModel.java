
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpDrpPlanBiz;
import app.erp.drp.dao.entity.ErpDrpPlan;

@BizModel("ErpDrpPlan")
public class ErpDrpPlanBizModel extends CrudBizModel<ErpDrpPlan> implements IErpDrpPlanBiz{
    public ErpDrpPlanBizModel(){
        setEntityName(ErpDrpPlan.class.getName());
    }
}
