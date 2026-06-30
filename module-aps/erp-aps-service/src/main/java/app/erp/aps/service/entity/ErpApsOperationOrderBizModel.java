
package app.erp.aps.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.dao.entity.ErpApsOperationOrder;

@BizModel("ErpApsOperationOrder")
public class ErpApsOperationOrderBizModel extends CrudBizModel<ErpApsOperationOrder> implements IErpApsOperationOrderBiz{
    public ErpApsOperationOrderBizModel(){
        setEntityName(ErpApsOperationOrder.class.getName());
    }
}
