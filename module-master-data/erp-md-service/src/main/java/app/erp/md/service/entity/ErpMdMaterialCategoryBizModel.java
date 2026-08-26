
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.md.biz.IErpMdMaterialCategoryBiz;
import app.erp.md.dao.entity.ErpMdMaterialCategory;

@BizModel("ErpMdMaterialCategory")
public class ErpMdMaterialCategoryBizModel extends AbstractErpCrudBizModel<ErpMdMaterialCategory> implements IErpMdMaterialCategoryBiz{
    public ErpMdMaterialCategoryBizModel(){
        setEntityName(ErpMdMaterialCategory.class.getName());
    }
}
