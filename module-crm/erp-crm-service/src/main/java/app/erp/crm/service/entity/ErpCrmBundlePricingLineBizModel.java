
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmBundlePricingLineBiz;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmBundlePricingLine")
public class ErpCrmBundlePricingLineBizModel extends CrudBizModel<ErpCrmBundlePricingLine> implements IErpCrmBundlePricingLineBiz{
    public ErpCrmBundlePricingLineBizModel(){
        setEntityName(ErpCrmBundlePricingLine.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmBundlePricingLine.class)
    public List<String> bundleName(@ContextSource List<ErpCrmBundlePricingLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("bundle"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmBundlePricingLine row : rows) {
            result.add(row.orm_attached() && row.getBundle() != null ? row.getBundle().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmBundlePricingLine.class)
    public List<String> orgName(@ContextSource List<ErpCrmBundlePricingLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmBundlePricingLine row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmBundlePricingLine.class)
    public List<String> materialName(@ContextSource List<ErpCrmBundlePricingLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("product"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmBundlePricingLine row : rows) {
            result.add(row.orm_attached() && row.getProduct() != null ? row.getProduct().getName() : null);
        }
        return result;
    }

}
