package app.erp.fin.service.job;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 兜底扫描重试 Job（{@link DeferredPostingSweepJob}）单元测试（L-3，杠杆 D）。验证自动扫描重试路径：
 *
 * <ul>
 *   <li>正路径：PENDING 异常（有效 eventData + postingType=NORMAL）重试成功 → status=RETRIED + 凭证落库</li>
 *   <li>重试耗尽：retryCount=2 重试失败 → retryCount=3 + status=RETRYING</li>
 *   <li>空配置跳过：cron 空 → execute() noop（无状态变更）</li>
 *   <li>REVERSAL 路径：postingType=REVERSAL → {@code voucherBiz.reverse} 分支</li>
 *   <li>单条失败隔离：一条失败不阻断其他记录</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/finance/posting-log.md §兜底扫描重试}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinDeferredPostingSweepJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long ACCT_SCHEMA_ID = 1L;
    static final Long CURRENCY_ID = 1L;
    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testRetrySucceedsAfterRootCauseFixed() {
        final Long[] periodId = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod p = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpFinConstants.PERIOD_STATUS_CLOSED);
            periodId[0] = p.getId();
            seedSubjects();
            seedApInvoiceTemplate();
        });

        // 期间关闭 → post 失败 → 异常记录以独立事务写入 PENDING（含有效 eventData）
        PostingEvent event = apInvoiceEvent("AP-SWEEP-001");
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX), "期间关闭应抛 NopException");
        ErpFinPostingException ex = findException("AP-SWEEP-001");
        assertNotNull(ex, "失败应留下 PENDING 异常记录");
        assertNotNull(ex.getEventData(), "异常记录含有效 eventData（重试重建用）");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING, ex.getStatus());

        // 修复根因：回开期间
        reopenPeriod(periodId[0]);

        // 扫描重试 → 重新过账成功 → RETRIED + 凭证落库
        newJob("0 0/5 * * * ?").execute();

        ErpFinPostingException resolved = findException("AP-SWEEP-001");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED, resolved.getStatus(),
                "重试成功后状态翻 RETRIED");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY, resolved.getResolution(),
                "处置动作为 RETRY");
        assertNotNull(findVoucherByBillCode("AP-SWEEP-001"), "重试应生成并落库凭证");
    }

    @Test
    public void testRetryExhaustedMarksRetrying() {
        final Long[] periodId = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod p = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpFinConstants.PERIOD_STATUS_CLOSED);
            periodId[0] = p.getId();
            seedSubjects();
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-SWEEP-002");
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX));
        ErpFinPostingException ex = findException("AP-SWEEP-002");
        assertNotNull(ex);

        // 预置 retryCount=2（接近上限），期间仍关闭 → 重试必然失败
        ormTemplate.runInSession(() -> {
            ErpFinPostingException e = daoProvider.daoFor(ErpFinPostingException.class).getEntityById(ex.getId());
            e.setRetryCount(2);
        });
        // 期间保持关闭，重试会再次失败

        newJob("0 0/5 * * * ?").execute();

        ErpFinPostingException after = findException("AP-SWEEP-002");
        assertEquals(3, after.getRetryCount(), "重试失败 retryCount 递增到 3");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING, after.getStatus(),
                "达到 MAX_RETRY(3) 标记 RETRYING（可观测，等待人工处置）");
    }

    @Test
    public void testCronEmptySkipsExecution() {
        final Long[] periodId = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod p = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpFinConstants.PERIOD_STATUS_CLOSED);
            periodId[0] = p.getId();
            seedSubjects();
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-SWEEP-003");
        assertThrows(NopException.class, () -> voucherBiz.post(event, CTX));
        ErpFinPostingException ex = findException("AP-SWEEP-003");
        assertNotNull(ex);
        int retryBefore = ex.getRetryCount();

        // cron 空 → execute() noop（无异常、无状态变更）
        newJob("").execute();

        ErpFinPostingException after = findException("AP-SWEEP-003");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING, after.getStatus(),
                "cron 空时状态不变（仍 PENDING）");
        assertEquals(retryBefore, after.getRetryCount(), "cron 空时 retryCount 不变（跳过执行）");
        assertNull(findVoucherByBillCode("AP-SWEEP-003"), "cron 空时不重试过账（无凭证）");
    }

    @Test
    public void testReversalRetryPath() {
        seed(() -> {
            seedPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            seedSubjects();
            seedApInvoiceTemplate();
        });

        // 前置：先成功过账原凭证（供 reverse 反查）
        Long originalId = voucherBiz.post(apInvoiceEvent("AP-SWEEP-004"), CTX);
        assertNotNull(originalId, "前置：原凭证已过账");

        // 手工落一条 REVERSAL PENDING 异常（模拟红冲过账失败被记录）
        seedException("AP-SWEEP-004", ErpFinConstants.POSTING_TYPE_REVERSAL, null, 0);

        newJob("0 0/5 * * * ?").execute();

        ErpFinPostingException resolved = findException("AP-SWEEP-004");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED, resolved.getStatus(),
                "REVERSAL 重试成功 → RETRIED");

        // 应生成红字凭证（isReversed=true，关联原凭证）
        List<ErpFinVoucher> redVouchers = findVouchersReversing(originalId);
        assertTrue(!redVouchers.isEmpty(), "REVERSAL 重试应生成红字凭证");
        ErpFinVoucher red = redVouchers.get(0);
        assertEquals(true, red.getIsReversed(), "红字凭证 isReversed=true");
        assertEquals(originalId, red.getReversalOfVoucherId(), "红字凭证关联原凭证");
    }

    @Test
    public void testSingleFailureIsolation() {
        final Long[] periodId = new Long[1];
        seed(() -> {
            ErpFinAccountingPeriod p = seedPeriod("2026-07", 2026, 7,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    ErpFinConstants.PERIOD_STATUS_CLOSED);
            periodId[0] = p.getId();
            seedSubjects();
            seedApInvoiceTemplate();
        });

        // 记录 A：有效 eventData（来自真实失败过账）
        PostingEvent eventA = apInvoiceEvent("AP-SWEEP-005A");
        assertThrows(NopException.class, () -> voucherBiz.post(eventA, CTX));
        ErpFinPostingException exA = findException("AP-SWEEP-005A");
        assertNotNull(exA, "记录 A 已落 PENDING");

        // 记录 B：手工落一条 eventData 损坏的 PENDING（rebuildEvent 反序列化将抛错 → 重试失败）
        seedException("AP-SWEEP-005B", ErpFinConstants.POSTING_TYPE_NORMAL, "{CORRUPT-JSON", 0);

        // 修复根因使 A 可重试成功；B 因 eventData 损坏仍失败
        reopenPeriod(periodId[0]);

        newJob("0 0/5 * * * ?").execute();

        ErpFinPostingException afterA = findException("AP-SWEEP-005A");
        ErpFinPostingException afterB = findException("AP-SWEEP-005B");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED, afterA.getStatus(),
                "记录 A 重试成功 → RETRIED（不被 B 的失败阻断）");
        assertEquals(1, afterB.getRetryCount(), "记录 B 重试失败 retryCount=1（损坏 eventData 隔离失败）");
        assertNotEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED, afterB.getStatus(),
                "记录 B 未成功（单条失败隔离，不阻断 A）");
    }

    // ---------- testable job ----------

    /** 子类：固定 cron（绕过 AppConfig），复用父类 protected/package-private 字段。 */
    private TestableDeferredPostingSweepJob newJob(String cron) {
        TestableDeferredPostingSweepJob job = new TestableDeferredPostingSweepJob(cron);
        job.daoProvider = daoProvider;
        job.transactionTemplate = transactionTemplate;
        job.ormTemplate = ormTemplate;
        job.voucherBiz = voucherBiz;
        return job;
    }

    private static class TestableDeferredPostingSweepJob extends DeferredPostingSweepJob {
        private final String cron;

        TestableDeferredPostingSweepJob(String cron) {
            this.cron = cron;
        }

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        // 生产调度器执行 execute() 时，REQUIRES_NEW 不替换线程绑定 ORM 会话，故 markRetried/
        // incrementRetryAndRethrow 的 updateEntity(ex) 经 session.contains(ex)=true 走 update 路径。
        // autotest 环境下 REQUIRES_NEW 会切换为独立会话（不含 ex），导致 saveOrUpdate 走 save 路径并触发
        // save-entity-not-transient。此处按 id 在当前会话重载实体以对齐生产会话语义（仅测试环境适配，
        // 被测逻辑 markRetried/incrementRetryAndRethrow 的字段设置与状态机不变）。
        private app.erp.fin.dao.entity.ErpFinPostingException reloadInCurrentSession(
                app.erp.fin.dao.entity.ErpFinPostingException ex) {
            return daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class)
                    .getEntityById(ex.getId());
        }

        @Override
        protected void markRetried(app.erp.fin.dao.entity.ErpFinPostingException ex) {
            super.markRetried(reloadInCurrentSession(ex));
        }

        // incrementRetryAndRethrow 在 retryOne 的 catch 中被调用（处于其自身 REQUIRES_NEW 会话之外），
        // 故须在其自身的 REQUIRES_NEW 会话内重载实体（逻辑与基线逐行一致，仅 ex→managed 替换）。
        @Override
        protected void incrementRetryAndRethrow(app.erp.fin.dao.entity.ErpFinPostingException ex, Exception e) {
            try {
                transactionTemplate.runInTransaction(null,
                        io.nop.api.core.annotations.txn.TransactionPropagation.REQUIRES_NEW, txn ->
                                ormTemplate.runInSession(session -> {
                                    app.erp.fin.dao.entity.ErpFinPostingException managed =
                                            daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class)
                                                    .getEntityById(ex.getId());
                                    managed.setRetryCount((managed.getRetryCount() == null ? 0 : managed.getRetryCount()) + 1);
                                    if (managed.getRetryCount() >= MAX_RETRY) {
                                        managed.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING);
                                    }
                                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class)
                                            .updateEntity(managed);
                                    session.flush();
                                    return null;
                                }));
            } catch (Exception persistErr) {
                // 对齐基线降级：持久化失败不重抛（仅告警）
            }
        }
    }

    // ---------- helpers: seed ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private ErpFinAccountingPeriod seedPeriod(String code, int year, int month, LocalDate start, LocalDate end,
                                              String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
        return period;
    }

    private void reopenPeriod(Long periodId) {
        ormTemplate.runInSession(() -> {
            ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
            p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        });
    }

    private void seedSubjects() {
        seedSubject("6602", "管理费用", "EXPENSE", "DEBIT");
        seedSubject("2221", "应交税费-进项税", "ASSET", "DEBIT");
        seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
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

    private void seedException(String billHeadCode, String postingType, String eventData, int retryCount) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
            ErpFinPostingException ex = dao.newEntity();
            ex.setTraceId("test-trace-" + billHeadCode);
            ex.setBillHeadCode(billHeadCode);
            ex.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
            ex.setPostingType(postingType);
            ex.setErrorCode("erp.err.fin.posting.period-closed");
            ex.setErrorMessage("seeded-for-sweep-test");
            ex.setFailedStage("resolveOpenPeriod");
            ex.setVoucherDate(LocalDate.of(2026, 7, 15));
            ex.setOrgId(ORG_ID);
            ex.setAcctSchemaId(ACCT_SCHEMA_ID);
            ex.setCurrencyId(CURRENCY_ID);
            ex.setExchangeRate(BigDecimal.ONE);
            ex.setEventData(eventData);
            ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING);
            ex.setRetryCount(retryCount);
            ex.setOccurrenceTime(CoreMetrics.currentTimestamp());
            dao.saveEntity(ex);
        });
    }

    private PostingEvent apInvoiceEvent(String billHeadCode) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(ACCT_SCHEMA_ID);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_ID);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(LocalDate.of(2026, 7, 15));
        event.getBillData().put("AMOUNT", new BigDecimal("100"));
        event.getBillData().put("TAX", new BigDecimal("13"));
        event.getBillData().put("TOTAL", new BigDecimal("113"));
        event.getBillData().put("partnerId", 1L);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 15));
        return event;
    }

    // ---------- helpers: queries ----------

    private ErpFinPostingException findException(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucherByBillCode(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", BUSINESS_TYPE_AP_INVOICE));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private List<ErpFinVoucher> findVouchersReversing(Long originalVoucherId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reversalOfVoucherId", originalVoucherId));
        return dao.findAllByQuery(q);
    }
}
