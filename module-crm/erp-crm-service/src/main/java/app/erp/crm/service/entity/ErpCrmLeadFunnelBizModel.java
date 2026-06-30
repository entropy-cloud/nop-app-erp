
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadFunnelBiz;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;

@BizModel("ErpCrmLeadFunnel")
public class ErpCrmLeadFunnelBizModel extends CrudBizModel<ErpCrmLeadFunnel> implements IErpCrmLeadFunnelBiz{
    public ErpCrmLeadFunnelBizModel(){
        setEntityName(ErpCrmLeadFunnel.class.getName());
    }
}
