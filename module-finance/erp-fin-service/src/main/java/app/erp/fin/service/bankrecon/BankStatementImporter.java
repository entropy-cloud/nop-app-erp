package app.erp.fin.service.bankrecon;

import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.ITreeBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.api.core.time.CoreMetrics;

/**
 * 银行对账单导入器。校验资金账户类型（必须 BANK）+ 幂等去重 + 写头/行。
 *
 * <p>幂等去重：{@code refNo} 优先（银行参考号全局唯一），缺失回退
 * {@code (transactionDate, amount, dcDirection)} 组合键；严格度经
 * {@code erp-fin.bank-import-strict-refno} 配置（{@code true}=缺 refNo 拒绝）。
 *
 * <p>跨实体访问经 daoProvider（同 finance 域内 ErpFinFundAccount / ErpFinBankStatement / ErpFinBankStatementLine，
 * 无独立 IBiz）；行初始化 {@code matchStatus=UNMATCHED}。
 */
public class BankStatementImporter {

    @Inject
    IDaoProvider daoProvider;

    public ErpFinBankStatement importStatement(String fundAccountId, LocalDate statementDate,
                                                List<BankStatementLineInput> lines) {
        if (fundAccountId == null || statementDate == null || lines == null || lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }

        ErpFinFundAccount account = requireBankAccount(fundAccountId);
        boolean strictRefNo = AppConfig.var(ErpFinConstants.CONFIG_BANK_IMPORT_STRICT_REFNO, Boolean.FALSE);

        List<BankStatementLineInput> normalized = new ArrayList<>(lines.size());
        for (BankStatementLineInput in : lines) {
            validateLine(in, strictRefNo, fundAccountId);
            normalized.add(in);
        }
        assertNoDuplicates(account.getId(), normalized);

        IEntityDao<ErpFinBankStatement> headDao = daoProvider.daoFor(ErpFinBankStatement.class);
        ErpFinBankStatement head = headDao.newEntity();
        head.setCode("BST-" + StringHelper.generateUUID().substring(0, 12));
        head.setOrgId(account.getOrgId());
        head.setFundAccountId(account.getId());
        head.setStatementDate(statementDate);
        head.setBeginningBalance(account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO);
        head.setEndingBalance(account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO);
        head.setTotalDebit(BigDecimal.ZERO);
        head.setTotalCredit(BigDecimal.ZERO);
        head.setImportTime(CoreMetrics.currentTimestamp());
        head.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        headDao.saveEntity(head);

        IEntityDao<ErpFinBankStatementLine> lineDao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal lastBalance = head.getBeginningBalance();
        int lineNo = 1;
        for (BankStatementLineInput in : normalized) {
            ErpFinBankStatementLine line = lineDao.newEntity();
            line.setStatementId(head.getId());
            line.setLineNo(lineNo++);
            line.setTransactionDate(in.getTransactionDate());
            line.setDescription(in.getDescription());
            line.setRefNo(in.getRefNo());
            line.setDcDirection(in.getDcDirection());
            line.setAmount(in.getAmount());
            line.setCurrencyId(account.getCurrencyId());
            line.setBalanceAfter(in.getBalanceAfter() != null ? in.getBalanceAfter() : lastBalance);
            line.setCounterpartyAccount(in.getCounterpartyAccount());
            line.setCounterpartyName(in.getCounterpartyName());
            line.setCounterpartyBank(in.getCounterpartyBank());
            line.setMatchStatus(ErpFinConstants.BANK_MATCH_UNMATCHED);
            lineDao.saveEntity(line);

            if (ErpFinConstants.DC_DEBIT.equals(in.getDcDirection())) {
                totalDebit = totalDebit.add(nz(in.getAmount()));
            } else if (ErpFinConstants.DC_CREDIT.equals(in.getDcDirection())) {
                totalCredit = totalCredit.add(nz(in.getAmount()));
            }
            if (in.getBalanceAfter() != null) {
                lastBalance = in.getBalanceAfter();
            }
        }
        head.setTotalDebit(totalDebit);
        head.setTotalCredit(totalCredit);
        head.setEndingBalance(lastBalance);
        return head;
    }

