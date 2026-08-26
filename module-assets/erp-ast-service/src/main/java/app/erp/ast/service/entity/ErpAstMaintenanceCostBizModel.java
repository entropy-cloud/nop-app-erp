
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMaintenanceCostBiz;
import app.erp.ast.dao.entity.ErpAstMaintenanceCost;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import java.util.List;

@BizModel("ErpAstMaintenanceCost")
public class ErpAstMaintenanceCostBizModel extends AbstractErpCrudBizModel<ErpAstMaintenanceCost> implements IErpAstMaintenanceCostBiz {
    public ErpAstMaintenanceCostBizModel() {
        setEntityName(ErpAstMaintenanceCost.class.getName());
    }

}
