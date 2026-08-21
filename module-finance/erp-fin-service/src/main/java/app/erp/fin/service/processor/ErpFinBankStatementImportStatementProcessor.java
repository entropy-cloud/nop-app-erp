package app.erp.fin.service.processor;

import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.service.bankrecon.BankStatementImporter;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

/**
 * ErpFinBankStatement importStatement per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含对账单导入编排，委派 {@link BankStatementImporter}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBankStatementImportStatementProcessor {

    @Inject
    BankStatementImporter bankStatementImporter;

    public ErpFinBankStatement importStatement(String fundAccountId, LocalDate statementDate,
                                               List<BankStatementLineInput> lines, IServiceContext context) {
        return bankStatementImporter.importStatement(fundAccountId, statementDate, lines);
    }
}
