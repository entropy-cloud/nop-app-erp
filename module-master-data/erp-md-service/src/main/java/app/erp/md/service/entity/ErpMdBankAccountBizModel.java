
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdBankAccountBiz;
import app.erp.md.dao.entity.ErpMdBankAccount;

@BizModel("ErpMdBankAccount")
public class ErpMdBankAccountBizModel extends CrudBizModel<ErpMdBankAccount> implements IErpMdBankAccountBiz{
    public ErpMdBankAccountBizModel(){
        setEntityName(ErpMdBankAccount.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdBankAccount.class)
    public List<String> partnerName(@ContextSource List<ErpMdBankAccount> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdBankAccount row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

}
