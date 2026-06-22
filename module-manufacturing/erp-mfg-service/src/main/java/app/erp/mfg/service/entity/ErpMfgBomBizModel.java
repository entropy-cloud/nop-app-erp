
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgBomBiz;
import app.erp.mfg.dao.entity.ErpMfgBom;

@BizModel("ErpMfgBom")
public class ErpMfgBomBizModel extends CrudBizModel<ErpMfgBom> implements IErpMfgBomBiz{
    public ErpMfgBomBizModel(){
        setEntityName(ErpMfgBom.class.getName());
    }
}
