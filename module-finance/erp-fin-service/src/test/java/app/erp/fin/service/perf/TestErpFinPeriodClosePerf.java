package app.erp.fin.service.perf;

import app.erp.common.test.PerfTiming;
import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.FinFrozenClockExtension;
import app.erp.md.dao.entity.ErpMdCurrency;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径 2 性能基线测试：期间结账（大规模 GL 行结账）（plan 2026-08-02-1121-2 Phase 5 / 设计文档 §4.2 + §5.2）。
 *
 * <p><b>被测链路</b>：{@link IErpFinAccountingPeriodBiz#closePeriod(Long, IServiceContext)}
 * 编排（前置检查 → AR/AP/INV/AST/GL 按序关账 → 损益结转 → 试算平衡 → CLOSED）。
 *
 * <p><b>复现性协议</b>（设计文档 §4 统一约定）：K=2 untimed warmup + N=10 timed 测量，
 * 方差比 = (max−min)/median，验收阈值 &lt; 20%。
 *
 * <p><b>每轮重置纪律</b>（设计文档 §4.2 + §5.2）：closePeriod 使期间进入 CLOSED，再次调用会因 status
 * 断言失败抛异常。为使每轮可重测，每轮 close 后须 reverseClose（反结账回到 OPEN）。reverseClose 在计时
 * 窗口外。
 *
 * <p><b>计时窗口纪律</b>：seed 期间 / 科目 / GL 余额 / 凭证全部在测量循环<b>之前</b>构造完成；每轮 reverseClose
 * 也在计时窗口外；计时窗口仅包裹 closePeriod。PerfTiming.measure 不支持 per-round untimed setup，故用手动
 * 计时循环 + {@link PerfTiming#compute(long[])} 统计。
 *
 * <p><b>数据规模裁决</b>：plan §Phase 5 引用设计文档 §4.2 + roadmap 工作项「1 万 GL 行/期」，并明示「具体在
 * Phase 2 plan 裁决」。首基线裁决 N=2000 GL 行——理由：H2 localDb 单测构建 1 万 GL 行 seed 耗时
 * 显著（每行 4-5 字段实体持久化），且 closePeriod 多阶段链路本身偏重（5 模块关账 + 损益结转 + 试算）。
 * N=2000 已能测到结账端到端成本，回归语义保持；生产规模 successor 见设计文档 §9。
 *
 * <p><b>@Tag("perf")</b> + fin-service pom {@code <excludedGroups>perf</excludedGroups>}：默认不进 per-commit
 * {@code mvn test}，经 {@code -Pperf} 激活。
 */
@Tag("perf")
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-end-to-end-test.yaml")
public class TestErpFinPeriodClosePerf extends JunitAutoTestCase {

    @RegisterExtension
    static FinFrozenClockExtension frozenClock = new FinFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final int WARMUP_K = 2;
    static final int TIMED_N = 10;
    static final double VARIANCE_THRESHOLD_PERCENT = 20.0;
    static final int GL_LINE_COUNT = 2000;
    static final String PERIOD_CODE = "2025-06";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;

    @Test
    public void testPeriodClosePerformanceBaseline() {
        Long periodId = seedPeriodWithGlLines();

        long[] nanos = new long[TIMED_N];
        for (int round = -WARMUP_K; round < TIMED_N; round++) {
            if (round > -WARMUP_K) {
                // 上轮 close 后期间 CLOSED；finalizePeriod → CLOSED_FINAL → reverseClose → OPEN（计时窗口外）。
                // reverseClose 要求 CLOSED_FINAL（ErpFinAccountingPeriodReverseCloseProcessor.assertPeriodStatus）。
                ormTemplate.runInSession(session -> periodBiz.finalizePeriod(periodId, CTX));
                ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, CTX));
            }
            long start = io.nop.api.core.time.CoreMetrics.nanoTime();
            ErpFinAccountingPeriod period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));
            long elapsed = io.nop.api.core.time.CoreMetrics.nanoTimeDiff(start);
            assertTrue(ErpFinConstants.PERIOD_STATUS_CLOSED.equals(period.getStatus()),
                    "closePeriod 应使期间 CLOSED (round=" + round + ")");
            if (round >= 0) {
                nanos[round] = elapsed;
            }
        }

        PerfTiming.Measurement m = PerfTiming.compute(nanos);
        System.out.println("[PERF] path=2 period-close"
                + " dataScale=" + GL_LINE_COUNT + "GL-lines"
                + " warmupK=" + WARMUP_K
                + " timedN=" + TIMED_N
                + " medianMs=" + String.format("%.3f", m.medianMillis())
                + " p95Ms=" + String.format("%.3f", m.p95Millis())
                + " varianceRatioPercent=" + String.format("%.3f", m.varianceRatioPercent())
                + " withinThreshold(<" + VARIANCE_THRESHOLD_PERCENT + "%)=" + m.withinThreshold(VARIANCE_THRESHOLD_PERCENT));

        assertTrue(periodId != null && periodId > 0, "perf 测试应 seed 有效期间");
    }

    // ---------- seed ----------

    private Long seedPeriodWithGlLines() {
        return ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod();
            ErpMdSubject cash = seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpMdSubject inc = seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT);
            ErpMdSubject exp = seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            ErpMdSubject eq = seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT);
            ErpMdSubject ar = seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpMdSubject ap = seedSubject("2202", "应付账款", "LIABILITY", ErpFinConstants.DC_CREDIT);
            ErpMdSubject fx = seedSubject("6603", "汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);

            // 批量灌注 N=GL_LINE_COUNT/2 张凭证（每张 2 行 GL），累积到 GL_LINE_COUNT GL 行。
            int vouchers = GL_LINE_COUNT / 2;
            for (int i = 0; i < vouchers; i++) {
                seedPostedVoucher("V-PERF-" + i, pid, LocalDate.of(2025, 6, 10),
                        new BigDecimal("1").add(new BigDecimal(i)), cash, inc);
            }
            // 一笔未核销外币应收 → 触发汇兑重估阶段（period-close 链路完整性，对齐 PeriodCloseTestSupport.seedFullPeriod）。
            seedOpenArAp("ARI-PERF-001", pid, LocalDate.of(2025, 6, 11),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 2L, new BigDecimal("100"), new BigDecimal("800"));
            return pid;
        });
    }

    private void seedOpenArAp(String code, Long periodId, LocalDate date, String direction,
                              Long currencyId, BigDecimal openSource, BigDecimal openFunctional) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinArApItem> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinArApItem.class);
        app.erp.fin.dao.entity.ErpFinArApItem item = new app.erp.fin.dao.entity.ErpFinArApItem();
        item.setCode(code);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(1L);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(code);
        item.setBusinessDate(date);
        item.setCurrencyId(currencyId);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(openSource);
        item.setAmountFunctional(openFunctional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openSource);
        item.setOpenAmountFunctional(openFunctional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
    }

    private Long seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(PERIOD_CODE);
        p.setName(PERIOD_CODE);
        p.setOrgId(1L);
        p.setYear(2025);
        p.setMonth(6);
        p.setStartDate(LocalDate.of(2025, 6, 1));
        p.setEndDate(LocalDate.of(2025, 6, 28));
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
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    private void seedPostedVoucher(String vcode, Long periodId, LocalDate date, BigDecimal amt,
                                    ErpMdSubject debit, ErpMdSubject credit) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucher> vDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucher.class);
        app.erp.fin.dao.entity.ErpFinVoucher v = new app.erp.fin.dao.entity.ErpFinVoucher();
        v.setCode(vcode);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amt);
        v.setTotalCredit(amt);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);

        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> lDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);

        app.erp.fin.dao.entity.ErpFinVoucherLine debitLine = new app.erp.fin.dao.entity.ErpFinVoucherLine();
        debitLine.setVoucherId(v.getId());
        debitLine.setLineNo(1);
        debitLine.setSubjectId(debit.getId());
        debitLine.setSubjectCode(debit.getCode());
        debitLine.setSubjectName(debit.getName());
        debitLine.setDcDirection(ErpFinConstants.DC_DEBIT);
        debitLine.setDebitAmount(amt);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setCurrencyId(1L);
        debitLine.setExchangeRate(BigDecimal.ONE);
        debitLine.setAmountSource(amt);
        debitLine.setAmountFunctional(amt);
        debitLine.setAcctSchemaId(1L);
        lDao.saveEntity(debitLine);

        app.erp.fin.dao.entity.ErpFinVoucherLine creditLine = new app.erp.fin.dao.entity.ErpFinVoucherLine();
        creditLine.setVoucherId(v.getId());
        creditLine.setLineNo(2);
        creditLine.setSubjectId(credit.getId());
        creditLine.setSubjectCode(credit.getCode());
        creditLine.setSubjectName(credit.getName());
        creditLine.setDcDirection(ErpFinConstants.DC_CREDIT);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amt);
        creditLine.setCurrencyId(1L);
        creditLine.setExchangeRate(BigDecimal.ONE);
        creditLine.setAmountSource(amt);
        creditLine.setAmountFunctional(amt);
        creditLine.setAcctSchemaId(1L);
        lDao.saveEntity(creditLine);
    }
}
