
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;

@BizModel("ErpCrmEvent")
public class ErpCrmEventBizModel extends CrudBizModel<ErpCrmEvent> implements IErpCrmEventBiz{
    public ErpCrmEventBizModel(){
        setEntityName(ErpCrmEvent.class.getName());
    }
}
