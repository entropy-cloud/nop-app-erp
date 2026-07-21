
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpDrpScenarioVersionBiz;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;

@BizModel("ErpDrpScenarioVersion")
public class ErpDrpScenarioVersionBizModel extends CrudBizModel<ErpDrpScenarioVersion> implements IErpDrpScenarioVersionBiz{
    public ErpDrpScenarioVersionBizModel(){
        setEntityName(ErpDrpScenarioVersion.class.getName());
    }
}
