
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockMoveLineBiz;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;

import java.util.List;

@BizModel("ErpInvStockMoveLine")
public class ErpInvStockMoveLineBizModel extends CrudBizModel<ErpInvStockMoveLine> implements IErpInvStockMoveLineBiz{
    public ErpInvStockMoveLineBizModel(){
        setEntityName(ErpInvStockMoveLine.class.getName());
    }

}
