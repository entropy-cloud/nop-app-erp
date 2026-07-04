
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.bankrecon.BankReconciliationBuilder;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

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
}
