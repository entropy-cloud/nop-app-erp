
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLostReasonBiz;
import app.erp.crm.dao.entity.ErpCrmLostReason;

@BizModel("ErpCrmLostReason")
public class ErpCrmLostReasonBizModel extends CrudBizModel<ErpCrmLostReason> implements IErpCrmLostReasonBiz{
    public ErpCrmLostReasonBizModel(){
        setEntityName(ErpCrmLostReason.class.getName());
    }
}
