
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCategoryBiz;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

@BizModel("ErpAstAssetCategory")
public class ErpAstAssetCategoryBizModel extends CrudBizModel<ErpAstAssetCategory> implements IErpAstAssetCategoryBiz {
    public ErpAstAssetCategoryBizModel() {
        setEntityName(ErpAstAssetCategory.class.getName());
    }

}
