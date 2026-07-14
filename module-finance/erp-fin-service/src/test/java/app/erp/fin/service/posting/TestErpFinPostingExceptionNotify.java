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
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 行为测试（plan 2026-07-06-0642-1）：过账异常告警通知消费者。
 *
 * <p>覆盖：过账失败 → {@link ErpFinPostingExceptionRecorder} 以 REQUIRES_NEW 落 PENDING 异常记录后
 * 调 {@code IErpSysNotificationBiz.notify("fin.posting-exception", ctx)}；断言：
 * <ul>
 *   <li>notify 被调 + ErpSysNotification 行落入（recipient 匹配 USER_LIST 模板的接收人）</li>
 *   <li>config 关闭（erp-fin.posting-exception-notify-enabled=false）时静默跳过，无新通知行</li>
 * </ul>
 *
 * <p>主过账事务回滚期间触发：异常记录与通知均以独立 REQUIRES_NEW 提交，不随主过账回滚丢失。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPostingExceptionNotify extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.name();
    static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final String PERIOD_STATUS_CLOSED = ErpFinConstants.PERIOD_STATUS_CLOSED;
    static final String NOTIFY_EVENT = ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION;
    static final String RECIPIENT = "fin-posting-recipient";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testFailedPostTriggersNotify() {
        seed(() -> {
            seedPeriod("2026-07-NOTIFY", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    PERIOD_STATUS_CLOSED);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
            seedNotifyTemplate(7102L, RECIPIENT);
        });

        // 期间关闭 → post 失败 → 异常记录独立事务落 PENDING → notify 派发
        PostingEvent event = apInvoiceEvent("AP-NOTIFY-001", LocalDate.of(2026, 7, 15),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)),
                "期间关闭应抛 NopException");

        ErpFinPostingException ex = findException("AP-NOTIFY-001");
        assertNotNull(ex, "异常记录应落 PENDING");

        ErpSysNotification n = findNotification(NOTIFY_EVENT);
        assertNotNull(n, "过账失败应派发 fin.posting-exception 通知");
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    @Test
    public void testNotifyDisabledSkipsDispatch() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_POSTING_EXCEPTION_NOTIFY_ENABLED, "false");
        try {
            seed(() -> {
                seedPeriod("2026-07-NOTIFY-OFF", 2026, 7, LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31), PERIOD_STATUS_CLOSED);
                seedSubject("6602", "管理费用");
                seedSubject("2221", "应交税费-进项税");
                seedSubject("2202", "应付账款");
                seedApInvoiceTemplate();
                seedNotifyTemplate(7112L, RECIPIENT);
            });
            int before = countNotifications(NOTIFY_EVENT);

            PostingEvent event = apInvoiceEvent("AP-NOTIFY-OFF-001", LocalDate.of(2026, 7, 15),
                    new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
            assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)));

            ErpFinPostingException ex = findException("AP-NOTIFY-OFF-001");
            assertNotNull(ex, "config 关闭不影响异常记录落库");

            int after = countNotifications(NOTIFY_EVENT);
            assertEquals(before, after, "config 关闭时应静默跳过 notify 派发");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_POSTING_EXCEPTION_NOTIFY_ENABLED, "true");
        }
    }

    // ---------- helpers ----------

    private ErpFinPostingException findException(String billHeadCode) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billHeadCode));
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpSysNotification findNotification(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q).size();
    }

    private void seedNotifyTemplate(Long id, String recipientUserId) {
        IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
        ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
        t.orm_propValueByName("id", id);
        t.setNotificationType(NOTIFY_EVENT);
        t.setName("过账异常告警");
        t.setChannelSet("IN_APP");
        t.setSubjectTpl("过账异常告警");
        t.setBodyTpl("过账异常: 单据 ${postingNo} 金额 ${amount}，请立即处置");
        t.setRecipientResolver("USER_LIST");
        t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
        t.setMergeWindowSeconds(60);
        t.setMergeStrategy("MERGE_BY_USER_TYPE");
        t.setStatus("ACTIVE");
        dao.saveEntity(t);
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

    private void seedPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
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
