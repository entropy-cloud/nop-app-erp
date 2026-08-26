
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.inv.biz.IErpInvPickingOrderLineBiz;
import app.erp.inv.dao.entity.ErpInvPickingOrderLine;

@BizModel("ErpInvPickingOrderLine")
public class ErpInvPickingOrderLineBizModel extends AbstractErpCrudBizModel<ErpInvPickingOrderLine> implements IErpInvPickingOrderLineBiz{
    public ErpInvPickingOrderLineBizModel(){
        setEntityName(ErpInvPickingOrderLine.class.getName());
    }
}
