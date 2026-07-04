package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.biz.IErpFinPostingExceptionBiz;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PeriodPreCheckReport;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 过账异常工作台 + 结账前置门控集成测试（计划 {@code 2026-07-04-1452-1} Phase 4）。
 *
 * <p>验证：失败先落 PENDING 异常记录（独立事务，不随主过账回滚丢失）→ 工作台可查可处置
 * （重试/忽略/补录 + 状态机 ErrorCode 守门）→ 期末结账前置检查扫描未处置异常阻止结账、处置完后放行。
 *
 * <p>覆盖 {@code posting-log.md §过账异常处置} + {@code §失败不静默丢弃}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPostingExceptionWorkbench extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;
    static final String PERIOD_STATUS_CLOSED = ErpFinConstants.PERIOD_STATUS_CLOSED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IErpFinPostingExceptionBiz postingExceptionBiz;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;

    @Test
    public void testFailedPostRecordsPendingExceptionAndPreCheckBlocks() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        final Long[] periodIdHolder = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod period = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), PERIOD_STATUS_CLOSED);
            periodIdHolder[0] = period.getId();
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });
        Long periodId = periodIdHolder[0];

        // 期间关闭 → post 失败 → 异常记录以独立事务写入 PENDING（不随主过账回滚丢失）
        PostingEvent event = apInvoiceEvent("AP-EXC-CLOSED-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX),
                "期间关闭应抛 NopException");

        ErpFinPostingException ex = findException("AP-EXC-CLOSED-001");
        assertNotNull(ex, "失败应在异常工作台留下 PENDING 记录（失败不静默）");
        assertEquals("erp.err.fin.posting.period-closed", ex.getErrorCode(), "异常记录含 ErrorCode");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING, ex.getStatus(), "初始状态 PENDING");
        assertEquals("resolveOpenPeriod", ex.getFailedStage(), "异常记录含失败阶段");
        assertNotNull(ex.getTraceId(), "异常记录含 traceId");
        assertNotNull(ex.getEventData(), "异常记录含原始事件数据（重试重建用）");

        // 期末结账前置检查扫描到未处置异常 → 阻止结账
        assertTrue(postingExceptionBiz.countUnresolved(CTX) >= 1, "存在未处置异常");
        PeriodPreCheckReport report = periodBiz.preCheck(periodId, CTX);
        assertTrue(report.getUnresolvedPostingExceptionKeys().stream()
                        .anyMatch(k -> "AP-EXC-CLOSED-001".equals(k)),
                "前置检查应列出未处置异常单据号");
        assertTrue(report.hasIssues(), "存在未处置异常应阻止结账");
    }

    @Test
    public void testRetrySucceedsAfterFixAndPreCheckPasses() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        final Long[] periodIdHolder = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod period = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), PERIOD_STATUS_CLOSED);
            periodIdHolder[0] = period.getId();
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });
        Long periodId = periodIdHolder[0];

        PostingEvent event = apInvoiceEvent("AP-EXC-RETRY-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX),
                "期间关闭应抛 NopException");
        ErpFinPostingException ex = findException("AP-EXC-RETRY-001");
        assertNotNull(ex, "前置：异常记录已落 PENDING");

        // 修复根因：回开期间为 OPEN，使重试过账可通过 resolveOpenPeriod 门控
        ormTemplate.runInSession(() -> {
            ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
            p.setStatus(PERIOD_STATUS_OPEN);
        });

        // 重试 → 重新过账成功 → 状态翻 RETRIED
        ErpFinPostingException resolved = postingExceptionBiz.retry(ex.getId(), CTX);
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED, resolved.getStatus(),
                "重试成功后状态翻 RETRIED");
        assertNotNull(resolved.getVoucherId(), "RETRIED 记录关联新生成的凭证");
        assertTrue(resolved.getRetryCount() >= 1, "重试次数递增");

        // 前置检查此时放行（无未处置异常）
        assertEquals(0, postingExceptionBiz.countUnresolved(CTX), "处置完后无未处置异常");
    }

    @Test
    public void testIgnoreRequiresReasonAndStateMachineGuards() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            seedPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    PERIOD_STATUS_CLOSED);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-EXC-IGNORE-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX));
        ErpFinPostingException ex = findException("AP-EXC-IGNORE-001");
        assertNotNull(ex);

        // 忽略须填写原因 → 缺原因抛 ErrorCode
        assertThrows(NopException.class, () -> postingExceptionBiz.ignore(ex.getId(), null, CTX),
                "忽略缺原因应抛 ErrorCode");

        // 正常忽略 → IGNORED
        ErpFinPostingException ignored = postingExceptionBiz.ignore(ex.getId(), "内部调拨不跨法人，无需记账", CTX);
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_IGNORED, ignored.getStatus());

        // 已处置（IGNORED）再处置 → 状态机守门抛 not-pending
        assertThrows(NopException.class, () -> postingExceptionBiz.ignore(ex.getId(), "再次", CTX),
                "非 PENDING 状态再处置应抛 ErrorCode");
    }

    @Test
    public void testManualEntryRequiresVoucherId() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            seedPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    PERIOD_STATUS_CLOSED);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-EXC-MANUAL-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX));
        ErpFinPostingException ex = findException("AP-EXC-MANUAL-001");
        assertNotNull(ex);

        // 手工补录须关联凭证 → 缺 voucherId 抛 ErrorCode
        assertThrows(NopException.class, () -> postingExceptionBiz.manualEntry(ex.getId(), null, "备注", CTX),
                "手工补录缺 voucherId 应抛 ErrorCode");

        // 关联凭证后 → MANUAL
        ErpFinPostingException manual = postingExceptionBiz.manualEntry(ex.getId(), 9999L, "财务手工补录", CTX);
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL, manual.getStatus());
        assertEquals(9999L, manual.getVoucherId());
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
        tpl.setCode("TPL-AP-INVOICE");
        tpl.setName("应付发票模板");
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
