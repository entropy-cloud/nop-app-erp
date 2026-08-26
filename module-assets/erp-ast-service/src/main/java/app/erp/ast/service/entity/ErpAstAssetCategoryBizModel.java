
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCategoryBiz;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import java.util.List;

@BizModel("ErpAstAssetCategory")
public class ErpAstAssetCategoryBizModel extends AbstractErpCrudBizModel<ErpAstAssetCategory> implements IErpAstAssetCategoryBiz {
    public ErpAstAssetCategoryBizModel() {
        setEntityName(ErpAstAssetCategory.class.getName());
    }

}
