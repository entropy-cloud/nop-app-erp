
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicketAction;

@BizModel("ErpCsTicketAction")
public class ErpCsTicketActionBizModel extends CrudBizModel<ErpCsTicketAction> implements IErpCsTicketActionBiz{
    public ErpCsTicketActionBizModel(){
        setEntityName(ErpCsTicketAction.class.getName());
    }
}
