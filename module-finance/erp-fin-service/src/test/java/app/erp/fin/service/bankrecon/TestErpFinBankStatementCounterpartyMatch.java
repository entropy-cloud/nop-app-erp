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
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 银行流水自动勾对对方账号维度过滤（P1-RC-004 / RC-R1.43）集成测试。
 *
 * <p>对方账号维度：对账单行 {@code counterpartyName} 与账面候选侧 {@code ErpMdPartner.name}
 * 非空精确匹配（C1 选项 A + C2 选项 A，见 plan 2026-08-15-1838-2 Phase 1 裁决），两侧任一为空放行。
 *
 * <p>覆盖：① 同额同日同科目不同对方账号 + 账面多候选 → 过滤后唯一候选 MATCHED（对照现状错误 MATCHED）；
 * ② 对方户名不匹配 → 候选排除（UNMATCHED 正确归位，错误 MATCHED 消除）；③ 对账单无 counterparty（null）
 * → 过滤空放行；⑤ 多 statement 场景下过滤与占用排除联动。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinBankStatementCounterpartyMatch extends JunitAutoTestCase {
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
    public void testCounterpartyNameNarrowsMultipleCandidatesToUnique() {
        long seed = System.nanoTime();
        long subjectId = 8101L;
        long[] ctx = new long[1];
        final Long[] partnerALineId = new Long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            long partnerA = seedPartner("P-A-" + seed, "甲公司");
            long partnerB = seedPartner("P-B-" + seed, "乙公司");
            // 账面两笔同额同向分录，对方账号（partner）不同
            ErpFinVoucherLine lineA = seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("500"),
                    LocalDate.of(2026, 6, 10), partnerA);
            partnerALineId[0] = lineA.getId();
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("500"),
                    LocalDate.of(2026, 6, 10), partnerB);
        });

        // 对账单行 counterpartyName=甲公司：过滤后唯一候选 → MATCHED
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 10), "REF-CP-A-" + seed,
                DC_CREDIT, new BigDecimal("500"), "甲公司");
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(1, result.getMatched(), "对方账号过滤后唯一候选应 MATCHED");
        assertEquals(0, result.getUnmatched());
        assertEquals(0, result.getSuspense());

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_MATCHED, reloaded.getMatchStatus());
        assertEquals(partnerALineId[0], reloaded.getMatchedLineId(), "应勾对 partner=甲公司 的分录");
    }

    @Test
    public void testCounterpartyNameMismatchExcludesSingleCandidate() {
        long seed = System.nanoTime();
        long subjectId = 8102L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            long partnerB = seedPartner("P-B2-" + seed, "乙公司");
            // 账面仅 1 候选（对方=乙公司）；现状（无过滤）会错误 MATCHED
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("300"),
                    LocalDate.of(2026, 6, 11), partnerB);
        });

        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 11), "REF-CP-MIS-" + seed,
                DC_CREDIT, new BigDecimal("300"), "甲公司");
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(0, result.getMatched(), "对方户名不符的候选应被排除，不再错误 MATCHED");
        assertEquals(1, result.getUnmatched());

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_UNMATCHED, reloaded.getMatchStatus());
    }

    @Test
    public void testCounterpartyNullNoFilterPassthrough() {
        long seed = System.nanoTime();
        long subjectId = 8103L;
        long[] ctx = new long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            long partnerA = seedPartner("P-A3-" + seed, "甲公司");
            long partnerB = seedPartner("P-B3-" + seed, "乙公司");
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("400"),
                    LocalDate.of(2026, 6, 12), partnerA);
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("400"),
                    LocalDate.of(2026, 6, 12), partnerB);
        });

        // 对账单行无 counterpartyName → 过滤空放行，既有语义（多候选 SUSPENSE）保持
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 12), "REF-NULL-" + seed,
                DC_CREDIT, new BigDecimal("400"), null);
        ErpFinBankStatement head = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));

        BankStatementMatchResult result = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(head.getId(), CTX));
        assertEquals(0, result.getMatched());
        assertEquals(1, result.getSuspense(), "无 counterparty 数据时保持多候选 SUSPENSE 语义");

        ErpFinBankStatementLine reloaded = firstLine(head.getId());
        assertEquals(ErpFinConstants.BANK_MATCH_SUSPENSE, reloaded.getMatchStatus());
    }

    @Test
    public void testCounterpartyFilterAcrossStatements() {
        long seed = System.nanoTime();
        long subjectId = 8104L;
        long[] ctx = new long[1];
        final Long[] partnerBLineId = new Long[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            ctx[0] = seedFundAccount(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK, subjectId,
                    new BigDecimal("10000"));
            long partnerA = seedPartner("P-A4-" + seed, "甲公司");
            long partnerB = seedPartner("P-B4-" + seed, "乙公司");
            seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("600"),
                    LocalDate.of(2026, 6, 13), partnerA);
            ErpFinVoucherLine lineB = seedPostedVoucherLine(subjectId, DC_DEBIT, new BigDecimal("600"),
                    LocalDate.of(2026, 6, 13), partnerB);
            partnerBLineId[0] = lineB.getId();
        });

        // statement 1：counterpartyName=甲公司 → 唯一候选（乙公司行被过滤）MATCHED 占用
        BankStatementLineInput l1 = line(LocalDate.of(2026, 6, 13), "REF-CP-S1-" + seed,
                DC_CREDIT, new BigDecimal("600"), "甲公司");
        ErpFinBankStatement h1 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l1), CTX));
        BankStatementMatchResult r1 = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(h1.getId(), CTX));
        assertEquals(1, r1.getMatched());

        // statement 2：同额 counterpartyName=乙公司 → 甲行已占用排除 + 乙行过滤后唯一 → MATCHED 乙
        BankStatementLineInput l2 = line(LocalDate.of(2026, 6, 13), "REF-CP-S2-" + seed,
                DC_CREDIT, new BigDecimal("600"), "乙公司");
        ErpFinBankStatement h2 = ormTemplate.runInSession(session -> bankStatementBiz.importStatement(ctx[0], LocalDate.of(2026, 6, 30),
                Collections.singletonList(l2), CTX));
        BankStatementMatchResult r2 = ormTemplate.runInSession(session -> bankStatementLineBiz.autoMatch(h2.getId(), CTX));
        assertEquals(1, r2.getMatched(), "过滤 + 占用排除联动：statement 2 应勾对乙公司分录");
        ErpFinBankStatementLine line2 = firstLine(h2.getId());
        assertEquals(partnerBLineId[0], line2.getMatchedLineId());
    }

    // ---------- helpers ----------

    private ErpFinBankStatementLine firstLine(Long statementId) {
        IEntityDao<ErpFinBankStatementLine> dao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statementId", statementId));
        q.setLimit(1);
        return dao.findFirstByQuery(q);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount,
                                        String counterpartyName) {
        BankStatementLineInput in = new BankStatementLineInput();
        in.setTransactionDate(txnDate);
        in.setRefNo(refNo);
        in.setDcDirection(dc);
        in.setAmount(amount);
        in.setCounterpartyName(counterpartyName);
        return in;
    }

    private long seedPartner(String code, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.setCode(code);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        dao.saveEntity(p);
        return p.getId();
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

    private ErpFinVoucherLine seedPostedVoucherLine(long subjectId, String dc, BigDecimal amount,
                                                     LocalDate voucherDate, Long partnerId) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = vDao.newEntity();
        v.setCode("V-" + System.nanoTime());
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(voucherDate);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(1L);
        v.setTotalDebit(DC_DEBIT.equals(dc) ? amount : BigDecimal.ZERO);
        v.setTotalCredit(DC_CREDIT.equals(dc) ? amount : BigDecimal.ZERO);
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
        line.setPartnerId(partnerId);
        lDao.saveEntity(line);
        return line;
    }
}
