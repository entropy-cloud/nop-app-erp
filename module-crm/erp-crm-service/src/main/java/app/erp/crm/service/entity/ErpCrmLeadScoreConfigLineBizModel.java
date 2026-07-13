
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadScoreConfigLineBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmLeadScoreConfigLine")
public class ErpCrmLeadScoreConfigLineBizModel extends CrudBizModel<ErpCrmLeadScoreConfigLine> implements IErpCrmLeadScoreConfigLineBiz{
    public ErpCrmLeadScoreConfigLineBizModel(){
        setEntityName(ErpCrmLeadScoreConfigLine.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmLeadScoreConfigLine.class)
    public List<String> configName(@ContextSource List<ErpCrmLeadScoreConfigLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("config"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadScoreConfigLine row : rows) {
            result.add(row.orm_attached() && row.getConfig() != null ? row.getConfig().getConfigName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadScoreConfigLine.class)
    public List<String> orgName(@ContextSource List<ErpCrmLeadScoreConfigLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadScoreConfigLine row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
