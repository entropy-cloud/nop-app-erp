package app.erp.fin.service.processor;

import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.service.bankrecon.BankStatementMatcher;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinBankStatementLine autoMatch per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含自动勾对编排，委派 {@link BankStatementMatcher}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBankStatementLineAutoMatchProcessor {

    @Inject
    BankStatementMatcher bankStatementMatcher;

    public BankStatementMatchResult autoMatch(Long statementId, IServiceContext context) {
        return bankStatementMatcher.autoMatch(statementId);
    }
}
