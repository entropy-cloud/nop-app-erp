
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsCannedCategoryBiz;
import app.erp.cs.dao.entity.ErpCsCannedCategory;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsCannedCategory")
public class ErpCsCannedCategoryBizModel extends CrudBizModel<ErpCsCannedCategory> implements IErpCsCannedCategoryBiz{
    public ErpCsCannedCategoryBizModel(){
        setEntityName(ErpCsCannedCategory.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsCannedCategory.class)
    public List<String> orgName(@ContextSource List<ErpCsCannedCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCannedCategory row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsCannedCategory.class)
    public List<String> parentName(@ContextSource List<ErpCsCannedCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCannedCategory row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

}
