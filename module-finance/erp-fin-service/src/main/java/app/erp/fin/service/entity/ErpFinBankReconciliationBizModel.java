
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.bankrecon.BankReconciliationBuilder;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpFinBankReconciliation")
public class ErpFinBankReconciliationBizModel extends CrudBizModel<ErpFinBankReconciliation>
        implements IErpFinBankReconciliationBiz {
    public ErpFinBankReconciliationBizModel() {
        setEntityName(ErpFinBankReconciliation.class.getName());
    }

    @Inject
    BankReconciliationBuilder bankReconciliationBuilder;

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBankReconciliation generate(@Name("statementId") Long statementId, IServiceContext context) {
        return bankReconciliationBuilder.generate(statementId);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBankReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        return bankReconciliationBuilder.post(reconciliationId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBankReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        return bankReconciliationBuilder.reverse(reconciliationId, context);
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinBankReconciliation.class)
    public List<String> orgName(@ContextSource List<ErpFinBankReconciliation> reconciliations) {
        orm().batchLoadProps(reconciliations, Collections.singleton("org"));
        List<String> result = new ArrayList<>(reconciliations.size());
        for (ErpFinBankReconciliation recon : reconciliations) {
            result.add(recon.getOrg() != null ? recon.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBankReconciliation.class)
    public List<String> fundAccountName(@ContextSource List<ErpFinBankReconciliation> reconciliations) {
        orm().batchLoadProps(reconciliations, Collections.singleton("fundAccount"));
        List<String> result = new ArrayList<>(reconciliations.size());
        for (ErpFinBankReconciliation recon : reconciliations) {
            result.add(recon.getFundAccount() != null ? recon.getFundAccount().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBankReconciliation.class)
    public List<String> statementCode(@ContextSource List<ErpFinBankReconciliation> reconciliations) {
        orm().batchLoadProps(reconciliations, Collections.singleton("statement"));
        List<String> result = new ArrayList<>(reconciliations.size());
        for (ErpFinBankReconciliation recon : reconciliations) {
            result.add(recon.getStatement() != null ? recon.getStatement().getCode() : null);
        }
        return result;
    }
}
