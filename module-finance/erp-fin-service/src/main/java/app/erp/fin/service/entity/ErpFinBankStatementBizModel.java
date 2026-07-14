
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.service.bankrecon.BankStatementImporter;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpFinBankStatement")
public class ErpFinBankStatementBizModel extends CrudBizModel<ErpFinBankStatement> implements IErpFinBankStatementBiz {
    public ErpFinBankStatementBizModel() {
        setEntityName(ErpFinBankStatement.class.getName());
    }

    @jakarta.inject.Inject
    BankStatementImporter bankStatementImporter;

    @Override
    @BizMutation
    public ErpFinBankStatement importStatement(@Name("fundAccountId") Long fundAccountId,
                                                @Name("statementDate") LocalDate statementDate,
                                                @Name("lines") List<BankStatementLineInput> lines,
                                                IServiceContext context) {
        return bankStatementImporter.importStatement(fundAccountId, statementDate, lines);
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinBankStatement.class)
    public List<String> orgName(@ContextSource List<ErpFinBankStatement> statements) {
        orm().batchLoadProps(statements, Collections.singleton("org"));
        List<String> result = new ArrayList<>(statements.size());
        for (ErpFinBankStatement statement : statements) {
            result.add(statement.getOrg() != null ? statement.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBankStatement.class)
    public List<String> fundAccountName(@ContextSource List<ErpFinBankStatement> statements) {
        orm().batchLoadProps(statements, Collections.singleton("fundAccount"));
        List<String> result = new ArrayList<>(statements.size());
        for (ErpFinBankStatement statement : statements) {
            result.add(statement.getFundAccount() != null ? statement.getFundAccount().getName() : null);
        }
        return result;
    }
}
