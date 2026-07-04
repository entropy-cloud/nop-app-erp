
package app.erp.fin.biz;

import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

public interface IErpFinBankStatementBiz extends ICrudBiz<ErpFinBankStatement> {

    /**
     * 导入银行对账单（头 + 行）。幂等去重：refNo 优先，缺失回退
     * {@code (transactionDate, amount, dcDirection)} 组合键，严格度经
     * {@code erp-fin.bank-import-strict-refno} 配置。
     *
     * <p>行初始化 {@code matchStatus=UNMATCHED}；头 {@code docStatus=DRAFT}。
     *
     * @return 落库的银行对账单头
     */
    @BizMutation
    ErpFinBankStatement importStatement(@Name("fundAccountId") Long fundAccountId,
                                        @Name("statementDate") LocalDate statementDate,
                                        @Name("lines") List<BankStatementLineInput> lines,
                                        IServiceContext context);
}
