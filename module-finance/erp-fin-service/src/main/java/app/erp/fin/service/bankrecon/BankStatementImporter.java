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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

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

    public ErpFinBankStatement importStatement(Long fundAccountId, LocalDate statementDate,
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
        head.setImportTime(LocalDateTime.now());
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

    protected ErpFinFundAccount requireBankAccount(Long fundAccountId) {
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

    protected void validateLine(BankStatementLineInput in, boolean strictRefNo, Long fundAccountId) {
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
    protected void assertNoDuplicates(Long fundAccountId, List<BankStatementLineInput> lines) {
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
    protected boolean existsByRefNo(Long fundAccountId, String refNo) {
        Long statementId = findStatementIdByAccount(fundAccountId);
        if (statementId == null) {
            return false;
        }
        return countLinesByFilter(and(eq("statementId", statementId), eq("refNo", refNo))) > 0;
    }

    protected boolean existsByComposite(Long fundAccountId, LocalDate txnDate, BigDecimal amount, String dcDirection) {
        Long statementId = findStatementIdByAccount(fundAccountId);
        if (statementId == null) {
            return false;
        }
        return countLinesByFilter(and(eq("statementId", statementId),
                eq("transactionDate", txnDate), eq("amount", amount), eq("dcDirection", dcDirection))) > 0;
    }

    protected Long findStatementIdByAccount(Long fundAccountId) {
        IEntityDao<ErpFinBankStatement> dao = daoProvider.daoFor(ErpFinBankStatement.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("fundAccountId", fundAccountId));
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
