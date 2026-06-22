
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;

@BizModel("ErpAstAssetCapitalization")
public class ErpAstAssetCapitalizationBizModel extends CrudBizModel<ErpAstAssetCapitalization> implements IErpAstAssetCapitalizationBiz{
    public ErpAstAssetCapitalizationBizModel(){
        setEntityName(ErpAstAssetCapitalization.class.getName());
    }
}
