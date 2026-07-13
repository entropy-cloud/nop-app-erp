
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmLeadConvLogBiz;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmLeadConvLog")
public class ErpCrmLeadConvLogBizModel extends CrudBizModel<ErpCrmLeadConvLog> implements IErpCrmLeadConvLogBiz{
    public ErpCrmLeadConvLogBizModel(){
        setEntityName(ErpCrmLeadConvLog.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmLeadConvLog.class)
    public List<String> leadCode(@ContextSource List<ErpCrmLeadConvLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("lead"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadConvLog row : rows) {
            result.add(row.orm_attached() && row.getLead() != null ? row.getLead().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadConvLog.class)
    public List<String> orgName(@ContextSource List<ErpCrmLeadConvLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadConvLog row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadConvLog.class)
    public List<String> fromStageName(@ContextSource List<ErpCrmLeadConvLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("fromStage"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadConvLog row : rows) {
            result.add(row.orm_attached() && row.getFromStage() != null ? row.getFromStage().getStageName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadConvLog.class)
    public List<String> toStageName(@ContextSource List<ErpCrmLeadConvLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("toStage"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadConvLog row : rows) {
            result.add(row.orm_attached() && row.getToStage() != null ? row.getToStage().getStageName() : null);
        }
        return result;
    }

}
