
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bTestExchangeBiz;
import app.erp.b2b.dao.entity.ErpB2bTestExchange;

@BizModel("ErpB2bTestExchange")
public class ErpB2bTestExchangeBizModel extends CrudBizModel<ErpB2bTestExchange> implements IErpB2bTestExchangeBiz{
    public ErpB2bTestExchangeBizModel(){
        setEntityName(ErpB2bTestExchange.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bTestExchange.class)
    public List<String> partnerProfileName(@ContextSource List<ErpB2bTestExchange> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partnerProfile"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bTestExchange row : rows) {
            result.add(row.orm_attached() && row.getPartnerProfile() != null ? row.getPartnerProfile().getCode() : null);
        }
        return result;
    }

}
