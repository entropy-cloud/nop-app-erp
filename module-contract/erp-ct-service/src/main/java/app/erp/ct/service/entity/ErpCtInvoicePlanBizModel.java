
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtInvoicePlanBiz;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;

@BizModel("ErpCtInvoicePlan")
public class ErpCtInvoicePlanBizModel extends CrudBizModel<ErpCtInvoicePlan> implements IErpCtInvoicePlanBiz{
    public ErpCtInvoicePlanBizModel(){
        setEntityName(ErpCtInvoicePlan.class.getName());
    }
}
