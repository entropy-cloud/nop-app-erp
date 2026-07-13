
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsSlaPolicyBiz;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsSlaPolicy")
public class ErpCsSlaPolicyBizModel extends CrudBizModel<ErpCsSlaPolicy> implements IErpCsSlaPolicyBiz{
    public ErpCsSlaPolicyBizModel(){
        setEntityName(ErpCsSlaPolicy.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsSlaPolicy.class)
    public List<String> ticketTypeName(@ContextSource List<ErpCsSlaPolicy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("ticketType"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsSlaPolicy row : rows) {
            result.add(row.orm_attached() && row.getTicketType() != null ? row.getTicketType().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsSlaPolicy.class)
    public List<String> teamName(@ContextSource List<ErpCsSlaPolicy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("team"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsSlaPolicy row : rows) {
            result.add(row.orm_attached() && row.getTeam() != null ? row.getTeam().getName() : null);
        }
        return result;
    }

}
