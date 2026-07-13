
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdSubjectMappingBiz;
import app.erp.md.dao.entity.ErpMdSubjectMapping;

@BizModel("ErpMdSubjectMapping")
public class ErpMdSubjectMappingBizModel extends CrudBizModel<ErpMdSubjectMapping> implements IErpMdSubjectMappingBiz{
    public ErpMdSubjectMappingBizModel(){
        setEntityName(ErpMdSubjectMapping.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdSubjectMapping.class)
    public List<String> sourceSubjectName(@ContextSource List<ErpMdSubjectMapping> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sourceSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdSubjectMapping row : rows) {
            result.add(row.orm_attached() && row.getSourceSubject() != null ? row.getSourceSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdSubjectMapping.class)
    public List<String> targetAcctSchemaName(@ContextSource List<ErpMdSubjectMapping> rows) {
        orm().batchLoadProps(rows, Collections.singleton("targetAcctSchema"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdSubjectMapping row : rows) {
            result.add(row.orm_attached() && row.getTargetAcctSchema() != null ? row.getTargetAcctSchema().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdSubjectMapping.class)
    public List<String> targetSubjectName(@ContextSource List<ErpMdSubjectMapping> rows) {
        orm().batchLoadProps(rows, Collections.singleton("targetSubject"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdSubjectMapping row : rows) {
            result.add(row.orm_attached() && row.getTargetSubject() != null ? row.getTargetSubject().getName() : null);
        }
        return result;
    }

}
