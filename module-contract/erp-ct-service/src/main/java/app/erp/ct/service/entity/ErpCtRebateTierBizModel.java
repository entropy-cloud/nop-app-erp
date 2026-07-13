
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateTierBiz;
import app.erp.contract.dao.entity.ErpCtRebateTier;

@BizModel("ErpCtRebateTier")
public class ErpCtRebateTierBizModel extends CrudBizModel<ErpCtRebateTier> implements IErpCtRebateTierBiz{
    public ErpCtRebateTierBizModel(){
        setEntityName(ErpCtRebateTier.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtRebateTier.class)
    public List<String> rebateAgreementName(@ContextSource List<ErpCtRebateTier> rows) {
        orm().batchLoadProps(rows, Collections.singleton("rebateAgreement"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtRebateTier row : rows) {
            result.add(row.orm_attached() && row.getRebateAgreement() != null ? row.getRebateAgreement().getCode() : null);
        }
        return result;
    }

}
