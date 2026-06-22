
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.dao.entity.ErpInvStockMove;

@BizModel("ErpInvStockMove")
public class ErpInvStockMoveBizModel extends CrudBizModel<ErpInvStockMove> implements IErpInvStockMoveBiz{
    public ErpInvStockMoveBizModel(){
        setEntityName(ErpInvStockMove.class.getName());
    }
}
