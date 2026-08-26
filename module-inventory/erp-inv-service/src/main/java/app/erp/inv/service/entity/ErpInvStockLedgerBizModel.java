
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import app.erp.common.service.AbstractErpImmutableCrudBizModel;

import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockLedger;

@BizModel("ErpInvStockLedger")
public class ErpInvStockLedgerBizModel extends AbstractErpImmutableCrudBizModel<ErpInvStockLedger> implements IErpInvStockLedgerBiz{
    public ErpInvStockLedgerBizModel(){
        setEntityName(ErpInvStockLedger.class.getName());
    }
}
