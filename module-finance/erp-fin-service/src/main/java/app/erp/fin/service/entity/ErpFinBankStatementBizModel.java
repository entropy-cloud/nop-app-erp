
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.service.bankrecon.BankStatementImporter;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
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
    @SingleSession
    public ErpFinBankStatement importStatement(@Name("fundAccountId") Long fundAccountId,
                                                @Name("statementDate") LocalDate statementDate,
                                                @Name("lines") List<BankStatementLineInput> lines,
                                                IServiceContext context) {
        return bankStatementImporter.importStatement(fundAccountId, statementDate, lines);
    }
}
