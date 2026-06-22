
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockBalanceBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;

@BizModel("ErpInvStockBalance")
public class ErpInvStockBalanceBizModel extends CrudBizModel<ErpInvStockBalance> implements IErpInvStockBalanceBiz{
    public ErpInvStockBalanceBizModel(){
        setEntityName(ErpInvStockBalance.class.getName());
    }
}
