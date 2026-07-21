
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMrpScenarioVersionBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;

@BizModel("ErpMfgMrpScenarioVersion")
public class ErpMfgMrpScenarioVersionBizModel extends CrudBizModel<ErpMfgMrpScenarioVersion> implements IErpMfgMrpScenarioVersionBiz{
    public ErpMfgMrpScenarioVersionBizModel(){
        setEntityName(ErpMfgMrpScenarioVersion.class.getName());
    }
}
