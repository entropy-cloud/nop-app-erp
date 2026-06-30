
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsEntitlementBiz;
import app.erp.cs.dao.entity.ErpCsEntitlement;

@BizModel("ErpCsEntitlement")
public class ErpCsEntitlementBizModel extends CrudBizModel<ErpCsEntitlement> implements IErpCsEntitlementBiz{
    public ErpCsEntitlementBizModel(){
        setEntityName(ErpCsEntitlement.class.getName());
    }
}
