package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.biz.IErpFinBankStatementBiz;
import app.erp.fin.dao.dto.BankStatementLineInput;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.dao.entity.ErpFinBankStatement;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 银行对账自动红冲调度（plan 2026-08-07-1932-3 Phase 2，P1-RC-005）集成测试。
 *
 * <p>覆盖（Phase 2 测试矩阵 ①-⑤）：
 * <ul>
 *   <li>① 跨期 POSTED 调节表 → 跑 helper（reverseAll）→ docStatus=CANCELLED + BANK_RECON_ADJ 红冲凭证生成 + 原调整凭证 isReversed=true</li>
 *   <li>② 当月调节表（reconciliationDate ≥ 当月第一天）→ 不红冲（扫描排除）</li>
 *   <li>③ config {@code erp-fin.bank-recon-auto-reverse-next-month}=false → 跳过（机制开关）</li>
 *   <li>④ DRAFT/CANCELLED 调节表 → 扫描排除（reverse POSTED 守卫侧证）</li>
 *   <li>⑤ CLOSED 期间候选（seed 期间 status=CLOSED 的跨期调节表）→ 逐条失败隔离（WARN 日志 + recon 保持 POSTED + 批次不中断、其余候选继续红冲）——CLOSED 碰撞从静默变为显式可观测（Phase 1 Decision 1 回归）</li>
 * </ul>
 *
 * <p>断言强度对齐既有 {@code TestErpFinBankReconciliation} post+reverse 范式（红冲凭证经
 * {@code ErpFinVoucher.reversalOfVoucherId} 反查 + {@code isReversed} 标记）；CLOSED 失败隔离经
 * logback {@link ListAppender} 捕获 {@code ErpFinBankReconAutoReverseHelper} WARN 日志（对齐
 * {@code TestErpFinPostingObservability} 日志捕获范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinBankReconAutoReverseJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;
    static final String VOUCHER_STATUS_CANCELLED = ErpFinConstants.VOUCHER_STATUS_CANCELLED;
    static final String VOUCHER_STATUS_DRAFT = ErpFinConstants.VOUCHER_STATUS_DRAFT;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBankStatementBiz bankStatementBiz;
    @Inject
    IErpFinBankReconciliationBiz bankReconciliationBiz;
    @Inject
    ErpFinBankReconAutoReverseHelper autoReverseHelper;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger helperLogger;

    @BeforeEach
    void attachLogAppender() {
        helperLogger = (Logger) LoggerFactory.getLogger(ErpFinBankReconAutoReverseHelper.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        helperLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        if (helperLogger != null && logAppender != null) {
            helperLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    /** ① 跨期 POSTED 调节表自动红冲：docStatus→CANCELLED + 红冲凭证 + 原凭证 isReversed=true。 */
    @Test
    public void testCrossPeriodPostedReconAutoReversed() {
        long seed = System.nanoTime();
        String subjectId = "9101";
        final String[] ctx = new String[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject("91010", "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            seedPeriod("2026-06-OPEN", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        ErpFinBankReconciliation recon = seedPostedCrossPeriodRecon(ctx[0], "2026-06-30", "REF-AUTO-REV-" + seed);
        String adjVoucherId = findAdjVoucherId(recon.getCode());
        assertNotNull(adjVoucherId, "post 后应存在 BANK_RECON_ADJ 调整凭证");
        ErpFinVoucher adj = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(adjVoucherId);
        assertEquals(VOUCHER_STATUS_POSTED, adj.getDocStatus(), "调整凭证已过账");
        assertFalse(Boolean.TRUE.equals(adj.getIsReversed()), "红冲前原凭证未标记已红冲");

        int reversed = ormTemplate.runInSession(session -> autoReverseHelper.reverseAll(CTX));

        assertEquals(1, reversed, "跨期候选应全部红冲");
        ErpFinBankReconciliation after = reloadRecon(recon.getId());
        assertEquals(VOUCHER_STATUS_CANCELLED, after.getDocStatus(), "红冲后调节表 CANCELLED");
        assertTrue(countReversalVouchers(adjVoucherId) >= 1, "应生成红字调整凭证");
        ErpFinVoucher adjAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(adjVoucherId);
        assertTrue(Boolean.TRUE.equals(adjAfter.getIsReversed()), "红冲后原调整凭证 isReversed=true");
    }

    /** ② 当月调节表（reconciliationDate 当月）→ 扫描排除，不红冲。 */
    @Test
    public void testCurrentMonthReconNotReversed() {
        long seed = System.nanoTime();
        String subjectId = "9102";
        final String[] ctx = new String[1];
        LocalDate today = LocalDate.now();
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject("91020", "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            seedPeriod("CUR-MONTH-OPEN", today.getYear(), today.getMonthValue(),
                    today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        ErpFinBankReconciliation recon = seedPostedCrossPeriodRecon(ctx[0], today.toString(), "REF-CUR-MONTH-" + seed);

        int reversed = ormTemplate.runInSession(session -> autoReverseHelper.reverseAll(CTX));

        assertEquals(0, reversed, "当月调节表不应被扫描为跨期候选");
        assertEquals(VOUCHER_STATUS_POSTED, reloadRecon(recon.getId()).getDocStatus(), "当月调节表保持 POSTED");
    }

    /** ③ config=false → 跳过（机制开关）。 */
    @Test
    public void testConfigDisabledSkips() {
        long seed = System.nanoTime();
        String subjectId = "9103";
        final String[] ctx = new String[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject("91030", "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            seedPeriod("2026-06-OPEN-2", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        ErpFinBankReconciliation recon = seedPostedCrossPeriodRecon(ctx[0], "2026-06-30", "REF-CFG-OFF-" + seed);

        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH, "false");
        try {
            int reversed = ormTemplate.runInSession(session -> autoReverseHelper.reverseAll(CTX));
            assertEquals(0, reversed, "config=false 时跳过红冲");
            assertEquals(VOUCHER_STATUS_POSTED, reloadRecon(recon.getId()).getDocStatus(), "调节表保持 POSTED");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH, "true");
        }
    }

    /** ④ DRAFT/CANCELLED 调节表 → 扫描排除（reverse POSTED 守卫侧证）。 */
    @Test
    public void testNonPostedReconsExcludedFromScan() {
        long seed = System.nanoTime();
        String subjectId = "9104";
        final String[] ctx = new String[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject("91040", "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            seedPeriod("2026-06-OPEN-3", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        // DRAFT：仅 generate 不 post
        ErpFinBankStatement stmt = importStatement(ctx[0], "2026-06-30", "REF-DRAFT-" + seed, "1500");
        ErpFinBankReconciliation draft = ormTemplate.runInSession(session ->
                bankReconciliationBiz.generate(stmt.getId(), CTX));
        assertEquals(VOUCHER_STATUS_DRAFT, draft.getDocStatus());

        // CANCELLED：generate + post + 手动 reverse
        ErpFinBankReconciliation cancelled = seedPostedCrossPeriodRecon(ctx[0], "2026-06-30",
                "REF-CANCELLED-" + seed);
        ormTemplate.runInSession(() -> bankReconciliationBiz.reverse(cancelled.getId(), CTX));
        assertEquals(VOUCHER_STATUS_CANCELLED, reloadRecon(cancelled.getId()).getDocStatus());

        int reversed = ormTemplate.runInSession(session -> autoReverseHelper.reverseAll(CTX));

        assertEquals(0, reversed, "DRAFT/CANCELLED 非候选");
        assertEquals(VOUCHER_STATUS_DRAFT, reloadRecon(draft.getId()).getDocStatus(), "DRAFT 不被扫描");
        assertEquals(VOUCHER_STATUS_CANCELLED, reloadRecon(cancelled.getId()).getDocStatus(), "CANCELLED 不被扫描");
    }

    /** ⑤ CLOSED 期间候选 → 逐条失败隔离（WARN 日志 + 保持 POSTED + 批次不中断、其余候选继续红冲）。 */
    @Test
    public void testClosedPeriodCandidateFailureIsolation() {
        long seed = System.nanoTime();
        String subjectId = "9105";
        final String[] ctx = new String[1];
        final ErpFinAccountingPeriod[] mayPeriod = new ErpFinAccountingPeriod[1];
        final ErpFinAccountingPeriod[] junPeriod = new ErpFinAccountingPeriod[1];
        ormTemplate.runInSession(() -> {
            seedSubject(subjectId, "1002", "银行存款");
            seedSubject("91050", "2240OTHER", "未达账项调整");
            ctx[0] = seedFundAccount(subjectId, new BigDecimal("1000"));
            mayPeriod[0] = seedPeriod("2026-05-OPEN", 2026, 5,
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            junPeriod[0] = seedPeriod("2026-06-OPEN-4", 2026, 6,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
        });

        // 5 月候选：期间 OPEN，正常红冲
        ErpFinBankReconciliation mayRecon = seedPostedCrossPeriodRecon(ctx[0], "2026-05-31", "REF-MAY-" + seed);
        // 6 月候选：post 后把期间置 CLOSED → 红冲撞 ERR_PERIOD_CLOSED
        ErpFinBankReconciliation junRecon = seedPostedCrossPeriodRecon(ctx[0], "2026-06-30", "REF-JUN-" + seed);
        ormTemplate.runInSession(() -> {
            ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class)
                    .getEntityById(junPeriod[0].getId());
            p.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED);
            daoProvider.daoFor(ErpFinAccountingPeriod.class).updateEntity(p);
        });

        int reversed = ormTemplate.runInSession(session -> autoReverseHelper.reverseAll(CTX));

        assertEquals(1, reversed, "5 月候选成功红冲、6 月 CLOSED 候选失败隔离——批次不中断");
        assertEquals(VOUCHER_STATUS_CANCELLED, reloadRecon(mayRecon.getId()).getDocStatus(), "OPEN 期间候选红冲成功");
        assertEquals(VOUCHER_STATUS_POSTED, reloadRecon(junRecon.getId()).getDocStatus(),
                "CLOSED 期间候选保持 POSTED（下月重试）");

        // CLOSED 碰撞显式可观测：WARN 日志断言（Phase 2 Proof）
        ILoggingEvent warnLog = findWarnLog();
        assertNotNull(warnLog, "CLOSED 候选失败应记录 WARN 日志");
        assertTrue(warnLog.getFormattedMessage().contains("erp-fin-bank-recon-auto-reverse-failed"),
                "WARN 日志含失败标记");
        assertTrue(warnLog.getFormattedMessage().contains(String.valueOf(junRecon.getId())),
                "WARN 日志含失败候选 reconciliationId");
    }

    // ---------- helpers ----------

    /** seed 跨期（过去月份 statementDate）平衡且含未达的 POSTED 调节表（period 须已 OPEN seed）。 */
    private ErpFinBankReconciliation seedPostedCrossPeriodRecon(String fundAccountId, String statementDate,
                                                                String refNo) {
        ErpFinBankStatement head = importStatement(fundAccountId, statementDate, refNo, "1500");
        ErpFinBankReconciliation recon = ormTemplate.runInSession(session ->
                bankReconciliationBiz.generate(head.getId(), CTX));
        assertTrue(recon.getIsBalanced(), "应平衡");
        ormTemplate.runInSession(() -> bankReconciliationBiz.post(recon.getId(), CTX));
        assertEquals(VOUCHER_STATUS_POSTED, reloadRecon(recon.getId()).getDocStatus());
        assertTrue(countBillLinks(recon.getCode()) >= 1, "存在未达项时应生成 BANK_RECON_ADJ 调整凭证");
        return recon;
    }

    private ErpFinBankStatement importStatement(String fundAccountId, String statementDate, String refNo,
                                                String balanceAfter) {
        BankStatementLineInput l1 = line(LocalDate.parse(statementDate), refNo,
                DC_CREDIT, new BigDecimal("500"), new BigDecimal(balanceAfter));
        return ormTemplate.runInSession(session -> bankStatementBiz.importStatement(fundAccountId,
                LocalDate.parse(statementDate), Collections.singletonList(l1), CTX));
    }

    private int countBillLinks(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", "BANK_RECON_ADJ"));
        return dao.findAllByQuery(q).size();
    }

    private String findAdjVoucherId(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", "BANK_RECON_ADJ"));
        q.setLimit(1);
        ErpFinVoucherBillR link = dao.findFirstByQuery(q);
        return link != null ? link.getVoucherId() : null;
    }

    private long countReversalVouchers(String originalVoucherId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reversalOfVoucherId", originalVoucherId));
        return dao.findAllByQuery(q).size();
    }

    private ILoggingEvent findWarnLog() {
        for (ILoggingEvent event : logAppender.list) {
            if (event.getLevel().levelStr.equals("WARN")) {
                return event;
            }
        }
        return null;
    }

    private ErpFinBankReconciliation reloadRecon(String id) {
        return daoProvider.daoFor(ErpFinBankReconciliation.class).getEntityById(id);
    }

    private BankStatementLineInput line(LocalDate txnDate, String refNo, String dc, BigDecimal amount,
                                        BigDecimal balanceAfter) {
        BankStatementLineInput in = new BankStatementLineInput();
        in.setTransactionDate(txnDate);
        in.setRefNo(refNo);
        in.setDcDirection(dc);
        in.setAmount(amount);
        in.setBalanceAfter(balanceAfter);
        return in;
    }

    private String seedFundAccount(String subjectId, BigDecimal currentBalance) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount account = dao.newEntity();
        account.setCode("FA-" + System.nanoTime());
        account.setName("Bank");
        account.setOrgId("1");
        account.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        account.setSubjectId(subjectId);
        account.setCurrencyId("1");
        account.setOpeningBalance(currentBalance);
        account.setCurrentBalance(currentBalance);
        account.setStatus("ACTIVE");
        dao.saveEntity(account);
        return account.getId();
    }

    private void seedSubject(String id, String code, String name) {
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

    private ErpFinAccountingPeriod seedPeriod(String code, int year, int month,
                                               LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = dao.newEntity();
        p.setCode(code);
        p.setName(code);
        p.setOrgId("1");
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(status);
        dao.saveEntity(p);
        return p;
    }
}
