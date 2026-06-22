
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstAssetCategoryBiz;
import app.erp.ast.dao.entity.ErpAstAssetCategory;

@BizModel("ErpAstAssetCategory")
public class ErpAstAssetCategoryBizModel extends CrudBizModel<ErpAstAssetCategory> implements IErpAstAssetCategoryBiz{
    public ErpAstAssetCategoryBizModel(){
        setEntityName(ErpAstAssetCategory.class.getName());
    }
}
