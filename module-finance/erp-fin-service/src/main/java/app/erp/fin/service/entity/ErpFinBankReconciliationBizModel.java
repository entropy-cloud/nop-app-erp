
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.bankrecon.BankReconciliationBuilder;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

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
    public ErpFinBankReconciliation generate(@Name("statementId") Long statementId, IServiceContext context) {
        return bankReconciliationBuilder.generate(statementId);
    }

    @Override
    @BizMutation
    public ErpFinBankReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        return bankReconciliationBuilder.post(reconciliationId, context);
    }

    @Override
    @BizMutation
    public ErpFinBankReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        return bankReconciliationBuilder.reverse(reconciliationId, context);
    }

}
