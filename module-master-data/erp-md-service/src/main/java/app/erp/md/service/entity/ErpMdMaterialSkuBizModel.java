
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdMaterialSkuBiz;
import app.erp.md.dao.entity.ErpMdMaterialSku;

@BizModel("ErpMdMaterialSku")
public class ErpMdMaterialSkuBizModel extends CrudBizModel<ErpMdMaterialSku> implements IErpMdMaterialSkuBiz{
    public ErpMdMaterialSkuBizModel(){
        setEntityName(ErpMdMaterialSku.class.getName());
    }
}
