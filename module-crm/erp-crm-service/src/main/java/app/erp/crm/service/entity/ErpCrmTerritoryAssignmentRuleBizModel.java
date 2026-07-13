
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmTerritoryAssignmentRuleBiz;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmTerritoryAssignmentRule")
public class ErpCrmTerritoryAssignmentRuleBizModel extends CrudBizModel<ErpCrmTerritoryAssignmentRule> implements IErpCrmTerritoryAssignmentRuleBiz{
    public ErpCrmTerritoryAssignmentRuleBizModel(){
        setEntityName(ErpCrmTerritoryAssignmentRule.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmTerritoryAssignmentRule.class)
    public List<String> orgName(@ContextSource List<ErpCrmTerritoryAssignmentRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmTerritoryAssignmentRule row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmTerritoryAssignmentRule.class)
    public List<String> territoryName(@ContextSource List<ErpCrmTerritoryAssignmentRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("territory"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmTerritoryAssignmentRule row : rows) {
            result.add(row.orm_attached() && row.getTerritory() != null ? row.getTerritory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmTerritoryAssignmentRule.class)
    public List<String> teamName(@ContextSource List<ErpCrmTerritoryAssignmentRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("team"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmTerritoryAssignmentRule row : rows) {
            result.add(row.orm_attached() && row.getTeam() != null ? row.getTeam().getName() : null);
        }
        return result;
    }

}
