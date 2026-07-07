
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstMaintenanceCostBiz;
import app.erp.ast.dao.entity.ErpAstMaintenanceCost;

@BizModel("ErpAstMaintenanceCost")
public class ErpAstMaintenanceCostBizModel extends CrudBizModel<ErpAstMaintenanceCost> implements IErpAstMaintenanceCostBiz{
    public ErpAstMaintenanceCostBizModel(){
        setEntityName(ErpAstMaintenanceCost.class.getName());
    }
}
