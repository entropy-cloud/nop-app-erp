
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import app.erp.common.service.AbstractErpImmutableCrudBizModel;

import app.erp.inv.biz.IErpInvStockBalanceBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;

@BizModel("ErpInvStockBalance")
public class ErpInvStockBalanceBizModel extends AbstractErpImmutableCrudBizModel<ErpInvStockBalance> implements IErpInvStockBalanceBiz{
    public ErpInvStockBalanceBizModel(){
        setEntityName(ErpInvStockBalance.class.getName());
    }
}
