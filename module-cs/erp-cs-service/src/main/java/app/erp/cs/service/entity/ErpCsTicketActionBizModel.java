
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsTicketAction")
public class ErpCsTicketActionBizModel extends CrudBizModel<ErpCsTicketAction> implements IErpCsTicketActionBiz{
    public ErpCsTicketActionBizModel(){
        setEntityName(ErpCsTicketAction.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsTicketAction.class)
    public List<String> ticketCode(@ContextSource List<ErpCsTicketAction> rows) {
        orm().batchLoadProps(rows, Collections.singleton("ticket"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsTicketAction row : rows) {
            result.add(row.orm_attached() && row.getTicket() != null ? row.getTicket().getCode() : null);
        }
        return result;
    }

}
