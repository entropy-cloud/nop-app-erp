
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadStatusBiz;
import app.erp.crm.dao.entity.ErpCrmLeadStatus;

@BizModel("ErpCrmLeadStatus")
public class ErpCrmLeadStatusBizModel extends CrudBizModel<ErpCrmLeadStatus> implements IErpCrmLeadStatusBiz{
    public ErpCrmLeadStatusBizModel(){
        setEntityName(ErpCrmLeadStatus.class.getName());
    }
}
