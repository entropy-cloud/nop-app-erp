
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMovementBiz;
import app.erp.ast.dao.entity.ErpAstMovement;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

@BizModel("ErpAstMovement")
public class ErpAstMovementBizModel extends CrudBizModel<ErpAstMovement> implements IErpAstMovementBiz {
    public ErpAstMovementBizModel() {
        setEntityName(ErpAstMovement.class.getName());
    }

}
