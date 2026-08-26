
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.qa.biz.IErpQaSamplingPlanBiz;
import app.erp.qa.dao.entity.ErpQaSamplingPlan;

@BizModel("ErpQaSamplingPlan")
public class ErpQaSamplingPlanBizModel extends AbstractErpCrudBizModel<ErpQaSamplingPlan> implements IErpQaSamplingPlanBiz{
    public ErpQaSamplingPlanBizModel(){
        setEntityName(ErpQaSamplingPlan.class.getName());
    }
}
