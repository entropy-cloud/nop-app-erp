
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockTakeLineBiz;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;

@BizModel("ErpInvStockTakeLine")
public class ErpInvStockTakeLineBizModel extends CrudBizModel<ErpInvStockTakeLine> implements IErpInvStockTakeLineBiz{
    public ErpInvStockTakeLineBizModel(){
        setEntityName(ErpInvStockTakeLine.class.getName());
    }
}
