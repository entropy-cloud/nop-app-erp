
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketTypeBiz;
import app.erp.cs.dao.entity.ErpCsTicketType;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsTicketType")
public class ErpCsTicketTypeBizModel extends CrudBizModel<ErpCsTicketType> implements IErpCsTicketTypeBiz{
    public ErpCsTicketTypeBizModel(){
        setEntityName(ErpCsTicketType.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsTicketType.class)
    public List<String> slaPolicyName(@ContextSource List<ErpCsTicketType> rows) {
        orm().batchLoadProps(rows, Collections.singleton("defaultSlaPolicy"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsTicketType row : rows) {
            result.add(row.orm_attached() && row.getDefaultSlaPolicy() != null ? row.getDefaultSlaPolicy().getName() : null);
        }
        return result;
    }

}
