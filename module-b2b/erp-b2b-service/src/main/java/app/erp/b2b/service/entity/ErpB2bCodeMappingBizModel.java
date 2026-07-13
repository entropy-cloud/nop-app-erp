
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bCodeMappingBiz;
import app.erp.b2b.dao.entity.ErpB2bCodeMapping;

@BizModel("ErpB2bCodeMapping")
public class ErpB2bCodeMappingBizModel extends CrudBizModel<ErpB2bCodeMapping> implements IErpB2bCodeMappingBiz{
    public ErpB2bCodeMappingBizModel(){
        setEntityName(ErpB2bCodeMapping.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bCodeMapping.class)
    public List<String> orgName(@ContextSource List<ErpB2bCodeMapping> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bCodeMapping row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bCodeMapping.class)
    public List<String> partnerName(@ContextSource List<ErpB2bCodeMapping> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bCodeMapping row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

}
