
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsCatalogCategoryBiz;
import app.erp.cs.dao.entity.ErpCsCatalogCategory;

@BizModel("ErpCsCatalogCategory")
public class ErpCsCatalogCategoryBizModel extends CrudBizModel<ErpCsCatalogCategory> implements IErpCsCatalogCategoryBiz{
    public ErpCsCatalogCategoryBizModel(){
        setEntityName(ErpCsCatalogCategory.class.getName());
    }
}
