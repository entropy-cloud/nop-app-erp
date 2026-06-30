
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadSequenceProgressBiz;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;

@BizModel("ErpCrmLeadSequenceProgress")
public class ErpCrmLeadSequenceProgressBizModel extends CrudBizModel<ErpCrmLeadSequenceProgress> implements IErpCrmLeadSequenceProgressBiz{
    public ErpCrmLeadSequenceProgressBizModel(){
        setEntityName(ErpCrmLeadSequenceProgress.class.getName());
    }
}
