
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceStepBiz;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmSequenceStep")
public class ErpCrmSequenceStepBizModel extends CrudBizModel<ErpCrmSequenceStep> implements IErpCrmSequenceStepBiz{
    public ErpCrmSequenceStepBizModel(){
        setEntityName(ErpCrmSequenceStep.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmSequenceStep.class)
    public List<String> sequenceName(@ContextSource List<ErpCrmSequenceStep> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sequence"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmSequenceStep row : rows) {
            result.add(row.orm_attached() && row.getSequence() != null ? row.getSequence().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmSequenceStep.class)
    public List<String> orgName(@ContextSource List<ErpCrmSequenceStep> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmSequenceStep row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
