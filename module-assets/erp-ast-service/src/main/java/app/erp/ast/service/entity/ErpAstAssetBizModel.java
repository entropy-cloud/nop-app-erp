
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

@BizModel("ErpAstAsset")
public class ErpAstAssetBizModel extends CrudBizModel<ErpAstAsset> implements IErpAstAssetBiz {
    public ErpAstAssetBizModel() {
        setEntityName(ErpAstAsset.class.getName());
    }

}
