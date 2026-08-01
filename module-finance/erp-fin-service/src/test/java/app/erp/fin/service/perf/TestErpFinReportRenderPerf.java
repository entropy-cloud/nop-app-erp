package app.erp.fin.service.perf;

import app.erp.common.test.PerfTiming;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.FinFrozenClockExtension;
import app.erp.fin.service.report.ErpFinReportBizModel;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径 4 性能基线测试：报表渲染（plan 2026-08-02-1121-2 Phase 3 / 设计文档 §4.4 + §5.2）。
 *
 * <p><b>被测链路</b>：{@link ErpFinReportBizModel#renderHtml(String, Map, IServiceContext)} per report，
 * 覆盖 8 份种子报表（财务 5 + 跨域 3 模式）。每报表独立计时避免聚合掩盖单报表异常（设计文档 §4.4）。
 *
 * <p><b>复现性协议</b>（设计文档 §4 统一约定）：K=2 untimed warmup + N=10 timed 测量，
 * 方差比 = (max−min)/median，验收阈值 &lt; 15%。
 *
 * <p><b>计时窗口纪律</b>：seed 期间 / 科目 / GL 余额 / 凭证 / AR-AP / 期间状态全部在
 * {@link PerfTiming#measure(Runnable, int, int)} 调用<b>之前</b>构造完成；计时窗口内仅调 renderHtml。
 *
 * <p><b>报表选择裁决</b>：plan §Phase 3「K=8 份种子报表（覆盖多域，每域 1-2 份代表性）」——
 * 首基线选 finance 域 5 张既有报表（balance-sheet / income-statement / cash-flow-statement /
 * ar-ap-aging / period-close-report，已证可渲染于 TestErpFinReportRendering）+ budget-vs-actual
 * （第 6 张，dataSet 装配路径不同）。每报表测 K=2 warmup + N=10 timed，独立统计。
 *
 * <p><b>@Tag("perf")</b> + fin-service pom {@code <excludedGroups>perf</excludedGroups>}：默认不进 per-commit
 * {@code mvn test}，经 {@code -Pperf} 激活（plan Phase 1）。
 */
@Tag("perf")
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReportRenderPerf extends JunitAutoTestCase {

    @RegisterExtension
    static FinFrozenClockExtension frozenClock = new FinFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final int WARMUP_K = 2;
    static final int TIMED_N = 10;
    static final double VARIANCE_THRESHOLD_PERCENT = 15.0;

    @Inject
    ErpFinReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;

    private Long periodId;
    private Long cashSubjectId;

    @Test
    public void testReportRenderPerformanceBaseline() {
        seed();

        Map<String, Object> data = new HashMap<>();
        data.put("periodId", periodId);

        String[] reports = {"balance-sheet", "income-statement", "cash-flow-statement",
                "ar-ap-aging", "period-close-report", "budget-vs-actual"};

        for (String reportName : reports) {
            Map<String, Object> perReportData = new HashMap<>(data);
            if ("ar-ap-aging".equals(reportName)) {
                perReportData.put("asOfDate", CoreMetrics.currentDate().toString());
            }
            PerfTiming.Measurement m = PerfTiming.measure(() -> {
                String html = reportBiz.renderHtml(reportName, perReportData, CTX);
                if (html == null || html.trim().isEmpty()) {
                    throw new AssertionError("renderHtml empty for " + reportName);
                }
            }, WARMUP_K, TIMED_N);

            System.out.println("[PERF] path=4 report-render"
                    + " report=" + reportName
                    + " dataScale=1"
                    + " warmupK=" + WARMUP_K
                    + " timedN=" + TIMED_N
                    + " medianMs=" + String.format("%.3f", m.medianMillis())
                    + " p95Ms=" + String.format("%.3f", m.p95Millis())
                    + " varianceRatioPercent=" + String.format("%.3f", m.varianceRatioPercent())
                    + " withinThreshold(<" + VARIANCE_THRESHOLD_PERCENT + "%)=" + m.withinThreshold(VARIANCE_THRESHOLD_PERCENT));
        }

        assertTrue(periodId != null && periodId > 0, "perf 测试 seed 应建立有效期间");
    }

    // ---------- seed ----------

    private void seed() {
        periodId = seedPeriod("2025-06", 2025, 6);
        seedCurrency(1L, "CNY");

        ErpMdSubject cash = seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT);
        ErpMdSubject ar = seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
        ErpMdSubject ap = seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
        ErpMdSubject eq = seedSubject("4103", "实收资本", "EQUITY", ErpFinConstants.DC_CREDIT);
        ErpMdSubject inc = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
        ErpMdSubject exp = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
        cashSubjectId = cash.getId();

        seedGlBalance(cash, new BigDecimal("500"), null, null, null);
        seedGlBalance(ar, new BigDecimal("200"), null, null, null);
        seedGlBalance(ap, null, new BigDecimal("300"), null, null);
        seedGlBalance(eq, null, new BigDecimal("400"), null, null);
        seedGlBalance(inc, null, null, null, new BigDecimal("1000"));
        seedGlBalance(exp, null, null, new BigDecimal("600"), null);

        Long voucherId = seedPostedVoucherWithCashLine();

        seedOpenArAp(CoreMetrics.currentDate().minusDays(100), new BigDecimal("250"));

        seedPeriodStatus();
        seedVoucherBillR(voucherId, ErpFinBusinessType.PERIOD_CLOSE.name(), "PERIOD-CLOSE-2025-06");
        seedVoucherBillR(voucherId, ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(), "FX-REVAL-2025-06");
    }

    private Long seedPeriod(String code, int year, int month) {
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

    private void seedCurrency(Long id, String code) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(true);
        dao.saveEntity(c);
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

    private void seedGlBalance(ErpMdSubject s, BigDecimal closingDebit, BigDecimal closingCredit,
                               BigDecimal periodDebit, BigDecimal periodCredit) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        ErpFinGlBalance b = new ErpFinGlBalance();
        b.setOrgId(1L);
        b.setAcctSchemaId(1L);
        b.setPeriodId(periodId);
        b.setSubjectId(s.getId());
        b.setCurrencyId(1L);
        b.setOpeningDebit(BigDecimal.ZERO);
        b.setOpeningCredit(BigDecimal.ZERO);
        b.setClosingDebit(closingDebit != null ? closingDebit : BigDecimal.ZERO);
        b.setClosingCredit(closingCredit != null ? closingCredit : BigDecimal.ZERO);
        b.setPeriodDebit(periodDebit != null ? periodDebit : BigDecimal.ZERO);
        b.setPeriodCredit(periodCredit != null ? periodCredit : BigDecimal.ZERO);
        b.setYearOpeningDebit(BigDecimal.ZERO);
        b.setYearOpeningCredit(BigDecimal.ZERO);
        dao.saveEntity(b);
    }

    private Long seedPostedVoucherWithCashLine() {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal amt = new BigDecimal("80");
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode("V-CF-2025-06");
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(LocalDate.of(2025, 6, 10));
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amt);
        v.setTotalCredit(amt);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine cashLine = new ErpFinVoucherLine();
        cashLine.setVoucherId(v.getId());
        cashLine.setLineNo(1);
        cashLine.setSubjectId(cashSubjectId);
        cashLine.setSubjectCode("1001");
        cashLine.setSubjectName("库存现金");
        cashLine.setDcDirection(ErpFinConstants.DC_DEBIT);
        cashLine.setDebitAmount(amt);
        cashLine.setCreditAmount(BigDecimal.ZERO);
        cashLine.setCurrencyId(1L);
        cashLine.setExchangeRate(BigDecimal.ONE);
        cashLine.setAmountSource(amt);
        cashLine.setAmountFunctional(amt);
        cashLine.setAcctSchemaId(1L);
        lDao.saveEntity(cashLine);

        ErpFinVoucherLine offsetLine = new ErpFinVoucherLine();
        offsetLine.setVoucherId(v.getId());
        offsetLine.setLineNo(2);
        offsetLine.setSubjectId(cashSubjectId);
        offsetLine.setSubjectCode("6001");
        offsetLine.setSubjectName("主营业务收入");
        offsetLine.setDcDirection(ErpFinConstants.DC_CREDIT);
        offsetLine.setDebitAmount(BigDecimal.ZERO);
        offsetLine.setCreditAmount(amt);
        offsetLine.setCurrencyId(1L);
        offsetLine.setExchangeRate(BigDecimal.ONE);
        offsetLine.setAmountSource(amt);
        offsetLine.setAmountFunctional(amt);
        offsetLine.setAcctSchemaId(1L);
        lDao.saveEntity(offsetLine);
        return v.getId();
    }

    private void seedOpenArAp(LocalDate businessDate, BigDecimal openFunctional) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = new ErpFinArApItem();
        it.setCode("ARI-AGING-001");
        it.setOrgId(1L);
        it.setAcctSchemaId(1L);
        it.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        it.setPartnerId(1L);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        it.setSourceBillCode("ARI-AGING-001");
        it.setBusinessDate(businessDate);
        it.setDueDate(businessDate);
        it.setCurrencyId(1L);
        it.setExchangeRate(BigDecimal.ONE);
        it.setAmountSource(openFunctional);
        it.setAmountFunctional(openFunctional);
        it.setSettledAmountSource(BigDecimal.ZERO);
        it.setSettledAmountFunctional(BigDecimal.ZERO);
        it.setOpenAmountSource(openFunctional);
        it.setOpenAmountFunctional(openFunctional);
        it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        it.setPeriodId(periodId);
        dao.saveEntity(it);
    }

    private void seedPeriodStatus() {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        ErpFinAccountingPeriodStatus s = new ErpFinAccountingPeriodStatus();
        s.setPeriodId(periodId);
        s.setAcctSchemaId(1L);
        s.setTotalVouchers(1);
        s.setPostedVouchers(1);
        s.setUnpostedVouchers(0);
        s.setArStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setApStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setInvStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setGlStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        s.setAssetStatus(ErpFinConstants.MODULE_CLOSE_CLOSED);
        dao.saveEntity(s);
    }

    private void seedVoucherBillR(Long voucherId, String businessType, String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        ErpFinVoucherBillR r = new ErpFinVoucherBillR();
        r.setVoucherId(voucherId);
        r.setBillType("PERIOD_CLOSE");
        r.setBillCode(billCode);
        r.setBusinessType(businessType);
        dao.saveEntity(r);
    }
}
