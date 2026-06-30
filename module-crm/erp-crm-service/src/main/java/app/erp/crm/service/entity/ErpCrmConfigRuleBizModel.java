
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmConfigRuleBiz;
import app.erp.crm.dao.entity.ErpCrmConfigRule;

@BizModel("ErpCrmConfigRule")
public class ErpCrmConfigRuleBizModel extends CrudBizModel<ErpCrmConfigRule> implements IErpCrmConfigRuleBiz{
    public ErpCrmConfigRuleBizModel(){
        setEntityName(ErpCrmConfigRule.class.getName());
    }
}
