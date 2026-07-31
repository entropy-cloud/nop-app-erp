package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.bankrecon.BankReconciliationBuilder;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinBankReconciliation post per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含调节表过账编排，委派 {@link BankReconciliationBuilder}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBankReconciliationPostProcessor {

    @Inject
    BankReconciliationBuilder bankReconciliationBuilder;

    public ErpFinBankReconciliation post(Long reconciliationId, IServiceContext context) {
        return bankReconciliationBuilder.post(reconciliationId, context);
    }
}
