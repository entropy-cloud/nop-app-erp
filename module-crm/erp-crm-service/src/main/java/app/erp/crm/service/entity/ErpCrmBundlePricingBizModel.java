
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmBundlePricingBiz;
import app.erp.crm.dao.entity.ErpCrmBundlePricing;

@BizModel("ErpCrmBundlePricing")
public class ErpCrmBundlePricingBizModel extends CrudBizModel<ErpCrmBundlePricing> implements IErpCrmBundlePricingBiz{
    public ErpCrmBundlePricingBizModel(){
        setEntityName(ErpCrmBundlePricing.class.getName());
    }
}
