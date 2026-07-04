package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财过账可观测性测试（计划 {@code 2026-07-04-1452-1} Phase 2）。
 *
 * <p>验证 traceId 端到端贯穿与结构化日志埋点：经 logback {@link ListAppender} 捕获
 * {@link ErpFinPostingProcessor} 的日志输出，断言四路径（成功 / 模板缺失 / 借贷不平衡 / 期间关闭）
 * 的结构化日志含 {@code traceId} 与对应 {@code ErrorCode}（失败）或 {@code voucherId}（成功）。
 *
 * <p>{@code traceId} 缺失时由引擎入口生成（{@link ErpFinPostingProcessor#ensureTraceId}），
 * 透传至 {@link PostingEvent}，使业务域审核 → 过账编排 → GL 写入可经同一 traceId 串联。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPostingObservability extends JunitAutoTestCase {
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

    private ListAppender<ILoggingEvent> logAppender;
    private Logger processorLogger;

    @BeforeEach
    void attachLogAppender() {
        processorLogger = (Logger) LoggerFactory.getLogger(ErpFinPostingProcessor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        processorLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        if (processorLogger != null && logAppender != null) {
            processorLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @Test
    void testSuccessPathLogsTraceIdAndVoucherId() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-OBS-SUCC-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        event.setTraceId(null);

        Long voucherId = voucherBiz.post(event, CTX);

        assertNotNull(voucherId, "happy path 应生成凭证");
        assertNotNull(event.getTraceId(), "traceId 缺失时应由引擎入口生成");
        assertTrue(event.getTraceId().length() > 0, "生成的 traceId 非空");

        ILoggingEvent successLog = findLogContaining("过账成功");
        assertNotNull(successLog, "成功路径应记录结构化日志");
        String rendered = successLog.getFormattedMessage();
        assertContains(rendered, "traceId=" + event.getTraceId(), "成功日志含 traceId");
        assertContains(rendered, "billHeadCode=AP-OBS-SUCC-001", "成功日志含 billHeadCode");
        assertContains(rendered, "voucherId=" + voucherId, "成功日志含 voucherId");
    }

    @Test
    void testTemplateMissingLogsTraceIdAndErrorCode() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            // 故意不 seed AP_INVOICE 模板 → 命中 ERR_TEMPLATE_NOT_FOUND
        });

        PostingEvent event = apInvoiceEvent("AP-OBS-NOPL-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        event.setTraceId("TRACE-FIXED-NOPL");

        NopException ex = assertThrows(NopException.class, () -> voucherBiz.post(event, CTX),
                "模板缺失应抛 NopException");
        assertEquals("erp.err.fin.posting.template-not-found", ex.getErrorCode());

        ILoggingEvent failLog = findLogContaining("过账失败");
        assertNotNull(failLog, "失败路径应记录结构化日志");
        String rendered = failLog.getFormattedMessage();
        assertContains(rendered, "traceId=TRACE-FIXED-NOPL", "失败日志含传入 traceId");
        assertContains(rendered, "errorCode=erp.err.fin.posting.template-not-found", "失败日志含 ErrorCode");
    }

    @Test
    void testUnbalancedLogsTraceIdAndErrorCode() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // TOTAL 与 AMOUNT+TAX 不等 → 借贷不平衡
        PostingEvent event = apInvoiceEvent("AP-OBS-UNBAL-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("200"));
        event.setTraceId("TRACE-FIXED-UNBAL");

        NopException ex = assertThrows(NopException.class, () -> voucherBiz.post(event, CTX),
                "借贷不平衡应抛 NopException");
        assertEquals("erp.err.fin.posting.unbalanced", ex.getErrorCode());

        ILoggingEvent failLog = findLogContaining("过账失败");
        assertNotNull(failLog, "失败路径应记录结构化日志");
        String rendered = failLog.getFormattedMessage();
        assertContains(rendered, "traceId=TRACE-FIXED-UNBAL", "失败日志含传入 traceId");
        assertContains(rendered, "errorCode=erp.err.fin.posting.unbalanced", "失败日志含 ErrorCode");
    }

    @Test
    void testPeriodClosedLogsTraceIdAndErrorCode() {
        LocalDate voucherDate = LocalDate.of(2026, 7, 15);
        seed(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    PERIOD_STATUS_CLOSED);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-OBS-CLSD-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        event.setTraceId("TRACE-FIXED-CLSD");

        NopException ex = assertThrows(NopException.class, () -> voucherBiz.post(event, CTX),
                "期间已结账应抛 NopException");
        assertEquals("erp.err.fin.posting.period-closed", ex.getErrorCode());

        ILoggingEvent failLog = findLogContaining("过账失败");
        assertNotNull(failLog, "失败路径应记录结构化日志");
        String rendered = failLog.getFormattedMessage();
        assertContains(rendered, "traceId=TRACE-FIXED-CLSD", "失败日志含传入 traceId");
        assertContains(rendered, "errorCode=erp.err.fin.posting.period-closed", "失败日志含 ErrorCode");
    }

    // ---------- helpers ----------

    private ILoggingEvent findLogContaining(String fragment) {
        List<ILoggingEvent> events = logAppender.list;
        for (ILoggingEvent e : events) {
            if (e.getFormattedMessage() != null && e.getFormattedMessage().contains(fragment)) {
                return e;
            }
        }
        return null;
    }

    private static void assertContains(String haystack, String needle, String msg) {
        assertTrue(haystack != null && haystack.contains(needle),
                msg + "（实际日志：" + haystack + "）");
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
}
