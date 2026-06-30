
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmQuotaBiz;
import app.erp.crm.dao.entity.ErpCrmQuota;

@BizModel("ErpCrmQuota")
public class ErpCrmQuotaBizModel extends CrudBizModel<ErpCrmQuota> implements IErpCrmQuotaBiz{
    public ErpCrmQuotaBizModel(){
        setEntityName(ErpCrmQuota.class.getName());
    }
}
