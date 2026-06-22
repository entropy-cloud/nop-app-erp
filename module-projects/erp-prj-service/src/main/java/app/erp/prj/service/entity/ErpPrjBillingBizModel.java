
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBillingBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;

@BizModel("ErpPrjBilling")
public class ErpPrjBillingBizModel extends CrudBizModel<ErpPrjBilling> implements IErpPrjBillingBiz{
    public ErpPrjBillingBizModel(){
        setEntityName(ErpPrjBilling.class.getName());
    }
}
