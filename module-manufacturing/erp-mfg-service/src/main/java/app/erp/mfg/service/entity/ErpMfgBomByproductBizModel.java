
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgBomByproductBiz;
import app.erp.mfg.dao.entity.ErpMfgBomByproduct;

@BizModel("ErpMfgBomByproduct")
public class ErpMfgBomByproductBizModel extends CrudBizModel<ErpMfgBomByproduct> implements IErpMfgBomByproductBiz{
    public ErpMfgBomByproductBizModel(){
        setEntityName(ErpMfgBomByproduct.class.getName());
    }
}
