package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinTrialBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import app.erp.fin.service.fx.ExchangeRevaluationService;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 试算平衡/GL 聚合 COMMITMENT 排除回归测试（plan 2026-08-15-1838-3 §Phase 3，finding P1-RC-091 / RC-R1.46）。
 *
 * <p>验证 5 GL 聚合路径（试算平衡快照/年度结转/损益结转/坏账准备/汇兑重估）过滤条件
 * {@code or(isNull, notIn(BUDGET, COMMITMENT))}（对齐控制引擎 {@code ErpFinBudgetControlBiz.applyPostingTypeFilter}
 * ACTUAL 通道）：承付凭证（{@code CommitmentVoucherGenerator} 单行单边影子凭证语义）不进入 5 GL 聚合，
 * 试算平衡 Dr==Cr 恒等式在承付开启 + PO approve 场景下恢复（budget.md 规则4/6/8 + A4.1.5 裁决）。
 *
 * <p>config 策略（T1 裁决 A+C）：类级 yaml 提供基座 config；承付开关按 per-test
 * {@code assignConfigValue} + try/finally 恢复（对齐 {@code TestErpPurInvoiceCommitmentRestore:164-172} 范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:trial-balance-commitment-test.yaml")
public class TestErpFinTrialBalanceCommitmentExclusion extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;
    @Inject
    BadDebtProvisionService provisionService;
    @Inject
    ExchangeRevaluationService exchangeRevaluationService;

    /** ① 承付启用 + 承付凭证存在 → 试算平衡快照 ΣclosingDebit==ΣclosingCredit（Dr==Cr 恒等式，修复前破坏）。 */
    @Test
    public void testTrialBalanceIdentityWithCommitmentExcluded() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2025-05", 2025, 5);
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subs.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subs.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedPostedVoucher("V-ACT-INC", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "200"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "200"));
            seedPostedVoucher("V-ACT-EXP", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "80"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "80"));
            seedCommitmentVoucher("V-CMT", pid, subs.get("6601"), new BigDecimal("500"));
            return pid;
        });

        withCommitmentEnabled(() -> ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX)));

        // Dr==Cr 恒等式：修复前承付单边 Dr 500 混入 → ΣclosingDebit − ΣclosingCredit = 500；修复后相等。
        BigDecimal[] totals = trialBalanceTotals(periodId);
        assertTrue(totals[0].compareTo(BigDecimal.ZERO) > 0, "试算平衡快照非空");
        assertEquals(0, totals[0].compareTo(totals[1]),
                "ΣclosingDebit==ΣclosingCredit（承付凭证被排除，恒等式恢复）");

        // 损益结转仅基于实际数：本年利润 = 收入 200 − 费用 80 = 120（修复前 −380，承付 500 混入费用聚合）。
        BigDecimal cypNet = netCredit(findSubjectByCode("4103").getId(), periodId);
        assertEquals(0, cypNet.compareTo(new BigDecimal("120")),
                "本年利润=实际收入200−实际费用80=120，承付 500 不得污染");
    }

    /** ②a 年度结转：承付凭证不参与本年利润→未分配利润结转（AnnualCloseService findPostedVoucherIds 过滤）。 */
    @Test
    public void testAnnualCloseExcludesCommitment() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2025-12", 2025, 12);
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subs.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subs.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            subs.put("4104", seedSubject("4104", "未分配利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedCurrency(1L, "CNY", true);
            seedPostedVoucher("V-DEC-INC", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "1000"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "1000"));
            seedPostedVoucher("V-DEC-EXP", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "400"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "400"));
            seedCommitmentVoucher("V-DEC-CMT", pid, subs.get("6601"), new BigDecimal("500"));
            return pid;
        });

        withCommitmentEnabled(() -> ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX)));

        // 未分配利润 = 实际净利润 1000−400=600（修复前承付混入费用 → 1000−900=100）。
        BigDecimal retainedNet = netCredit(findSubjectByCode("4104").getId(), periodId);
        assertEquals(0, retainedNet.compareTo(new BigDecimal("600")),
                "未分配利润=实际净利润 600，承付不得污染年度结转");
    }

    /** ②b 坏账准备：承付凭证不参与 Allowance 账面聚合（BadDebtProvisionService getAllowanceBalance 过滤）。 */
    @Test
    public void testBadDebtAllowanceExcludesCommitment() {
        seedReturn(() -> {
            Long pid = seedOpenPeriod("2025-06", 2025, 6);
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("1231", seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT));
            seedPostedVoucher("V-ALLOW", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "1000"),
                    line("1231", "坏账准备", ErpFinConstants.DC_CREDIT, "1000"));
            seedCommitmentVoucher("V-CMT-ALLOW", pid, subs.get("1231"), new BigDecimal("500"));
            return null;
        });

        BigDecimal balance = withCommitmentEnabled(() ->
                ormTemplate.runInSession(session -> provisionService.getAllowanceBalance()));
        // 修复前：账面 = 1000 − 500（承付单边 Dr 计入 credit−debit 聚合）= 500；修复后仅实际凭证 = 1000。
        assertEquals(0, balance.compareTo(new BigDecimal("1000")),
                "坏账准备账面=实际凭证 1000，承付 Dr 500 不得计入");
    }

    /** ②c 汇兑重估：承付凭证不参与银行存款账面本位币聚合（ExchangeRevaluationService aggregateBankSubjectBookFunctional 过滤）。 */
    @Test
    public void testExchangeRevaluationExcludesCommitment() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2025-08", 2025, 8);
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("1002", seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("6603", seedSubject("6603", "汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            seedPostedVoucher("V-BANK", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1002", "银行存款", ErpFinConstants.DC_DEBIT, "800"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "800"));
            seedCommitmentVoucher("V-CMT-BANK", pid, subs.get("1002"), new BigDecimal("500"));
            seedFundAccount("BANK-EUR", 2L, subs.get("1002").getId(), new BigDecimal("100"));
            return pid;
        });

        withCommitmentEnabled(() -> ormTemplate.runInSession(session ->
                exchangeRevaluationService.revalue(loadPeriod(periodId), CTX)));

        // 重估值 850（100 EUR × 8.5）vs 账面本位币：修复后仅实际凭证 800 → 差额 50 升值收益；
        // 修复前承付 Dr 500 计入 → 账面 1300 → 差额 −450（反向损失凭证）。
        ErpFinVoucher fx = findVoucherByBillCode("FX-REVAL-2025-08", ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name());
        assertNotNull(fx, "应生成汇兑重估凭证");
        assertEquals(0, fx.getTotalDebit().compareTo(new BigDecimal("50")),
                "重估凭证金额=50（承付 500 不得计入账面聚合）");
    }

    /** ③ 承付关闭（默认）：无承付凭证场景行为不变（notIn 语义 == ne 语义的结构性等价实测）。 */
    @Test
    public void testCommitmentDisabledBehaviorUnchanged() {
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2025-07", 2025, 7);
            Map<String, ErpMdSubject> subs = new HashMap<>();
            subs.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subs.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subs.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subs.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedPostedVoucher("V-INC", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("1001", "库存现金", ErpFinConstants.DC_DEBIT, "200"),
                    line("6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "200"));
            seedPostedVoucher("V-EXP", pid, ErpFinConstants.POSTING_TYPE_NORMAL, subs,
                    line("6601", "销售费用", ErpFinConstants.DC_DEBIT, "80"),
                    line("1001", "库存现金", ErpFinConstants.DC_CREDIT, "80"));
            return pid;
        });

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        BigDecimal[] totals = trialBalanceTotals(periodId);
        assertEquals(0, totals[0].compareTo(totals[1]), "无承付场景 ΣclosingDebit==ΣclosingCredit 不变");
        assertEquals(0, netCredit(findSubjectByCode("4103").getId(), periodId).compareTo(new BigDecimal("120")),
                "本年利润=120 与既有行为一致");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private <T> T withCommitmentEnabled(java.util.function.Supplier<T> action) {
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.TRUE);
        try {
            return action.get();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE);
        }
    }

    private void withCommitmentEnabled(Runnable action) {
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.TRUE);
        try {
            action.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE);
        }
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private ErpMdSubject seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private void seedCurrency(Long id, String code, boolean functional) {
        IEntityDao<app.erp.md.dao.entity.ErpMdCurrency> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class);
        app.erp.md.dao.entity.ErpMdCurrency c = new app.erp.md.dao.entity.ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    private void seedFundAccount(String code, Long currencyId, Long subjectId, BigDecimal currentBalance) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinFundAccount> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinFundAccount.class);
        app.erp.fin.dao.entity.ErpFinFundAccount acc = new app.erp.fin.dao.entity.ErpFinFundAccount();
        acc.setCode(code);
        acc.setName(code);
        acc.setOrgId(1L);
        acc.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        acc.setSubjectId(subjectId);
        acc.setCurrencyId(currencyId);
        acc.setCurrentBalance(currentBalance);
        acc.setOpeningBalance(BigDecimal.ZERO);
        acc.setStatus("ACTIVE");
        dao.saveEntity(acc);
    }

    private Object[] line(String subjectCode, String subjectName, String dc, String amount) {
        return new Object[]{subjectCode, subjectName, dc, new BigDecimal(amount)};
    }

    private void seedPostedVoucher(String code, Long periodId, String postingType,
                                   Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(postingType);
        v.setVoucherDate(CoreMetrics.today());
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(total);
        v.setTotalCredit(total);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        int lineNo = 1;
        for (Object[] l : lines) {
            ErpMdSubject subj = subjects.get((String) l[0]);
            String dc = (String) l[2];
            BigDecimal amt = (BigDecimal) l[3];
            ErpFinVoucherLine line = new ErpFinVoucherLine();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(subj.getId());
            line.setSubjectCode((String) l[0]);
            line.setSubjectName((String) l[1]);
            line.setDcDirection(dc);
            line.setDebitAmount(ErpFinConstants.DC_DEBIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCreditAmount(ErpFinConstants.DC_CREDIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCurrencyId(1L);
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(1L);
            lDao.saveEntity(line);
        }
    }

    /**
     * 承付凭证（postingType=COMMITMENT）：单行单边 Dr + totalDebit=amount/totalCredit=0 + docStatus=POSTED +
     * isReversed=false（镜像 {@code CommitmentVoucherGenerator.writeCommitmentVoucher} 影子凭证语义）。
     */
    private void seedCommitmentVoucher(String code, Long periodId, ErpMdSubject subject, BigDecimal amount) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setPostingType(ErpFinConstants.POSTING_TYPE_COMMITMENT);
        v.setVoucherDate(CoreMetrics.today());
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amount);
        v.setTotalCredit(BigDecimal.ZERO);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine line = new ErpFinVoucherLine();
        line.setVoucherId(v.getId());
        line.setLineNo(1);
        line.setSubjectId(subject.getId());
        line.setSubjectCode(subject.getCode());
        line.setSubjectName(subject.getName());
        line.setDcDirection(ErpFinConstants.DC_DEBIT);
        line.setDebitAmount(amount);
        line.setCreditAmount(BigDecimal.ZERO);
        line.setCurrencyId(1L);
        line.setExchangeRate(BigDecimal.ONE);
        line.setAmountSource(amount);
        line.setAmountFunctional(amount);
        line.setAcctSchemaId(1L);
        lDao.saveEntity(line);
    }

    private ErpMdSubject findSubjectByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinAccountingPeriod loadPeriod(Long periodId) {
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
    }

    private BigDecimal netCredit(Long subjectId, Long periodId) {
        return sum(subjectId, periodId)[1].subtract(sum(subjectId, periodId)[0]);
    }

    private BigDecimal[] sum(Long subjectId, Long periodId) {
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        List<Long> vids = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq).stream()
                .map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
        BigDecimal d = BigDecimal.ZERO, c = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : linesOf(subjectId, vids)) {
            d = d.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
            c = c.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
        }
        return new BigDecimal[]{d, c};
    }

    private List<ErpFinVoucherLine> linesOf(Long subjectId, List<Long> vids) {
        if (vids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subjectId", subjectId));
        return dao.findAllByQuery(q).stream()
                .filter(l -> vids.contains(l.getVoucherId()))
                .collect(java.util.stream.Collectors.toList());
    }

    private BigDecimal[] trialBalanceTotals(Long periodId) {
        IEntityDao<ErpFinTrialBalance> dao = daoProvider.daoFor(ErpFinTrialBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        BigDecimal d = BigDecimal.ZERO, c = BigDecimal.ZERO;
        for (ErpFinTrialBalance r : dao.findAllByQuery(q)) {
            d = d.add(r.getClosingDebit() != null ? r.getClosingDebit() : BigDecimal.ZERO);
            c = c.add(r.getClosingCredit() != null ? r.getClosingCredit() : BigDecimal.ZERO);
        }
        return new BigDecimal[]{d, c};
    }

    private ErpFinVoucher findVoucherByBillCode(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }
}
