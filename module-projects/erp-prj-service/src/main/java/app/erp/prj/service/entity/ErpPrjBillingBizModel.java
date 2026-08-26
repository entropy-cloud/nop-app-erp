
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjBillingBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;

import java.util.List;

@BizModel("ErpPrjBilling")
public class ErpPrjBillingBizModel extends AbstractErpCrudBizModel<ErpPrjBilling> implements IErpPrjBillingBiz{
    public ErpPrjBillingBizModel(){
        setEntityName(ErpPrjBilling.class.getName());
    }

}
