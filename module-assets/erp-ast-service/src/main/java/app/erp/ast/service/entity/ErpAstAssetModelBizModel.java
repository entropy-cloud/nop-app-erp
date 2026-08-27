
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstAssetModelBiz;
import app.erp.ast.dao.entity.ErpAstAssetModel;

@BizModel("ErpAstAssetModel")
public class ErpAstAssetModelBizModel extends CrudBizModel<ErpAstAssetModel> implements IErpAstAssetModelBiz{
    public ErpAstAssetModelBizModel(){
        setEntityName(ErpAstAssetModel.class.getName());
    }
}
