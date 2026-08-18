
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketFulfillmentStepBiz;
import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;

@BizModel("ErpCsTicketFulfillmentStep")
public class ErpCsTicketFulfillmentStepBizModel extends CrudBizModel<ErpCsTicketFulfillmentStep> implements IErpCsTicketFulfillmentStepBiz{
    public ErpCsTicketFulfillmentStepBizModel(){
        setEntityName(ErpCsTicketFulfillmentStep.class.getName());
    }
}
