
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceAssignmentBiz;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmSequenceAssignment")
public class ErpCrmSequenceAssignmentBizModel extends CrudBizModel<ErpCrmSequenceAssignment> implements IErpCrmSequenceAssignmentBiz{
    public ErpCrmSequenceAssignmentBizModel(){
        setEntityName(ErpCrmSequenceAssignment.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmSequenceAssignment.class)
    public List<String> orgName(@ContextSource List<ErpCrmSequenceAssignment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmSequenceAssignment row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmSequenceAssignment.class)
    public List<String> sequenceName(@ContextSource List<ErpCrmSequenceAssignment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sequence"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmSequenceAssignment row : rows) {
            result.add(row.orm_attached() && row.getSequence() != null ? row.getSequence().getName() : null);
        }
        return result;
    }

}
