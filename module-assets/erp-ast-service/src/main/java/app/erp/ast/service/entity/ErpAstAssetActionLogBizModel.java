
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstAssetActionLogBiz;
import app.erp.ast.dao.entity.ErpAstAssetActionLog;

@BizModel("ErpAstAssetActionLog")
public class ErpAstAssetActionLogBizModel extends CrudBizModel<ErpAstAssetActionLog> implements IErpAstAssetActionLogBiz{
    public ErpAstAssetActionLogBizModel(){
        setEntityName(ErpAstAssetActionLog.class.getName());
    }
}
