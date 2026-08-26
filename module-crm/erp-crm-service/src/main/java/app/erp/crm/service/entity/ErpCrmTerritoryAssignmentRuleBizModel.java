
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmTerritoryAssignmentRuleBiz;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import java.util.List;

@BizModel("ErpCrmTerritoryAssignmentRule")
public class ErpCrmTerritoryAssignmentRuleBizModel extends AbstractErpCrudBizModel<ErpCrmTerritoryAssignmentRule> implements IErpCrmTerritoryAssignmentRuleBiz{
    public ErpCrmTerritoryAssignmentRuleBizModel(){
        setEntityName(ErpCrmTerritoryAssignmentRule.class.getName());
    }

    

}
