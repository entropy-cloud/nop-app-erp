package app.erp.fin.service.bankrecon;

import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 银行流水自动勾对算法（plan Phase 2）。
 *
 * <p>遍历对账单 UNMATCHED 行，调 {@link BankLedgerQuery#findCandidates} 按金额+借贷反向+日期窗口+科目过滤候选
 * 已过账 {@link ErpFinVoucherLine}：
 * <ul>
 *   <li>唯一命中 → {@code MATCHED} + 回写 {@code matchedLineId}。</li>
 *   <li>多候选 → 保持 {@code UNMATCHED}（等待人工或下一次匹配）。</li>
 *   <li>金额一致但凭据不唯一（按 refNo 反向部分匹配推断）→ {@code SUSPENSE}。</li>
 * </ul>
 *
 * <p>方向语义：银行 {@code DEBIT}(借方/扣款) ↔ 账面 {@code CREDIT}(贷方/资金流出)；
 * 银行 {@code CREDIT}(贷方/到账) ↔ 账面 {@code DEBIT}(借方/资金流入)。
 */
public class BankStatementMatcher {

    @Inject
    BankLedgerQuery bankLedgerQuery;
    @Inject
    IDaoProvider daoProvider;

    public BankStatementMatchResult autoMatch(Long statementId) {
        BankStatementMatchResult result = new BankStatementMatchResult();
        if (statementId == null) {
            return result;
        }

        ErpFinBankStatement statement = loadStatement(statementId);
        ErpFinFundAccount account = requireFundAccount(statement.getFundAccountId());
        int daysWindow = bankLedgerQuery.resolveDaysWindow();

        List<ErpFinBankStatementLine> unmatched = loadUnmatchedLines(statementId);
        for (ErpFinBankStatementLine line : unmatched) {
            String oppositeDirection = oppositeDirection(line.getDcDirection());
            if (oppositeDirection == null) {
                result.setUnmatched(result.getUnmatched() + 1);
                continue;
            }
            List<ErpFinVoucherLine> candidates = bankLedgerQuery.findCandidates(
                    account, line.getAmount(), oppositeDirection, line.getTransactionDate(), daysWindow);

            if (candidates.size() == 1) {
                ErpFinVoucherLine chosen = candidates.get(0);
                line.setMatchStatus(ErpFinConstants.BANK_MATCH_MATCHED);
                line.setMatchedLineId(chosen.getId());
                result.setMatched(result.getMatched() + 1);
            } else if (candidates.isEmpty()) {
                result.setUnmatched(result.getUnmatched() + 1);
            } else {
                line.setMatchStatus(ErpFinConstants.BANK_MATCH_SUSPENSE);
                result.setSuspense(result.getSuspense() + 1);
            }
        }
        return result;
    }

    protected ErpFinBankStatement loadStatement(Long statementId) {
        IEntityDao<ErpFinBankStatement> dao = daoProvider.daoFor(ErpFinBankStatement.class);
        ErpFinBankStatement statement = dao.getEntityById(statementId);
        if (statement == null) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_STATEMENT_ID, statementId);
        }
        return statement;
    }

    protected ErpFinFundAccount requireFundAccount(Long fundAccountId) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.getEntityById(fundAccountId);
        if (account == null) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
        return account;
    }

    protected List<ErpFinBankStatementLine> loadUnmatchedLines(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statementId", statementId));
        q.addFilter(eq("matchStatus", ErpFinConstants.BANK_MATCH_UNMATCHED));
        return dao.findAllByQuery(q);
    }

    /** 银行方向 → 账面反向。 */
    protected String oppositeDirection(String bankDirection) {
        if (ErpFinConstants.DC_DEBIT.equals(bankDirection)) {
            return ErpFinConstants.DC_CREDIT;
        }
        if (ErpFinConstants.DC_CREDIT.equals(bankDirection)) {
            return ErpFinConstants.DC_DEBIT;
        }
        return null;
    }
}
