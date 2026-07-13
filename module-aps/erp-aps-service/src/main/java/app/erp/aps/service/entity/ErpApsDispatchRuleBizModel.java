
package app.erp.aps.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsDispatchRuleBiz;
import app.erp.aps.dao.entity.ErpApsDispatchRule;

@BizModel("ErpApsDispatchRule")
public class ErpApsDispatchRuleBizModel extends CrudBizModel<ErpApsDispatchRule> implements IErpApsDispatchRuleBiz{
    public ErpApsDispatchRuleBizModel(){
        setEntityName(ErpApsDispatchRule.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpApsDispatchRule.class)
    public List<String> orgName(@ContextSource List<ErpApsDispatchRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpApsDispatchRule row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
