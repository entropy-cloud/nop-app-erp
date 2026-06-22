
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;

@BizModel("ErpSalDelivery")
public class ErpSalDeliveryBizModel extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz{
    public ErpSalDeliveryBizModel(){
        setEntityName(ErpSalDelivery.class.getName());
    }
}
