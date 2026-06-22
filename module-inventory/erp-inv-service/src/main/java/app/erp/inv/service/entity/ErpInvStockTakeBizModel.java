
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockTakeBiz;
import app.erp.inv.dao.entity.ErpInvStockTake;

@BizModel("ErpInvStockTake")
public class ErpInvStockTakeBizModel extends CrudBizModel<ErpInvStockTake> implements IErpInvStockTakeBiz{
    public ErpInvStockTakeBizModel(){
        setEntityName(ErpInvStockTake.class.getName());
    }
}
