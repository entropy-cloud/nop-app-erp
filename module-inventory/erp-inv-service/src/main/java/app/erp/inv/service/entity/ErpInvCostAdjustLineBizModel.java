
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.inv.biz.IErpInvCostAdjustLineBiz;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;

@BizModel("ErpInvCostAdjustLine")
public class ErpInvCostAdjustLineBizModel extends AbstractErpCrudBizModel<ErpInvCostAdjustLine> implements IErpInvCostAdjustLineBiz{
    public ErpInvCostAdjustLineBizModel(){
        setEntityName(ErpInvCostAdjustLine.class.getName());
    }
}
