
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmTerritoryAssignmentRuleBiz;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;

@BizModel("ErpCrmTerritoryAssignmentRule")
public class ErpCrmTerritoryAssignmentRuleBizModel extends CrudBizModel<ErpCrmTerritoryAssignmentRule> implements IErpCrmTerritoryAssignmentRuleBiz{
    public ErpCrmTerritoryAssignmentRuleBizModel(){
        setEntityName(ErpCrmTerritoryAssignmentRule.class.getName());
    }
}
