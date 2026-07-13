
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdExchangeRateBiz;
import app.erp.md.dao.entity.ErpMdExchangeRate;

@BizModel("ErpMdExchangeRate")
public class ErpMdExchangeRateBizModel extends CrudBizModel<ErpMdExchangeRate> implements IErpMdExchangeRateBiz{
    public ErpMdExchangeRateBizModel(){
        setEntityName(ErpMdExchangeRate.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdExchangeRate.class)
    public List<String> fromCurrencyName(@ContextSource List<ErpMdExchangeRate> rows) {
        orm().batchLoadProps(rows, Collections.singleton("fromCurrency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdExchangeRate row : rows) {
            result.add(row.orm_attached() && row.getFromCurrency() != null ? row.getFromCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdExchangeRate.class)
    public List<String> toCurrencyName(@ContextSource List<ErpMdExchangeRate> rows) {
        orm().batchLoadProps(rows, Collections.singleton("toCurrency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdExchangeRate row : rows) {
            result.add(row.orm_attached() && row.getToCurrency() != null ? row.getToCurrency().getName() : null);
        }
        return result;
    }

}
