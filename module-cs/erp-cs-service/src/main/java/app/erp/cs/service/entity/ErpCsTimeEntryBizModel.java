
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTimeEntryBiz;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsTimeEntry")
public class ErpCsTimeEntryBizModel extends CrudBizModel<ErpCsTimeEntry> implements IErpCsTimeEntryBiz{
    public ErpCsTimeEntryBizModel(){
        setEntityName(ErpCsTimeEntry.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsTimeEntry.class)
    public List<String> orgName(@ContextSource List<ErpCsTimeEntry> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsTimeEntry row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsTimeEntry.class)
    public List<String> ticketCode(@ContextSource List<ErpCsTimeEntry> rows) {
        orm().batchLoadProps(rows, Collections.singleton("ticket"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsTimeEntry row : rows) {
            result.add(row.orm_attached() && row.getTicket() != null ? row.getTicket().getCode() : null);
        }
        return result;
    }

}
