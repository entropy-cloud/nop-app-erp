
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdOrganizationBiz;
import app.erp.md.dao.entity.ErpMdOrganization;

@BizModel("ErpMdOrganization")
public class ErpMdOrganizationBizModel extends CrudBizModel<ErpMdOrganization> implements IErpMdOrganizationBiz{
    public ErpMdOrganizationBizModel(){
        setEntityName(ErpMdOrganization.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdOrganization.class)
    public List<String> parentName(@ContextSource List<ErpMdOrganization> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdOrganization row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdOrganization.class)
    public List<String> functionalCurrencyName(@ContextSource List<ErpMdOrganization> rows) {
        orm().batchLoadProps(rows, Collections.singleton("functionalCurrency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdOrganization row : rows) {
            result.add(row.orm_attached() && row.getFunctionalCurrency() != null ? row.getFunctionalCurrency().getName() : null);
        }
        return result;
    }

}
