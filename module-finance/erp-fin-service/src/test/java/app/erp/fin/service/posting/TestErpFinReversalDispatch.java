package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 凭证红冲反写闭环集成测试（计划 {@code 2026-07-04-1452-2} Phase 2）。
 *
 * <p>验证 {@link IErpFinVoucherBiz#reverse} 成功后，{@link VoucherReversedEvent} 经
 * {@link ErpFinReversalListenerRegistry} 派发给监听者，事件字段（voucherId/reversalOfVoucherId/
 * billHeadCode/businessType/traceId）正确；监听者抛错时被隔离，红字凭证不回滚，失败落入
 * 5.1 异常工作台（{@link ErpFinPostingException}）。
 *
 * <p>测试经 {@link ErpFinReversalListenerRegistry#addListener} 编程式注册捕获/失败桩监听者
 * （不引入跨模块 test bean 文件，保持 finance-service 测试自包含）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinReversalDispatch extends JunitAutoTestCase {
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
    ErpFinReversalListenerRegistry reversalListenerRegistry;

    /** 捕获事件的桩监听者（编程式注册，避免引入跨模块 test bean 文件）。 */
    private static class CapturingListener implements IErpFinVoucherReversedListener {
        final AtomicInteger callCount = new AtomicInteger();
        VoucherReversedEvent lastEvent;

        @Override
        public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
            callCount.incrementAndGet();
            lastEvent = event;
        }
    }

    /** 总是抛 NopException 的桩监听者（验证失败隔离 + 红字凭证不回滚）。 */
    private static class FailingListener implements IErpFinVoucherReversedListener {
        @Override
        public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
            throw new NopException(ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED)
                    .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, event.getBillHeadCode());
        }
    }

    @Test
    public void testReverseDispatchesEventWithCorrectFields() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        Long originalId = ormTemplate.runInSession(session -> voucherBiz.post(apInvoiceEvent("AP-DISP-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
        assertNotNull(originalId, "前置：先 happy 过账生成原凭证");

        CapturingListener capturing = new CapturingListener();
        reversalListenerRegistry.addListener(capturing);

        Long redId = ormTemplate.runInSession(session -> voucherBiz.reverse("AP-DISP-001", ErpFinBusinessType.AP_INVOICE, CTX));

        assertNotNull(redId, "红冲应生成红字凭证");
        assertNotEquals(originalId, redId);
        output("1_red_id.json5", redId);
        assertEquals(1, capturing.callCount.get(), "监听者应被回调 1 次");
        VoucherReversedEvent event = capturing.lastEvent;
        assertNotNull(event, "监听者应收到事件");
        assertEquals(redId, event.getVoucherId(), "事件 voucherId = 红字凭证 ID");
        assertEquals(originalId, event.getReversalOfVoucherId(), "事件 reversalOfVoucherId = 原凭证 ID");
        assertEquals("AP-DISP-001", event.getBillHeadCode(), "事件 billHeadCode 正确");
        assertEquals(BUSINESS_TYPE_AP_INVOICE, event.getBusinessType(), "事件 businessType 正确");
        assertEquals(BUSINESS_TYPE_AP_INVOICE, event.getBillType(), "事件 billType 正确");
        assertNotNull(event.getTraceId(), "事件 traceId 由引擎生成非空");
        assertTrue(!event.getTraceId().isEmpty(), "traceId 非空串");
        java.util.Map<String, Object> eventState = new java.util.LinkedHashMap<>();
        eventState.put("voucherId", event.getVoucherId());
        eventState.put("reversalOfVoucherId", event.getReversalOfVoucherId());
        eventState.put("billHeadCode", event.getBillHeadCode());
        eventState.put("businessType", event.getBusinessType());
        eventState.put("billType", event.getBillType());
        eventState.put("callCount", capturing.callCount.get());
        output("2_event.json5", eventState);
    }

    @Test
    public void testReverseWithoutListenersDoesNotError() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        Long originalId = ormTemplate.runInSession(session -> voucherBiz.post(apInvoiceEvent("AP-NOOP-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
        assertNotNull(originalId);

        // 不注册任何监听者——reverse() 应正常完成不报错。
        Long redId = ormTemplate.runInSession(session -> voucherBiz.reverse("AP-NOOP-001", ErpFinBusinessType.AP_INVOICE, CTX));

        assertNotNull(redId, "无监听者时 reverse() 应正常返回红字凭证 ID");
        output("1_red_id.json5", redId);
    }

    @Test
    public void testFailingListenerIsolatedAndRedVoucherPreserved() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    PERIOD_STATUS_OPEN);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        Long originalId = ormTemplate.runInSession(session -> voucherBiz.post(apInvoiceEvent("AP-FAIL-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113")), CTX));
        assertNotNull(originalId);

        CapturingListener goodListener = new CapturingListener();
        FailingListener badListener = new FailingListener();
        reversalListenerRegistry.addListener(badListener);
        reversalListenerRegistry.addListener(goodListener);

        // reverse() 不抛错（监听者失败被隔离），红字凭证成功落库。
        Long redId = ormTemplate.runInSession(session -> voucherBiz.reverse("AP-FAIL-001", ErpFinBusinessType.AP_INVOICE, CTX));
        assertNotNull(redId, "监听者抛错时红字凭证仍应过账（法律效力）");
        output("1_red_id.json5", redId);
        assertEquals(1, goodListener.callCount.get(),
                "正常监听者应仍被回调（失败监听者不阻断其他监听者）");

        // 失败记录落入 5.1 异常工作台 PENDING 队列。
        assertEquals(1, countPostingExceptions("AP-FAIL-001"),
                "监听者失败应记 1 条 ErpFinPostingException PENDING 记录");
        ErpFinPostingException pe = findPostingException("AP-FAIL-001");
        assertNotNull(pe);
        assertEquals(ErpFinConstants.POSTING_TYPE_REVERSAL, pe.getPostingType(),
                "失败记录 postingType=REVERSAL");
        assertEquals(ErpFinConstants.FAILED_STAGE_NOTIFY_REVERSAL_LISTENER, pe.getFailedStage(),
                "失败记录 failedStage=notify-reversal-listener");
        assertEquals(ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED.getErrorCode(), pe.getErrorCode(),
                "失败记录 errorCode=ERR_REVERSAL_LISTENER_FAILED");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING, pe.getStatus(),
                "失败记录 status=PENDING");
        java.util.Map<String, Object> exceptionState = new java.util.LinkedHashMap<>();
        exceptionState.put("id", pe.getId());
        exceptionState.put("postingType", pe.getPostingType());
        exceptionState.put("failedStage", pe.getFailedStage());
        exceptionState.put("errorCode", pe.getErrorCode());
        exceptionState.put("status", pe.getStatus());
        exceptionState.put("goodListenerCallCount", goodListener.callCount.get());
        output("2_posting_exception.json5", exceptionState);
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

    private long countPostingExceptions(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        q.addFilter(eq("failedStage", ErpFinConstants.FAILED_STAGE_NOTIFY_REVERSAL_LISTENER));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinPostingException findPostingException(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        q.addFilter(eq("failedStage", ErpFinConstants.FAILED_STAGE_NOTIFY_REVERSAL_LISTENER));
        q.setLimit(1);
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
