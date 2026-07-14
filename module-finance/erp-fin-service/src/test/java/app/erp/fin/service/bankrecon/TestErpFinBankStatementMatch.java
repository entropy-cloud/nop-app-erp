package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.biz.IErpFinBankStatementLineBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 银行流水自动/手工勾对（{@link IErpFinBankStatementLineBiz}）集成测试（Phase 2）。
 *
 * <p>覆盖：唯一命中 MATCHED + matchedLineId 正确；多候选保持 UNMATCHED；金额一致凭据不唯一 SUSPENSE；
 * 方向相反校验（银行 CREDIT ↔ 账面 DEBIT）；手工勾对 MANUAL_MATCHED。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinBankStatementMatch extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBankStatementBiz bankStatementBiz;
    @Inject
    IErpFinBankStatementLineBiz bankStatementLineBiz;

    @Test
    public void testUniqueCandidateMatched() {
        long seed = System.nanoTime();
        long subjectId = 8001L;
        long[] ctx = new long[2]; // [accountId, accountSubjectId]
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            long accountId = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            ctx[0] = accountId;
            ctx[1] = subjectId;
            // 账面已有一笔到账（DEBIT 500），银行流水待勾对（CREDIT 500，方向反向）
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("500"), LocalDate.of(2026, 6, 10));
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-UNIQ-" + seed,
                DC_CREDIT, new BigDecimal("500"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(1, result.getMatched(), "唯一命中应 MATCHED");
        assertEquals(0, result.getUnmatched());
        assertEquals(0, result.getSuspense());

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_MATCHED, reloaded.getMatchStatus());
        assertNotNull(reloaded.getMatchedLineId(), "matchedLineId 应已回写");
    }

    @Test
    public void testMultipleCandidatesStayUnmatched() {
        long seed = System.nanoTime();
        long subjectId = 8002L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            long accountId = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            ctx[0] = accountId;
            // 账面有两笔同额同向分录（DEBIT 300），银行流水待勾对 CREDIT 300
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("300"), LocalDate.of(2026, 6, 11));
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("300"), LocalDate.of(2026, 6, 11));
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 11), "REF-MULTI-" + seed,
                DC_CREDIT, new BigDecimal("300"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(0, result.getMatched(), "多候选不应自动匹配");
        // 多候选金额一致 → SUSPENSE
        assertEquals(1, result.getSuspense());

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_SUSPENSE, reloaded.getMatchStatus(),
                "金额一致凭据不唯一 → SUSPENSE");
    }

    @Test
    public void testNoCandidateUnmatched() {
        long seed = System.nanoTime();
        long subjectId = 8003L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 12), "REF-NOPE-" + seed,
                DC_CREDIT, new BigDecimal("777"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(0, result.getMatched());
        assertEquals(1, result.getUnmatched(), "无候选 → UNMATCHED");
        assertEquals(0, result.getSuspense());

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_UNMATCHED, reloaded.getMatchStatus());
    }

    @Test
    public void testDirectionOppositeRequired() {
        long seed = System.nanoTime();
        long subjectId = 8004L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            // 账面分录方向与银行同向（CREDIT 500），不应匹配（银行也是 CREDIT 500）
            seedPostedVoucherLine(subjectId, DC_CREDIT, new BigDecimal("500"), LocalDate.of(2026, 6, 10));
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-DIR-" + seed,
                DC_CREDIT, new BigDecimal("500"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(0, result.getMatched(), "同向不应匹配");
        assertEquals(1, result.getUnmatched());
    }

    @Test
    public void testManualMatchMarksManualMatched() {
        long seed = System.nanoTime();
        long subjectId = 8005L;
        long[] ctx = new long[1];
        final Long[] voucherLineId = new Long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            ErpFinVoucherLine vl = seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("123"),
                    LocalDate.of(2026, 6, 13));
            voucherLineId[0] = vl.getId();
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 13), "REF-MAN-" + seed,
                DC_CREDIT, new BigDecimal("123"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));
        Long lineId = firstLine(head.getId()).getId();

        ErpFinBankStatementLine updated = ormTemplate.runInSession(session -> bankStatementLineBiz.manualMatch(lineId, voucherLineId[0], CTX));
        assertEquals(ErpFinConstants.BANK_MATCH_MANUAL_MATCHED, updated.getMatchStatus());
        assertEquals(voucherLineId[0], updated.getMatchedLineId());
    }

    @Test
    public void testManualMatchRejectsAlreadyMatched() {
        long seed = System.nanoTime();
        long subjectId = 8006L;
        long[] ctx = new long[1];
        final Long[] voucherLineId = new Long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            ErpFinVoucherLine vl = seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("999"),
                    LocalDate.of(2026, 6, 14));
            voucherLineId[0] = vl.getId();
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 14), "REF-MAN2-" + seed,
                DC_CREDIT, new BigDecimal("999"));
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));
        Long lineId = firstLine(head.getId()).getId();
        ormTemplate.runInSession(() -> bankStatementLineBiz.manualMatch(lineId, voucherLineId[0], CTX));

        assertThrows(NopException.class, () ->
                        ormTemplate.runInSession(session -> bankStatementLineBiz.manualMatch(lineId, voucherLineId[0], CTX)),
                "已勾对行不可重复勾对");
    }

    @Test
    public void testMatchedLineIdOccupiedExcludedFromLaterCandidates() {
        long seed = System.nanoTime();
        long subjectId = 8007L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("600"), LocalDate.of(2026, 6, 15));
        });

        // 先导入一笔并自动勾对（占唯一分录）
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 15), "REF-OCC-1-" + seed,
                DC_CREDIT, new BigDecimal("600"));
        ErpFinBankStatement h1 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));
        BankStatementMatchResult r1 = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(h1.getId(), CTX));
        assertEquals(1, r1.getMatched());

        // 再导入同额行，此时唯一候选已被占用 → 应无候选 → UNMATCHED
        BankStatementLineInput l2 = line(LocalDate.of(2026, 6, 15), "REF-OCC-2-" + seed,
                DC_CREDIT, new BigDecimal("600"));
        ErpFinBankStatement h2 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l2), CTX));
        BankStatementMatchResult r2 = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(h2.getId(), CTX));
        assertEquals(0, r2.getMatched(), "已占用分录不应再被勾对");
        assertEquals(1, r2.getUnmatched());
        assertNotEquals(h1.getId(), h2.getId());
    }

    // ---------- helpers ----------

    private ErpFinBankStatementLine firstLine(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statementId", statementId));
        q.setLimit(1);
        return dao.findFirstByQuery(q);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount) {
        BankStatementLineInput in = new BankStatementLineInput();
        in.setTransactionDate(txnDate);
        in.setRefNo(refNo);
        in.setDcDirection(dc);
        in.setAmount(amount);
        return in;
    }

    private long seedFundAccount(String accountType, long subjectId, BigDecimal currentBalance) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.newEntity();
        account.setCode("FA-" + System.nanoTime());
        account.setName("Bank " + accountType);
        account.setOrgId(1L);
        account.setAccountType(accountType);
        account.setSubjectId(subjectId);
        account.setCurrencyId(1L);
        account.setOpeningBalance(currentBalance);
        account.setCurrentBalance(currentBalance);
        account.setStatus("ACTIVE");
        dao.saveEntity(account);
        return account.getId();
    }

    private void seedSubject(long id, String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
        s.setDirection("DEBIT");
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private ErpFinVoucherLine seedPostedVoucherLine(long subjectId, String dc, BigDecimal amount, LocalDate voucherDate) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = vDao.newEntity();
        v.setCode("V-" + System.nanoTime());
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(voucherDate);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(1L);
        BigDecimal total = DC_DEBIT.equals(dc) ? amount : amount;
        v.setTotalDebit(DC_DEBIT.equals(dc) ? amount : total);
        v.setTotalCredit(DC_CREDIT.equals(dc) ? amount : total);
        v.setIsReversed(false);
        v.setDocStatus(VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine line = lDao.newEntity();
        line.setVoucherId(v.getId());
        line.setLineNo(1);
        line.setSubjectId(subjectId);
        line.setSubjectCode("1002");
        line.setSubjectName("银行存款");
        line.setDcDirection(dc);
        line.setDebitAmount(DC_DEBIT.equals(dc) ? amount : BigDecimal.ZERO);
        line.setCreditAmount(DC_CREDIT.equals(dc) ? amount : BigDecimal.ZERO);
        line.setCurrencyId(1L);
        line.setExchangeRate(BigDecimal.ONE);
        line.setAmountSource(amount);
        line.setAmountFunctional(amount);
        line.setAcctSchemaId(1L);
        line.setOrgId(1L);
        lDao.saveEntity(line);
        return line;
    }
}
