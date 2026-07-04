package app.erp.fin.service.bankrecon;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.dao.entity.ErpFinBankReconciliationLine;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 余额调节表生成/过账/红冲编排（plan Phase 3）。Facade {@code ErpFinBankReconciliationBizModel} 委托本类。
 *
 * <p>恒等式（{@code §业务规则9}）：{@code bankBalance + 在途 = bookBalance + 银行已记企业未记}，
 * 其中：银行已记企业未记 = 调节时点银行对账单中 UNMATCHED 行金额（按方向净值）；
 * 在途 = 调节时点账面已记但银行未记的部分（本次实现以已勾对银行行的反向金额近似承载，
 * 完整在途需对照资金账户凭证行集合——Non-Goal，本计划聚焦平衡恒等校验）。
 *
 * <p>期间门控：对账单所属期间 glStatus=CLOSED 时拒绝 generate。
 */
public class BankReconciliationBuilder {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BankReconAdjustmentVoucherBuilder adjustmentVoucherBuilder;

    public ErpFinBankReconciliation generate(Long statementId) {
        ErpFinBankStatement statement = requireStatement(statementId);
        ErpFinFundAccount account = requireFundAccount(statement.getFundAccountId());
        assertPeriodNotClosed(statement.getStatementDate(), statementId);

        List<ErpFinBankStatementLine> lines = loadStatementLines(statementId);
        BigDecimal bankCreditUnrecorded = BigDecimal.ZERO;
        BigDecimal bankDebitUnrecorded = BigDecimal.ZERO;
        for (ErpFinBankStatementLine l : lines) {
            if (ErpFinConstants.BANK_MATCH_UNMATCHED.equals(l.getMatchStatus())) {
                if (ErpFinConstants.DC_CREDIT.equals(l.getDcDirection())) {
                    bankCreditUnrecorded = bankCreditUnrecorded.add(nz(l.getAmount()));
                } else if (ErpFinConstants.DC_DEBIT.equals(l.getDcDirection())) {
                    bankDebitUnrecorded = bankDebitUnrecorded.add(nz(l.getAmount()));
                }
            }
        }

        BigDecimal bookBalance = nz(account.getCurrentBalance());
        BigDecimal statementBalance = nz(statement.getEndingBalance());
        // 恒等式（{@code §业务规则9}）：statementBalance + 在途 = bookBalance + 未达。
        // 本次以「未达 = UNMATCHED 银行行净值（CREDIT − DEBIT）」承载（在途为 Non-Goal 的精确推导，
        // 此处简化为 0）。等价变形：statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded。
        // 银行 CREDIT 未达（银行已收企业未收）→ statementBalance 比 bookBalance 高；银行 DEBIT 未达反向。
        BigDecimal netUnrecorded = bankCreditUnrecorded.subtract(bankDebitUnrecorded);
        BigDecimal statementMinusBook = statementBalance.subtract(bookBalance);
        BigDecimal diff = statementMinusBook.subtract(netUnrecorded);
        BigDecimal precision = reconcilePrecision();
        boolean balanced = diff.abs().compareTo(precision) <= 0;

        if (!balanced) {
            throw new NopException(ErpFinErrors.ERR_BANK_RECON_NOT_BALANCED)
                    .param(ErpFinErrors.ARG_BOOK_BALANCE, bookBalance)
                    .param(ErpFinErrors.ARG_STATEMENT_BALANCE, statementBalance)
                    .param(ErpFinErrors.ARG_UNRECONCILED_DIFF, diff);
        }

        IEntityDao<ErpFinBankReconciliation> reconDao = daoProvider.daoFor(ErpFinBankReconciliation.class);
        ErpFinBankReconciliation recon = reconDao.newEntity();
        recon.setCode("BRR-" + StringHelper.generateUUID().substring(0, 12));
        recon.setOrgId(statement.getOrgId());
        recon.setFundAccountId(account.getId());
        recon.setStatementId(statement.getId());
        recon.setReconciliationDate(statement.getStatementDate());
        recon.setBookBalance(bookBalance);
        recon.setStatementBalance(statementBalance);
        recon.setUnreconciledDiff(BigDecimal.ZERO);
        recon.setIsBalanced(true);
        recon.setReconciledAt(CoreMetrics.currentDateTime());
        recon.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        reconDao.saveEntity(recon);

        // 调整行（未达账项登记）：每条 UNMATCHED 银行行写一条 adjustment line，便于审计与下月红冲溯源。
        IEntityDao<ErpFinBankReconciliationLine> adjDao = daoProvider.daoFor(ErpFinBankReconciliationLine.class);
        int lineNo = 1;
        for (ErpFinBankStatementLine l : lines) {
            if (!ErpFinConstants.BANK_MATCH_UNMATCHED.equals(l.getMatchStatus())) {
                continue;
            }
            ErpFinBankReconciliationLine adj = adjDao.newEntity();
            adj.setReconciliationId(recon.getId());
            adj.setLineNo(lineNo++);
            adj.setAdjustmentType("BANK_RECORDED_UNRECORDED");
            adj.setDescription(l.getDescription());
            adj.setDcDirection(l.getDcDirection());
            adj.setAmount(nz(l.getAmount()));
            adj.setSide(ErpFinConstants.BANK_RECON_SIDE_UNRECORDED);
            adjDao.saveEntity(adj);
        }
        return recon;
    }

