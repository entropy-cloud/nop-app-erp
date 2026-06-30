
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmPriceRuleBiz;
import app.erp.crm.dao.entity.ErpCrmPriceRule;

@BizModel("ErpCrmPriceRule")
public class ErpCrmPriceRuleBizModel extends CrudBizModel<ErpCrmPriceRule> implements IErpCrmPriceRuleBiz{
    public ErpCrmPriceRuleBizModel(){
        setEntityName(ErpCrmPriceRule.class.getName());
    }
}
