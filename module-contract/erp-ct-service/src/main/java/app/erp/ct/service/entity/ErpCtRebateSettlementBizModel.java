
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateSettlementBiz;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;

@BizModel("ErpCtRebateSettlement")
public class ErpCtRebateSettlementBizModel extends CrudBizModel<ErpCtRebateSettlement> implements IErpCtRebateSettlementBiz{
    public ErpCtRebateSettlementBizModel(){
        setEntityName(ErpCtRebateSettlement.class.getName());
    }
}
