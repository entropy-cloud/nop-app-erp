
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdCostCenterBiz;
import app.erp.md.dao.entity.ErpMdCostCenter;

@BizModel("ErpMdCostCenter")
public class ErpMdCostCenterBizModel extends CrudBizModel<ErpMdCostCenter> implements IErpMdCostCenterBiz{
    public ErpMdCostCenterBizModel(){
        setEntityName(ErpMdCostCenter.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdCostCenter.class)
    public List<String> orgName(@ContextSource List<ErpMdCostCenter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("organization"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdCostCenter row : rows) {
            result.add(row.orm_attached() && row.getOrganization() != null ? row.getOrganization().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdCostCenter.class)
    public List<String> managerName(@ContextSource List<ErpMdCostCenter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("manager"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdCostCenter row : rows) {
            result.add(row.orm_attached() && row.getManager() != null ? row.getManager().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdCostCenter.class)
    public List<String> parentName(@ContextSource List<ErpMdCostCenter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdCostCenter row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getCode() : null);
        }
        return result;
    }

}
