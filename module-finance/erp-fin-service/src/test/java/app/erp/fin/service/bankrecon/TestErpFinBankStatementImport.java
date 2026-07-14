package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 银行对账单导入（{@link IErpFinBankStatementBiz#importStatement}）集成测试（Phase 1）。
 *
 * <p>覆盖：成功导入 + 行 UNMATCHED + 借贷合计回写；重复导入（同 refNo）拒绝 ERR_BANK_STMT_DUPLICATE；
 * 非 BANK 账户拒绝 ERR_FUND_ACCOUNT_NOT_BANK；strict-refno=true 缺 refNo 拒绝 ERR_BANK_IMPORT_REFNO_REQUIRED；
 * 组合键去重（无 refNo）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinBankStatementImport extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBankStatementBiz bankStatementBiz;

    @Test
    public void testImportHappyPath() {
        final Long[] accountId = new Long[1];
        ormTemplate.runInSession(() -> accountId[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK,
                new BigDecimal("1000")));

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-A-1",
                ErpFinConstants.DC_CREDIT, new BigDecimal("500"));
        BankStatementLineInput l2 = line(LocalDate.of(2026, 6, 12), "REF-A-2",
                ErpFinConstants.DC_DEBIT, new BigDecimal("300"));

        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(accountId[0],
                LocalDate.of(2026, 6, 30), Arrays.asList(l1, l2), CTX));

        assertNotNull(head.getId(), "对账单应已落库");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_DRAFT, head.getDocStatus(), "导入后 DRAFT");
        assertEquals(accountId[0], head.getFundAccountId());
        assertEquals(0, head.getTotalCredit().compareTo(new BigDecimal("500")), "贷方合计 500");
        assertEquals(0, head.getTotalDebit().compareTo(new BigDecimal("300")), "借方合计 300");

        List<ErpFinBankStatementLine> lines = loadLines(head.getId());
        assertEquals(2, lines.size(), "应生成 2 行");
        for (ErpFinBankStatementLine line : lines) {
            assertEquals(ErpFinConstants.BANK_MATCH_UNMATCHED, line.getMatchStatus(), "行初始化 UNMATCHED");
        }
    }

    @Test
    public void testDuplicateByRefNoRejected() {
        final Long[] accountId = new Long[1];
        ormTemplate.runInSession(() -> accountId[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK,
                BigDecimal.ZERO));

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-DUP-1",
                ErpFinConstants.DC_CREDIT, new BigDecimal("100"));

        ormTemplate.runInSession(() -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementLineInput dup = line(LocalDate.of(2026, 6, 10), "REF-DUP-1",
                ErpFinConstants.DC_CREDIT, new BigDecimal("100"));
        assertThrows(NopException.class, () ->
                        ormTemplate.runInSession(session -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                                Collections.singletonList(dup), CTX)),
                "同 refNo 重复导入应拒绝");
    }

    @Test
    public void testNonBankAccountRejected() {
        final Long[] accountId = new Long[1];
        ormTemplate.runInSession(() -> accountId[0] = seedFundAccount("CASH", BigDecimal.ZERO));

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-CASH-1",
                ErpFinConstants.DC_CREDIT, new BigDecimal("100"));

        assertThrows(NopException.class, () ->
                        ormTemplate.runInSession(session -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                                Collections.singletonList(l1), CTX)),
                "CASH 账户不可导入银行对账单");
    }

    @Test
    public void testStrictRefNoMissingRejected() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_BANK_IMPORT_STRICT_REFNO, "true");
        try {
            final Long[] accountId = new Long[1];
            ormTemplate.runInSession(() -> accountId[0] = seedFundAccount(
                    ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, BigDecimal.ZERO));

            BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), null,
                    ErpFinConstants.DC_CREDIT, new BigDecimal("100"));

            assertThrows(NopException.class, () ->
                            ormTemplate.runInSession(session -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                                    Collections.singletonList(l1), CTX)),
                    "strict-refno=true 缺 refNo 应拒绝");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_BANK_IMPORT_STRICT_REFNO, "false");
        }
    }

    @Test
    public void testCompositeKeyDedupWhenNoRefNo() {
        final Long[] accountId = new Long[1];
        ormTemplate.runInSession(() -> accountId[0] = seedFundAccount(
                ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, BigDecimal.ZERO));

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), null,
                ErpFinConstants.DC_CREDIT, new BigDecimal("200"));
        ormTemplate.runInSession(() -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementLineInput dup = line(LocalDate.of(2026, 6, 10), null,
                ErpFinConstants.DC_CREDIT, new BigDecimal("200"));
        assertThrows(NopException.class, () ->
                        ormTemplate.runInSession(session -> bankStatementBiz.importStatement(accountId[0], LocalDate.of(2026, 6, 30),
                                Collections.singletonList(dup), CTX)),
                "无 refNo 时同 (date, amount, dc) 组合应拒绝");
    }

    @Test
    public void testImportDistinctStatementsNoCrossAccountDuplicate() {
        final Long[] a1 = new Long[1];
        final Long[] a2 = new Long[1];
        ormTemplate.runInSession(() -> {
            a1[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, BigDecimal.ZERO);
            a2[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, BigDecimal.ZERO);
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-X-ACCT",
                ErpFinConstants.DC_CREDIT, new BigDecimal("100"));
        ErpFinBankStatement h1 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(a1[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        // 不同账户允许同 refNo
        BankStatementLineInput l2 = line(LocalDate.of(2026, 6, 10), "REF-X-ACCT",
                ErpFinConstants.DC_CREDIT, new BigDecimal("100"));
        ErpFinBankStatement h2 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(a2[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l2), CTX));

        assertNotNull(h1.getId());
        assertNotNull(h2.getId());
        assertNotEquals(h1.getId(), h2.getId());
    }

    // ---------- helpers ----------

    private List<ErpFinBankStatementLine> loadLines(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("statementId", statementId));
        return dao.findAllByQuery(q);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount) {
        BankStatementLineInput in = new BankStatementLineInput();
        in.setTransactionDate(txnDate);
        in.setRefNo(refNo);
        in.setDcDirection(dc);
        in.setAmount(amount);
        return in;
    }

    @SuppressWarnings("unused")
    private Long seedFundAccount(String accountType, BigDecimal currentBalance) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.newEntity();
        account.setCode("FA-" + System.nanoTime());
        account.setName("Account " + accountType);
        account.setOrgId(1L);
        account.setAccountType(accountType);
        account.setCurrencyId(1L);
        account.setOpeningBalance(currentBalance);
        account.setCurrentBalance(currentBalance);
        account.setStatus("ACTIVE");
        dao.saveEntity(account);
        return account.getId();
    }
}
