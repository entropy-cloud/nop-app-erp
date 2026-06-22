
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMrpDemandBiz;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;

@BizModel("ErpMfgMrpDemand")
public class ErpMfgMrpDemandBizModel extends CrudBizModel<ErpMfgMrpDemand> implements IErpMfgMrpDemandBiz{
    public ErpMfgMrpDemandBizModel(){
        setEntityName(ErpMfgMrpDemand.class.getName());
    }
}
