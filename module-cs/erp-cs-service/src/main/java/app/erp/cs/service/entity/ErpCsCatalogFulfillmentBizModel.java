
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsCatalogFulfillmentBiz;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;

@BizModel("ErpCsCatalogFulfillment")
public class ErpCsCatalogFulfillmentBizModel extends CrudBizModel<ErpCsCatalogFulfillment> implements IErpCsCatalogFulfillmentBiz{
    public ErpCsCatalogFulfillmentBizModel(){
        setEntityName(ErpCsCatalogFulfillment.class.getName());
    }
}
