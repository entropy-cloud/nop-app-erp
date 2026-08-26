
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.inv.biz.IErpInvPickingOrderBiz;
import app.erp.inv.dao.entity.ErpInvPickingOrder;

@BizModel("ErpInvPickingOrder")
public class ErpInvPickingOrderBizModel extends AbstractErpCrudBizModel<ErpInvPickingOrder> implements IErpInvPickingOrderBiz{
    public ErpInvPickingOrderBizModel(){
        setEntityName(ErpInvPickingOrder.class.getName());
    }
}
