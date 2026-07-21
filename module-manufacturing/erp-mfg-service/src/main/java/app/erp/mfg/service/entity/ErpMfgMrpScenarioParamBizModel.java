
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMrpScenarioParamBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioParam;

@BizModel("ErpMfgMrpScenarioParam")
public class ErpMfgMrpScenarioParamBizModel extends CrudBizModel<ErpMfgMrpScenarioParam> implements IErpMfgMrpScenarioParamBiz{
    public ErpMfgMrpScenarioParamBizModel(){
        setEntityName(ErpMfgMrpScenarioParam.class.getName());
    }
}
