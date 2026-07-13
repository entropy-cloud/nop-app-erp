
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bMftConfigBiz;
import app.erp.b2b.dao.entity.ErpB2bMftConfig;

@BizModel("ErpB2bMftConfig")
public class ErpB2bMftConfigBizModel extends CrudBizModel<ErpB2bMftConfig> implements IErpB2bMftConfigBiz{
    public ErpB2bMftConfigBizModel(){
        setEntityName(ErpB2bMftConfig.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bMftConfig.class)
    public List<String> partnerName(@ContextSource List<ErpB2bMftConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bMftConfig row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bMftConfig.class)
    public List<String> orgName(@ContextSource List<ErpB2bMftConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bMftConfig row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bMftConfig.class)
    public List<String> certName(@ContextSource List<ErpB2bMftConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("cert"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bMftConfig row : rows) {
            result.add(row.orm_attached() && row.getCert() != null ? row.getCert().getCertName() : null);
        }
        return result;
    }

}
