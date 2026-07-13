
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinFundAccountBiz;
import app.erp.fin.dao.entity.ErpFinFundAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpFinFundAccount")
public class ErpFinFundAccountBizModel extends CrudBizModel<ErpFinFundAccount> implements IErpFinFundAccountBiz{
    public ErpFinFundAccountBizModel(){
        setEntityName(ErpFinFundAccount.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinFundAccount.class)
    public List<String> orgName(@ContextSource List<ErpFinFundAccount> accounts) {
        orm().batchLoadProps(accounts, Collections.singleton("org"));
        List<String> result = new ArrayList<>(accounts.size());
        for (ErpFinFundAccount account : accounts) {
            result.add(account.getOrg() != null ? account.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinFundAccount.class)
    public List<String> subjectName(@ContextSource List<ErpFinFundAccount> accounts) {
        orm().batchLoadProps(accounts, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(accounts.size());
        for (ErpFinFundAccount account : accounts) {
            result.add(account.getSubject() != null ? account.getSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinFundAccount.class)
    public List<String> currencyName(@ContextSource List<ErpFinFundAccount> accounts) {
        orm().batchLoadProps(accounts, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(accounts.size());
        for (ErpFinFundAccount account : accounts) {
            result.add(account.getCurrency() != null ? account.getCurrency().getName() : null);
        }
        return result;
    }
}
