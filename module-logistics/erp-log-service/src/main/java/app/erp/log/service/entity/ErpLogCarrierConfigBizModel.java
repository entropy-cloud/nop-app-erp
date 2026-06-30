
package app.erp.log.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogCarrierConfigBiz;
import app.erp.log.dao.entity.ErpLogCarrierConfig;

@BizModel("ErpLogCarrierConfig")
public class ErpLogCarrierConfigBizModel extends CrudBizModel<ErpLogCarrierConfig> implements IErpLogCarrierConfigBiz{
    public ErpLogCarrierConfigBizModel(){
        setEntityName(ErpLogCarrierConfig.class.getName());
    }
}
