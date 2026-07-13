
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogCarrierBiz;
import app.erp.log.dao.entity.ErpLogCarrier;

@BizModel("ErpLogCarrier")
public class ErpLogCarrierBizModel extends CrudBizModel<ErpLogCarrier> implements IErpLogCarrierBiz{
    public ErpLogCarrierBizModel(){
        setEntityName(ErpLogCarrier.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpLogCarrier.class)
    public List<String> partnerName(@ContextSource List<ErpLogCarrier> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogCarrier row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpLogCarrier.class)
    public List<String> orgName(@ContextSource List<ErpLogCarrier> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogCarrier row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
