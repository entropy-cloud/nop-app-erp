
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstMovementBiz;
import app.erp.ast.dao.entity.ErpAstMovement;

@BizModel("ErpAstMovement")
public class ErpAstMovementBizModel extends CrudBizModel<ErpAstMovement> implements IErpAstMovementBiz{
    public ErpAstMovementBizModel(){
        setEntityName(ErpAstMovement.class.getName());
    }
}
