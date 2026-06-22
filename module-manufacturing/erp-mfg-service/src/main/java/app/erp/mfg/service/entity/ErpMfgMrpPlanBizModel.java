
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMrpPlanBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;

@BizModel("ErpMfgMrpPlan")
public class ErpMfgMrpPlanBizModel extends CrudBizModel<ErpMfgMrpPlan> implements IErpMfgMrpPlanBiz{
    public ErpMfgMrpPlanBizModel(){
        setEntityName(ErpMfgMrpPlan.class.getName());
    }
}