    public ErpFinBankReconciliation post(Long reconciliationId, IServiceContext context) {
        ErpFinBankReconciliation recon = requireRecon(reconciliationId);
        if (!ErpFinConstants.VOUCHER_STATUS_DRAFT.equals(recon.getDocStatus())) {
            throw illegalTransition(recon, ErpFinConstants.VOUCHER_STATUS_DRAFT);
        }
        ErpFinFundAccount account = requireFundAccount(recon.getFundAccountId());
        List<ErpFinBankStatementLine> unmatched = loadUnmatchedStatementLines(recon.getStatementId());

        adjustmentVoucherBuilder.post(recon, account, unmatched, context);

        recon.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        return recon;
    }

    public ErpFinBankReconciliation reverse(Long reconciliationId, IServiceContext context) {
        ErpFinBankReconciliation recon = requireRecon(reconciliationId);
        if (!ErpFinConstants.VOUCHER_STATUS_POSTED.equals(recon.getDocStatus())) {
            throw illegalTransition(recon, ErpFinConstants.VOUCHER_STATUS_POSTED);
        }
        adjustmentVoucherBuilder.reverse(recon, context);

        recon.setDocStatus(ErpFinConstants.VOUCHER_STATUS_CANCELLED);
        return recon;
    }

    // ---------- helpers ----------

    protected ErpFinBankStatement requireStatement(Long statementId) {
        IEntityDao<ErpFinBankStatement> dao = daoProvider.daoFor(ErpFinBankStatement.class);
        ErpFinBankStatement s = dao.getEntityById(statementId);
        if (s == null) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_STATEMENT_ID, statementId);
        }
        return s;
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

    protected ErpFinBankReconciliation requireRecon(Long reconciliationId) {
        IEntityDao<ErpFinBankReconciliation> dao = daoProvider.daoFor(ErpFinBankReconciliation.class);
        ErpFinBankReconciliation recon = dao.getEntityById(reconciliationId);
        if (recon == null) {
            throw new NopException(ErpFinErrors.ERR_BANK_RECON_NOT_FOUND)
                    .param(ErpFinErrors.ARG_STATEMENT_CODE, String.valueOf(reconciliationId));
        }
        return recon;
    }

    protected List<ErpFinBankStatementLine> loadStatementLines(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statementId", statementId));
        return dao.findAllByQuery(q);
    }

    protected List<ErpFinBankStatementLine> loadUnmatchedStatementLines(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statementId", statementId));
        q.addFilter(eq("matchStatus", ErpFinConstants.BANK_MATCH_UNMATCHED));
        return dao.findAllByQuery(q);
    }

    protected void assertPeriodNotClosed(LocalDate statementDate, Long statementId) {
        ErpFinAccountingPeriod period = findPeriodByDate(statementDate);
        if (period == null) {
            return;
        }
        ErpFinAccountingPeriodStatus status = findPeriodStatus(period.getId());
        if (status != null && ErpFinConstants.PERIOD_STATUS_CLOSED.equals(status.getGlStatus())) {
            throw new NopException(ErpFinErrors.ERR_BANK_RECON_PERIOD_CLOSED)
                    .param(ErpFinErrors.ARG_STATEMENT_ID, statementId);
        }
    }

    protected ErpFinAccountingPeriod findPeriodByDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(le("startDate", date));
        q.addFilter(ge("endDate", date));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpFinAccountingPeriodStatus findPeriodStatus(Long periodId) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.setLimit(1);
        List<ErpFinAccountingPeriodStatus> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected BigDecimal reconcilePrecision() {
        BigDecimal p = AppConfig.var(ErpFinConstants.CONFIG_RECONCILE_PRECISION, new BigDecimal("0.01"));
        return p != null ? p : new BigDecimal("0.01");
    }

    protected NopException illegalTransition(ErpFinBankReconciliation recon, String expected) {
        return new NopException(ErpFinErrors.ERR_BANK_RECON_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_STATEMENT_CODE, recon.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, recon.getDocStatus())
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
