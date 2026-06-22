
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgRoutingBiz;
import app.erp.mfg.dao.entity.ErpMfgRouting;

@BizModel("ErpMfgRouting")
public class ErpMfgRoutingBizModel extends CrudBizModel<ErpMfgRouting> implements IErpMfgRoutingBiz{
    public ErpMfgRoutingBizModel(){
        setEntityName(ErpMfgRouting.class.getName());
    }
}
