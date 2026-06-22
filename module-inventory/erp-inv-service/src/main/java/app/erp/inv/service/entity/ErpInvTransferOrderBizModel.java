
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;

@BizModel("ErpInvTransferOrder")
public class ErpInvTransferOrderBizModel extends CrudBizModel<ErpInvTransferOrder> implements IErpInvTransferOrderBiz{
    public ErpInvTransferOrderBizModel(){
        setEntityName(ErpInvTransferOrder.class.getName());
    }
}
