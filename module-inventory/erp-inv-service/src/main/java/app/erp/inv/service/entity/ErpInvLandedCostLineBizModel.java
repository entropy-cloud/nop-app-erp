
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.inv.biz.IErpInvLandedCostLineBiz;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;

@BizModel("ErpInvLandedCostLine")
public class ErpInvLandedCostLineBizModel extends AbstractErpCrudBizModel<ErpInvLandedCostLine> implements IErpInvLandedCostLineBiz{
    public ErpInvLandedCostLineBizModel(){
        setEntityName(ErpInvLandedCostLine.class.getName());
    }
}
