
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadConvLogBiz;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;

@BizModel("ErpCrmLeadConvLog")
public class ErpCrmLeadConvLogBizModel extends CrudBizModel<ErpCrmLeadConvLog> implements IErpCrmLeadConvLogBiz{
    public ErpCrmLeadConvLogBizModel(){
        setEntityName(ErpCrmLeadConvLog.class.getName());
    }
}
