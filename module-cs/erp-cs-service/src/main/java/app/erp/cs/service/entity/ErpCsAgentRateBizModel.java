
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsAgentRateBiz;
import app.erp.cs.dao.entity.ErpCsAgentRate;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsAgentRate")
public class ErpCsAgentRateBizModel extends CrudBizModel<ErpCsAgentRate> implements IErpCsAgentRateBiz{
    public ErpCsAgentRateBizModel(){
        setEntityName(ErpCsAgentRate.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsAgentRate.class)
    public List<String> orgName(@ContextSource List<ErpCsAgentRate> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsAgentRate row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
