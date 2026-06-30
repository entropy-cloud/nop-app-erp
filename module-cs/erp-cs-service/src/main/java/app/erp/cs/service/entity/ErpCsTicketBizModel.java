
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsTicket;

@BizModel("ErpCsTicket")
public class ErpCsTicketBizModel extends CrudBizModel<ErpCsTicket> implements IErpCsTicketBiz{
    public ErpCsTicketBizModel(){
        setEntityName(ErpCsTicket.class.getName());
    }
}
