
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgProductionVersionBiz;
import app.erp.mfg.dao.entity.ErpMfgProductionVersion;

@BizModel("ErpMfgProductionVersion")
public class ErpMfgProductionVersionBizModel extends CrudBizModel<ErpMfgProductionVersion> implements IErpMfgProductionVersionBiz{
    public ErpMfgProductionVersionBizModel(){
        setEntityName(ErpMfgProductionVersion.class.getName());
    }
}
