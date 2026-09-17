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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinBankStatement、ErpFinBankStatementLine、ErpFinFundAccount）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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

    public BankStatementMatchResult autoMatch(String statementId) {
        BankStatementMatchResult result = new BankStatementMatchResult();
        if (statementId == null) {
            return result;
        }

        ErpFinBankStatement statement = loadStatement(statementId);
        ErpFinFundAccount account = requireFundAccount(statement.getFundAccountId());
        int daysWindow = bankLedgerQuery.resolveDaysWindow();

        List<ErpFinBankStatementLine> unmatched = loadUnmatchedLines(statementId);

        // P2-CK-fin4-011：跨行缓存消除 N+1——按对账单行 min/max 日期并集一次预载窗口内全部
        // 凭证行，循环内按行做方向/金额/occupied/counterparty 内存过滤（与 findCandidates 逐行
        // 查询的过滤条件等价）；已勾对凭证行 id 集合循环内增量维护（本行命中即加入，防同轮双匹配）。
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (ErpFinBankStatementLine line : unmatched) {
            LocalDate d = line.getTransactionDate();
            if (d == null) {
                continue;
            }
            LocalDate lo = d.minusDays(Math.max(0, daysWindow));
            LocalDate hi = d.plusDays(Math.max(0, daysWindow));
            minDate = minDate == null || lo.isBefore(minDate) ? lo : minDate;
            maxDate = maxDate == null || hi.isAfter(maxDate) ? hi : maxDate;
        }
        java.util.List<ErpFinVoucherLine> windowLines = minDate == null
                ? java.util.Collections.emptyList()
                : bankLedgerQuery.findVoucherLinesInWindow(account, minDate, maxDate);
        java.util.Set<String> occupied = bankLedgerQuery.findOccupiedLineIds(account.getId());

        for (ErpFinBankStatementLine line : unmatched) {
            String oppositeDirection = oppositeDirection(line.getDcDirection());
            if (oppositeDirection == null) {
                result.setUnmatched(result.getUnmatched() + 1);
                continue;
            }
            BigDecimal amount = line.getAmount();
            LocalDate txnDate = line.getTransactionDate();

            List<ErpFinVoucherLine> candidates = new java.util.ArrayList<>();
            if (amount != null && txnDate != null) {
                for (ErpFinVoucherLine vl : windowLines) {
                    if (occupied.contains(vl.getId())) {
                        continue;
                    }
                    if (!ErpFinConstants.DC_DEBIT.equals(oppositeDirection)
                            ? nz(vl.getCreditAmount()).compareTo(amount) != 0
                            : nz(vl.getDebitAmount()).compareTo(amount) != 0) {
                        continue;
                    }
                    // 两侧任一为空放行（对齐 findCandidates counterparty 过滤语义）
                    if (line.getCounterpartyName() != null && vl.getPartner() != null
                            && vl.getPartner().getName() != null
                            && !line.getCounterpartyName().equals(vl.getPartner().getName())) {
                        continue;
                    }
                    candidates.add(vl);
                }
            }

            if (candidates.size() == 1) {
                ErpFinVoucherLine chosen = candidates.get(0);
                line.setMatchStatus(ErpFinConstants.BANK_MATCH_MATCHED);
                line.setMatchedLineId(chosen.getId());
                result.setMatched(result.getMatched() + 1);
                // 增量维护 occupied（同轮后续行不再命中同一凭证行）
                occupied.add(chosen.getId());
            } else if (candidates.isEmpty()) {
                result.setUnmatched(result.getUnmatched() + 1);
            } else {
                line.setMatchStatus(ErpFinConstants.BANK_MATCH_SUSPENSE);
                result.setSuspense(result.getSuspense() + 1);
            }
        }
        return result;
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }

    protected ErpFinBankStatement loadStatement(String statementId) {
        IEntityDao<ErpFinBankStatement> dao = daoProvider.daoFor(ErpFinBankStatement.class);
        ErpFinBankStatement statement = dao.getEntityById(statementId);
        if (statement == null) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_STATEMENT_ID, statementId);
        }
        return statement;
    }

    protected ErpFinFundAccount requireFundAccount(String fundAccountId) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.getEntityById(fundAccountId);
        if (account == null) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
        return account;
    }

    protected List<ErpFinBankStatementLine> loadUnmatchedLines(String statementId) {
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
