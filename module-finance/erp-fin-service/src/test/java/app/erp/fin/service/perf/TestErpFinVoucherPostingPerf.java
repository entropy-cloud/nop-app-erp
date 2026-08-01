package app.erp.fin.service.perf;

import app.erp.common.test.PerfTiming;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
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
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径 1 性能基线测试：批量凭证过账吞吐（plan 2026-08-02-1121-2 Phase 2 / 设计文档 §4.1 + §5.2）。
 *
 * <p><b>被测链路</b>：{@link IErpFinVoucherBiz#post(PostingEvent, IServiceContext)} per voucher
 * （SYNC 默认，业务+库存+凭证同事务），批量 1000 张/轮。
 *
 * <p><b>复现性协议</b>（设计文档 §4 统一约定）：K=2 untimed warmup + N=10 timed 测量，
 * 方差比 = (max−min)/median，验收阈值 &lt; 15%。
 *
 * <p><b>计时窗口纪律</b>：seed 期间 + 科目 + 模板 + 12 批 × 1000 个 PostingEvent 全部在
 * {@link PerfTiming#measure(Runnable, int, int)} 调用<b>之前</b>构造完成；计时窗口内仅消费预构造好的
 * 事件并触发过账。每轮消费唯一一批（billCode 全局唯一，幂等不再命中）——否则 round 2+ 命中幂等空操作，
 * 计时退化为测幂等成本而非过账成本（设计文档 §4.1 隐含约束）。
 *
 * <p><b>单据类型裁决</b>：plan §Phase 2 「混合比例本计划裁决」——首基线统一用 {@code AP_INVOICE}
 * （已证 happy-path 模板齐全，TestErpFinPostingService 既有范式）。多类型混合（AR_INVOICE / PAYMENT）
 * 是 successor refinement（仅在需 per-Provider 路由成本细分时引入）。本基线测的是过账管线端到端成本
 * （模板查找→fact→凭证+回链落库），模板查找首轮后被缓存，businessType 主要影响模板路由缓存命中率，
 * 不影响管线基础成本。
 *
 * <p><b>@Tag("perf")</b> + fin-service {@code pom.xml} {@code <excludedGroups>perf</excludedGroups>}：
 * 默认不进 per-commit {@code mvn test}，经 {@code -Dgroups=perf} 激活（plan Phase 1）。
 */
@Tag("perf")
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherPostingPerf extends JunitAutoTestCase {

    @RegisterExtension
    static FinFrozenClockExtension frozenClock = new FinFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;

    static final int WARMUP_K = 2;
    static final int TIMED_N = 10;
    static final int VOUCHERS_PER_ROUND = 1000;
    static final double VARIANCE_THRESHOLD_PERCENT = 15.0;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testVoucherPostingPerformanceBaseline() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedCurrency(1L, "CNY");
            seedApInvoiceTemplate();
        });

        // 预构造 (K + N) × 1000 个唯一 PostingEvent（计时窗口外）。每轮消费一批。
        final int totalRounds = WARMUP_K + TIMED_N;
        final List<List<PostingEvent>> batches = new ArrayList<>(totalRounds);
        for (int round = 0; round < totalRounds; round++) {
            List<PostingEvent> batch = new ArrayList<>(VOUCHERS_PER_ROUND);
            for (int i = 0; i < VOUCHERS_PER_ROUND; i++) {
                String billCode = "AP-PERF-R" + round + "-" + i;
                batch.add(apInvoiceEvent(billCode, voucherDate,
                        new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")));
            }
            batches.add(batch);
        }

        // 计时窗口：K warmup + N timed，每轮消费唯一一批。
        final int[] roundHolder = {0};
        PerfTiming.Measurement m = PerfTiming.measure(() -> {
            List<PostingEvent> batch = batches.get(roundHolder[0]++);
            for (PostingEvent event : batch) {
                ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
            }
        }, WARMUP_K, TIMED_N);

        long totalPosted = countPostedVouchers();
        assertTrue(totalPosted >= VOUCHERS_PER_ROUND,
                "perf 测试应至少产出 " + VOUCHERS_PER_ROUND + " 张凭证，实际 " + totalPosted);

        System.out.println("[PERF] path=1 voucher-posting"
                + " dataScale=" + VOUCHERS_PER_ROUND
                + " warmupK=" + WARMUP_K
                + " timedN=" + TIMED_N
                + " medianMs=" + String.format("%.3f", m.medianMillis())
                + " p95Ms=" + String.format("%.3f", m.p95Millis())
                + " varianceRatioPercent=" + String.format("%.3f", m.varianceRatioPercent())
                + " withinThreshold(<" + VARIANCE_THRESHOLD_PERCENT + "%)=" + m.withinThreshold(VARIANCE_THRESHOLD_PERCENT));
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private PostingEvent apInvoiceEvent(String billHeadCode, LocalDate voucherDate, BigDecimal amount,
                                        BigDecimal tax, BigDecimal total) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(1L);
        event.setOrgId(1L);
        event.setCurrencyId(1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("AMOUNT", amount);
        event.getBillData().put("TAX", tax);
        event.getBillData().put("TOTAL", total);
        event.getBillData().put("partnerId", 1L);
        event.getBillData().put("businessDate", voucherDate);
        return event;
    }

    private void seedApInvoiceTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-AP-INVOICE-PERF");
        tpl.setName("应付发票模板(perf)");
        tpl.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
        tpl.setVoucherType(VOUCHER_TYPE_TRANSFER);
        tpl.setIsActive(true);
        dao.saveEntity(tpl);

        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(templateLine(tpl.getId(), 1, "6602", DC_DEBIT, "AMOUNT", "EXPENSE"));
        lineDao.saveEntity(templateLine(tpl.getId(), 2, "2221", DC_DEBIT, "TAX", "INPUT_TAX"));
        lineDao.saveEntity(templateLine(tpl.getId(), 3, "2202", DC_CREDIT, "TOTAL", "AP"));
    }

    private ErpFinVoucherTemplateLine templateLine(Long templateId, int lineNo, String subjectCode,
                                                   String dcDirection, String amountKey, String accountKey) {
        ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setSubjectCode(subjectCode);
        line.setDcDirection(dcDirection);
        line.setAmountKey(amountKey);
        line.setAccountKey(accountKey);
        return line;
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedCurrency(Long id, String code) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(id);
        currency.setCode(code);
        currency.setName(code);
        currency.setIsFunctional(true);
        dao.saveEntity(currency);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private long countPostedVouchers() {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        return dao.findAllByQuery(new QueryBean()).size();
    }
}
