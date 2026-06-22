
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdMaterialBiz;
import app.erp.md.dao.entity.ErpMdMaterial;

@BizModel("ErpMdMaterial")
public class ErpMdMaterialBizModel extends CrudBizModel<ErpMdMaterial> implements IErpMdMaterialBiz{
    public ErpMdMaterialBizModel(){
        setEntityName(ErpMdMaterial.class.getName());
    }
}
