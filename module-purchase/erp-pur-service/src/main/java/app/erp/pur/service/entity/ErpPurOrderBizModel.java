
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;

@BizModel("ErpPurOrder")
public class ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> implements IErpPurOrderBiz{
    public ErpPurOrderBizModel(){
        setEntityName(ErpPurOrder.class.getName());
    }
}
