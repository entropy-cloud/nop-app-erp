
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.drp.biz.IErpDrpScenarioParamBiz;
import app.erp.drp.dao.entity.ErpDrpScenarioParam;

@BizModel("ErpDrpScenarioParam")
public class ErpDrpScenarioParamBizModel extends AbstractErpCrudBizModel<ErpDrpScenarioParam> implements IErpDrpScenarioParamBiz{
    public ErpDrpScenarioParamBizModel(){
        setEntityName(ErpDrpScenarioParam.class.getName());
    }
}
