package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinPostingExceptionBiz;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.ErpFinPostingMetricsSnapshot;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财过账运行监控四指标采集与呈现测试（计划 {@code 2026-07-04-1452-3} Phase 2）。
 *
 * <p>验证（{@code posting-log.md §裁决3}）：构造成功/失败/手工补录样本 → 四指标值正确 → 阈值门控判定正确
 * （达标 / 越限）。
 *
 * <ul>
 *   <li>自动化记账率 = 自动凭证数 ÷ (自动凭证数 + 手工补录异常数)</li>
 *   <li>凭证生成时延 P99 = 进程内窗口采样（成功 post 喂样）</li>
 *   <li>过账异常率 = 异常记录数 ÷ (异常记录数 + 成功凭证数)</li>
 *   <li>业财闭环成功率 = 代理值 1.0（SYNC 强一致假设，loopbackProxyMode=true）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPostingMetrics extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IErpFinPostingExceptionBiz postingExceptionBiz;

    @Test
    public void testFourMetricsWithSuccessAndFailureSamples() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            seedPeriod("2026-07-METRIC", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // 成功过账 2 笔（喂时延采样 + 计入凭证分母）
        ormTemplate.runInSession(() -> voucherBiz.post(apInvoiceEvent("AP-METRIC-OK-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
        ormTemplate.runInSession(() -> voucherBiz.post(apInvoiceEvent("AP-METRIC-OK-002", voucherDate,
                new BigDecimal("200"), new BigDecimal("26"), new BigDecimal("226")), CTX));

        ErpFinPostingMetricsSnapshot snapshot = ormTemplate.runInSession(session -> postingExceptionBiz.getRuntimeMetrics(null, CTX));
        assertNotNull(snapshot);
        assertTrue(snapshot.getVoucherCount() >= 2, "窗口内凭证数 ≥ 2");
        assertEquals(0, snapshot.getExceptionCount(), "无失败样本时异常数为 0");
        assertEquals(0, snapshot.getManualResolutionCount(), "无手工补录时为 0");
        assertTrue(snapshot.getLatencySampleCount() >= 2, "时延采样数 ≥ 2");

        // 自动化记账率 = 2/(2+0) = 1.0 ≥ 0.95 达标
        assertEquals(1.0, snapshot.getAutoPostingRate().getValue(), 0.001, "全成功：自动化记账率=1.0");
        assertTrue(snapshot.getAutoPostingRate().isHealthy(), "全成功：自动化记账率达标");
        assertEquals("higher_better", snapshot.getAutoPostingRate().getDirection());

        // 异常率 = 0/(0+2) = 0 < 0.01 达标
        assertEquals(0.0, snapshot.getExceptionRate().getValue(), 0.001, "无失败：异常率=0");
        assertTrue(snapshot.getExceptionRate().isHealthy(), "无失败：异常率达标");
        assertEquals("lower_better", snapshot.getExceptionRate().getDirection());

        // 时延 P99 < 30000ms 达标（测试内过账远小于 30s）
        assertTrue(snapshot.getLatencyP99Millis().getValue() < 30_000, "时延 P99 < 30s 阈值");
        assertTrue(snapshot.getLatencyP99Millis().isHealthy(), "时延达标");
        assertEquals("lower_better", snapshot.getLatencyP99Millis().getDirection());

        // 闭环成功率 = 代理值 1.0 ≥ 0.995 达标
        assertTrue(snapshot.isLoopbackProxyMode(), "闭环成功率为代理模式");
        assertEquals(1.0, snapshot.getLoopbackSuccessRate().getValue(), 0.001, "代理值=1.0");
        assertTrue(snapshot.getLoopbackSuccessRate().isHealthy(), "代理值达标");
    }

    @Test
    public void testExceptionRateAndAutoPostingRateDegradeOnFailure() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            // 同时开一个关闭期间用于构造失败
            seedPeriod("2026-08-METRIC-CLOSED", 2026, 8,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), ErpFinConstants.PERIOD_STATUS_CLOSED);
            seedPeriod("2026-07-METRIC", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // 1 笔成功（7 月期间）
        ormTemplate.runInSession(() -> voucherBiz.post(apInvoiceEvent("AP-METRIC-OK-010", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));

        // 3 笔失败（8 月期间关闭 → 抛 NopException → 异常工作台 PENDING）
        for (int i = 0; i < 3; i++) {
            final int fi = i;
            try {
                ormTemplate.runInSession(() -> voucherBiz.post(apInvoiceEvent("AP-METRIC-FAIL-" + fi,
                        LocalDate.of(2026, 8, 10),
                        new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
            } catch (NopException e) {
                // 预期失败
            }
        }

        // 1 笔失败 → 手工补录（resolution=MANUAL，计入自动化记账率分母）
        ErpFinPostingException toManual = findException("AP-METRIC-FAIL-0");
        assertNotNull(toManual);
        ormTemplate.runInSession(() -> postingExceptionBiz.manualEntry(toManual.getId(), 8888L, "财务手工补录", CTX));

        ErpFinPostingMetricsSnapshot snapshot = ormTemplate.runInSession(session -> postingExceptionBiz.getRuntimeMetrics(null, CTX));
        assertTrue(snapshot.getVoucherCount() >= 1, "凭证数 ≥ 1");
        assertTrue(snapshot.getExceptionCount() >= 3, "异常数 ≥ 3");
        assertTrue(snapshot.getManualResolutionCount() >= 1, "手工补录数 ≥ 1");

        // 异常率 = 3/(3+1) = 0.75 > 0.01 → 越限
        assertTrue(snapshot.getExceptionRate().getValue() > 0.01, "多失败：异常率越限");
        assertTrue(!snapshot.getExceptionRate().isHealthy(), "异常率越限 → healthy=false");

        // 自动化记账率 = 1/(1+1) = 0.5 < 0.95 → 越限
        assertTrue(snapshot.getAutoPostingRate().getValue() < 0.95, "有手工补录：自动化记账率越限");
        assertTrue(!snapshot.getAutoPostingRate().isHealthy(), "自动化记账率越限 → healthy=false");
    }

    // ---------- helpers ----------

    private ErpFinPostingException findException(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

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

    private ErpFinAccountingPeriod seedPeriod(String code, int year, int month, LocalDate start, LocalDate end,
                                              String status) {
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
        return period;
    }

    private void seedApInvoiceTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-AP-INVOICE-METRIC");
        tpl.setName("应付发票模板-指标测试");
        tpl.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
        tpl.setVoucherType(VOUCHER_TYPE_TRANSFER);
        tpl.setIsActive(true);
        dao.saveEntity(tpl);

        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(templateLine(tpl.getId(), 1, "6602", DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(templateLine(tpl.getId(), 2, "2221", DC_DEBIT, "TAX"));
        lineDao.saveEntity(templateLine(tpl.getId(), 3, "2202", DC_CREDIT, "TOTAL"));
    }

    private ErpFinVoucherTemplateLine templateLine(Long templateId, int lineNo, String subjectCode,
                                                    String dcDirection, String amountKey) {
        ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setSubjectCode(subjectCode);
        line.setDcDirection(dcDirection);
        line.setAmountKey(amountKey);
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
}