    protected ErpFinFundAccount requireBankAccount(String fundAccountId) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.getEntityById(fundAccountId);
        if (account == null) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_FOUND)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
        if (!ErpFinConstants.FUND_ACCOUNT_TYPE_BANK.equals(account.getAccountType())) {
            throw new NopException(ErpFinErrors.ERR_FUND_ACCOUNT_NOT_BANK)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId)
                    .param(ErpFinErrors.ARG_ACCOUNT_TYPE, account.getAccountType());
        }
        return account;
    }

    protected void validateLine(BankStatementLineInput in, boolean strictRefNo, String fundAccountId) {
        if (in.getTransactionDate() == null || in.getDcDirection() == null
                || in.getAmount() == null || in.getAmount().signum() < 0) {
            throw new NopException(ErpFinErrors.ERR_BANK_IMPORT_LINE_INVALID)
                    .param(ErpFinErrors.ARG_REF_NO, in.getRefNo())
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
        if (!ErpFinConstants.DC_DEBIT.equals(in.getDcDirection())
                && !ErpFinConstants.DC_CREDIT.equals(in.getDcDirection())) {
            throw new NopException(ErpFinErrors.ERR_BANK_IMPORT_LINE_INVALID)
                    .param(ErpFinErrors.ARG_REF_NO, in.getRefNo())
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
        if (strictRefNo && StringHelper.isBlank(in.getRefNo())) {
            throw new NopException(ErpFinErrors.ERR_BANK_IMPORT_REFNO_REQUIRED)
                    .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
        }
    }

    /**
     * 校验待导入行无重复（本次批次内 + 与账户已有流水对比）。refNo 非空时按 refNo 去重，
     * 否则按 (transactionDate, amount, dcDirection) 组合键去重。
     */
    protected void assertNoDuplicates(String fundAccountId, List<BankStatementLineInput> lines) {
        Set<String> seenRefNo = new HashSet<>();
        Set<String> seenComposite = new HashSet<>();
        for (BankStatementLineInput in : lines) {
            String refNo = in.getRefNo();
            if (!StringHelper.isBlank(refNo)) {
                if (!seenRefNo.add(refNo)) {
                    throw new NopException(ErpFinErrors.ERR_BANK_STMT_DUPLICATE)
                            .param(ErpFinErrors.ARG_REF_NO, refNo)
                            .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
                }
                if (existsByRefNo(fundAccountId, refNo)) {
                    throw new NopException(ErpFinErrors.ERR_BANK_STMT_DUPLICATE)
                            .param(ErpFinErrors.ARG_REF_NO, refNo)
                            .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
                }
            } else {
                String composite = in.getTransactionDate() + "|" + in.getAmount().toPlainString()
                        + "|" + in.getDcDirection();
                if (!seenComposite.add(composite)) {
                    throw new NopException(ErpFinErrors.ERR_BANK_STMT_DUPLICATE)
                            .param(ErpFinErrors.ARG_REF_NO, refNo)
                            .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
                }
                if (existsByComposite(fundAccountId, in.getTransactionDate(), in.getAmount(), in.getDcDirection())) {
                    throw new NopException(ErpFinErrors.ERR_BANK_STMT_DUPLICATE)
                            .param(ErpFinErrors.ARG_REF_NO, refNo)
                            .param(ErpFinErrors.ARG_FUND_ACCOUNT_ID, fundAccountId);
                }
            }
        }
    }

    /** 经 statement.fundAccountId 关联反查银行流水行是否已存在同 refNo（避免全表扫描）。 */
    /**
     * P2-CK-fin4-009：账户级幂等——去重范围从「最近一张对账单」扩展到该账户**全部**对账单行
     *（statementId 列表 in 查询；bank-reconciliation.md §业务规则1 账户维度唯一键）。
     */
    protected boolean existsByRefNo(String fundAccountId, String refNo) {
        return countLinesAcrossStatements(fundAccountId, qb -> {
            qb.addFilter(eq("refNo", refNo));
            return qb;
        }) > 0;
    }

    protected boolean existsByComposite(String fundAccountId, LocalDate txnDate, BigDecimal amount, String dcDirection) {
        return countLinesAcrossStatements(fundAccountId, qb -> {
            qb.addFilter(eq("transactionDate", txnDate));
            qb.addFilter(eq("amount", amount));
            qb.addFilter(eq("dcDirection", dcDirection));
            return qb;
        }) > 0;
    }

    /** 账户全部对账单行按过滤条件计数（statementId 分批 in，防参数上限）。 */
    protected long countLinesAcrossStatements(String fundAccountId, java.util.function.Function<io.nop.api.core.beans.query.QueryBean, io.nop.api.core.beans.query.QueryBean> extraAppender) {
        IEntityDao<ErpFinBankStatement> stmtDao = daoProvider.daoFor(ErpFinBankStatement.class);
        QueryBean sq = new QueryBean();
        sq.addFilter(eq("fundAccountId", fundAccountId));
        List<String> statementIds = stmtDao.findAllByQuery(sq).stream()
                .map(ErpFinBankStatement::getId).collect(java.util.stream.Collectors.toList());
        if (statementIds.isEmpty()) {
            return 0;
        }
        IEntityDao<ErpFinBankStatementLine> lineDao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        long total = 0;
        int batch = 500;
        for (int i = 0; i < statementIds.size(); i += batch) {
            QueryBean q = new QueryBean();
            q.addFilter(in("statementId", statementIds.subList(i, Math.min(i + batch, statementIds.size()))));
            extraAppender.apply(q);
            total += lineDao.countByQuery(q);
        }
        return total;
    }

    protected String findStatementIdByAccount(String fundAccountId) {
        IEntityDao<ErpFinBankStatement> dao = daoProvider.daoFor(ErpFinBankStatement.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("fundAccountId", fundAccountId));
        // O-5：追加 statementDate DESC 确保确定性（取最近一条流水单）
        q.addOrderField("statementDate", true);
        q.setLimit(1);
        ErpFinBankStatement s = dao.findFirstByQuery(q);
        return s != null ? s.getId() : null;
    }

    protected long countLinesByFilter(ITreeBean filter) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(filter);
        return dao.findAllByQuery(q).size();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
