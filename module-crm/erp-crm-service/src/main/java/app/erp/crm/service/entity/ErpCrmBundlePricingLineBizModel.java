
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmBundlePricingLineBiz;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;

@BizModel("ErpCrmBundlePricingLine")
public class ErpCrmBundlePricingLineBizModel extends CrudBizModel<ErpCrmBundlePricingLine> implements IErpCrmBundlePricingLineBiz{
    public ErpCrmBundlePricingLineBizModel(){
        setEntityName(ErpCrmBundlePricingLine.class.getName());
    }
}
