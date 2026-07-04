
package app.erp.fin.biz;

import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpFinBankStatementLineBiz extends ICrudBiz<ErpFinBankStatementLine> {

    /**
     * 对指定对账单的所有 UNMATCHED 行执行自动勾对算法：按金额、借贷反向、日期窗口、
     * 命中资金账户科目的已过账凭证行候选匹配。唯一命中 → MATCHED；多候选 → UNMATCHED；
     * 金额一致但凭据不唯一 → SUSPENSE。
     */
    @BizMutation
    BankStatementMatchResult autoMatch(@Name("statementId") Long statementId, IServiceContext context);

    /**
     * 手工勾对：将指定银行流水行标记为 MANUAL_MATCHED 并回写 matchedLineId。
     */
    @BizMutation
    ErpFinBankStatementLine manualMatch(@Name("lineId") Long lineId,
                                        @Name("voucherLineId") Long voucherLineId,
                                        IServiceContext context);
}
