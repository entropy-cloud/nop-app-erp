
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockLedger;

@BizModel("ErpInvStockLedger")
public class ErpInvStockLedgerBizModel extends CrudBizModel<ErpInvStockLedger> implements IErpInvStockLedgerBiz{
    public ErpInvStockLedgerBizModel(){
        setEntityName(ErpInvStockLedger.class.getName());
    }
}
