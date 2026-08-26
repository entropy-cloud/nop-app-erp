
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgBomOperationBiz;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;

@BizModel("ErpMfgBomOperation")
public class ErpMfgBomOperationBizModel extends AbstractErpCrudBizModel<ErpMfgBomOperation> implements IErpMfgBomOperationBiz{
    public ErpMfgBomOperationBizModel(){
        setEntityName(ErpMfgBomOperation.class.getName());
    }
}
