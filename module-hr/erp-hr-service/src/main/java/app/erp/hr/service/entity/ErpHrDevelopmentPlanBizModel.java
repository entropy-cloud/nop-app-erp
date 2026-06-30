
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrDevelopmentPlanBiz;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;

@BizModel("ErpHrDevelopmentPlan")
public class ErpHrDevelopmentPlanBizModel extends CrudBizModel<ErpHrDevelopmentPlan> implements IErpHrDevelopmentPlanBiz{
    public ErpHrDevelopmentPlanBizModel(){
        setEntityName(ErpHrDevelopmentPlan.class.getName());
    }
}
