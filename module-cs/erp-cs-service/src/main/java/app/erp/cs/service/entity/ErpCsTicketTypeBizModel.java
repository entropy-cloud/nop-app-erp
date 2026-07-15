
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketTypeBiz;
import app.erp.cs.dao.entity.ErpCsTicketType;
import java.util.List;

@BizModel("ErpCsTicketType")
public class ErpCsTicketTypeBizModel extends CrudBizModel<ErpCsTicketType> implements IErpCsTicketTypeBiz{
    public ErpCsTicketTypeBizModel(){
        setEntityName(ErpCsTicketType.class.getName());
    }

    

}
