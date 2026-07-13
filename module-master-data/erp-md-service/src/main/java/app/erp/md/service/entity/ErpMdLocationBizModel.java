
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdLocationBiz;
import app.erp.md.dao.entity.ErpMdLocation;

@BizModel("ErpMdLocation")
public class ErpMdLocationBizModel extends CrudBizModel<ErpMdLocation> implements IErpMdLocationBiz{
    public ErpMdLocationBizModel(){
        setEntityName(ErpMdLocation.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdLocation.class)
    public List<String> warehouseName(@ContextSource List<ErpMdLocation> rows) {
        orm().batchLoadProps(rows, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdLocation row : rows) {
            result.add(row.orm_attached() && row.getWarehouse() != null ? row.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdLocation.class)
    public List<String> parentName(@ContextSource List<ErpMdLocation> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdLocation row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

}
