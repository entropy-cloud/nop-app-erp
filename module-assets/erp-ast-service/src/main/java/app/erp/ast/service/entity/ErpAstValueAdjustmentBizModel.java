
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstValueAdjustmentBiz;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;

@BizModel("ErpAstValueAdjustment")
public class ErpAstValueAdjustmentBizModel extends CrudBizModel<ErpAstValueAdjustment> implements IErpAstValueAdjustmentBiz{
    public ErpAstValueAdjustmentBizModel(){
        setEntityName(ErpAstValueAdjustment.class.getName());
    }
}
