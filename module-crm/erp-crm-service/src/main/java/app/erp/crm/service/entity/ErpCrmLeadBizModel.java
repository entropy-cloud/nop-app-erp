
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.dao.entity.ErpCrmLead;

@BizModel("ErpCrmLead")
public class ErpCrmLeadBizModel extends CrudBizModel<ErpCrmLead> implements IErpCrmLeadBiz{
    public ErpCrmLeadBizModel(){
        setEntityName(ErpCrmLead.class.getName());
    }
}
