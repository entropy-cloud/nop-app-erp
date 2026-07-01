package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应收应付辅助账（ErpFinArApItem）生成集成测试（Phase 1）。验证过账成功后同事务生成辅助账项：
 * 方向/金额/openAmount/status 正确，幂等（重复过账不重复生成），红冲后原项置 CANCELLED。
 *
 * <p>直接调用凭证聚合根 Facade {@link IErpFinVoucherBiz} 的 Java API，断言辅助账落库状态。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinArApItemGeneration extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final int DC_DEBIT = 10;
    static final int DC_CREDIT = 20;
    static final int BUSINESS_TYPE_AP_INVOICE = ErpFinBusinessType.AP_INVOICE.getCode();
    static final int VOUCHER_TYPE_TRANSFER = 30;
    static final int PERIOD_STATUS_OPEN = 10;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testApInvoiceGeneratesPayableItem() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedSubject("2202", "应付账款");
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-ARAP-001", voucherDate, 2L,
                new BigDecimal("113"));

        Long voucherId = voucherBiz.post(event, CTX);
        assertNotNull(voucherId, "前置：过账成功");

        List<ErpFinArApItem> items = findItems("AP_INVOICE", "AP-ARAP-001");
        assertEquals(1, items.size(), "AP_INVOICE 过账应生成 1 条应付辅助账项");
        ErpFinArApItem item = items.get(0);
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, item.getDirection(), "方向=应付");
        assertEquals("AP_INVOICE", item.getSourceBillType());
        assertEquals(2L, item.getPartnerId());
        assertEquals(0, item.getAmountFunctional().compareTo(new BigDecimal("113")), "本位币金额=113");
        assertEquals(0, item.getOpenAmountFunctional().compareTo(new BigDecimal("113")), "未核销=113");
        assertEquals(0, item.getSettledAmountFunctional().compareTo(BigDecimal.ZERO), "已核销=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, item.getStatus(), "状态=未核销");
        assertEquals(voucherDate, item.getBusinessDate());
    }

    @Test
    public void testReceiptGeneratesReceivableItem() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 16);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedSubject("1002", "银行存款");
            seedSubject("1122", "应收账款");
            seedReceiptTemplate();
        });

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.RECEIPT);
        event.setBillHeadCode("RC-ARAP-001");
        event.setAcctSchemaId(1L);
        event.setOrgId(1L);
        event.setCurrencyId(1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("partnerId", 3L);
        event.getBillData().put("AMOUNT", new BigDecimal("200"));
        event.getBillData().put("businessDate", voucherDate);

        Long voucherId = voucherBiz.post(event, CTX);
        assertNotNull(voucherId, "前置：收款过账成功");

        List<ErpFinArApItem> items = findItems("RECEIPT", "RC-ARAP-001");
        assertEquals(1, items.size(), "RECEIPT 过账应生成 1 条应收辅助账项");
        ErpFinArApItem item = items.get(0);
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, item.getDirection(), "方向=应收");
        assertEquals("RECEIPT", item.getSourceBillType());
        assertEquals(0, item.getOpenAmountFunctional().compareTo(new BigDecimal("200")));
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, item.getStatus());
    }

    @Test
    public void testIdempotent() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 17);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedSubject("2202", "应付账款");
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("AP-ARAP-IDEM", voucherDate, 1L, new BigDecimal("100"));

        voucherBiz.post(event, CTX);
        voucherBiz.post(event, CTX);

        List<ErpFinArApItem> items = findItems("AP_INVOICE", "AP-ARAP-IDEM");
        assertEquals(1, items.size(), "幂等：重复过账不应重复生成辅助账项");
    }

    @Test
    public void testReverseCancelsItem() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 18);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedSubject("2202", "应付账款");
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedApInvoiceTemplate();
        });

        voucherBiz.post(apInvoiceEvent("AP-ARAP-REV", voucherDate, 1L, new BigDecimal("150")), CTX);

        List<ErpFinArApItem> before = findItems("AP_INVOICE", "AP-ARAP-REV");
        assertEquals(1, before.size());
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, before.get(0).getStatus());

        voucherBiz.reverse("AP-ARAP-REV", ErpFinBusinessType.AP_INVOICE, CTX);

        List<ErpFinArApItem> after = findItems("AP_INVOICE", "AP-ARAP-REV");
        assertEquals(1, after.size(), "红冲不新增辅助账项，而是取消原项");
        ErpFinArApItem cancelled = after.get(0);
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, cancelled.getStatus(), "红冲后原项状态=已作废");
        assertEquals(0, cancelled.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "红冲后未核销余额=0");
    }

    @Test
    public void testNonArApTypeNoOp() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 19);
        seed(() -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedSubject("1403", "原材料");
            seedSubject("2202", "应付账款");
            seedPurchaseInputTemplate();
        });

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PURCHASE_INPUT);
        event.setBillHeadCode("PO-NOARAP-001");
        event.setAcctSchemaId(1L);
        event.setOrgId(1L);
        event.setCurrencyId(1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("AMOUNT", new BigDecimal("500"));

        Long voucherId = voucherBiz.post(event, CTX);
        assertNotNull(voucherId);
        // PURCHASE_INPUT 非 AR/AP 类型，不应生成辅助账
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        assertTrue(dao.findAllByQuery(new QueryBean()).isEmpty(), "非 AR/AP 业务类型不应生成辅助账");
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private PostingEvent apInvoiceEvent(String billHeadCode, LocalDate voucherDate, long partnerId, BigDecimal total) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(1L);
        event.setOrgId(1L);
        event.setCurrencyId(1L);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("AMOUNT", total);
        event.getBillData().put("TAX", BigDecimal.ZERO);
        event.getBillData().put("TOTAL", total);
        event.getBillData().put("partnerId", partnerId);
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
        lineDao.saveEntity(tplLine(tpl.getId(), 1, "6602", DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(tplLine(tpl.getId(), 2, "2221", DC_DEBIT, "TAX"));
        lineDao.saveEntity(tplLine(tpl.getId(), 3, "2202", DC_CREDIT, "TOTAL"));
    }

    private void seedReceiptTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-RECEIPT");
        tpl.setName("收款模板");
        tpl.setBusinessType(ErpFinBusinessType.RECEIPT.getCode());
        tpl.setVoucherType(VOUCHER_TYPE_TRANSFER);
        tpl.setIsActive(true);
        dao.saveEntity(tpl);
        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(tplLine(tpl.getId(), 1, "1002", DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(tplLine(tpl.getId(), 2, "1122", DC_CREDIT, "AMOUNT"));
    }

    private void seedPurchaseInputTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-PURCHASE-INPUT");
        tpl.setName("采购入库模板");
        tpl.setBusinessType(ErpFinBusinessType.PURCHASE_INPUT.getCode());
        tpl.setVoucherType(VOUCHER_TYPE_TRANSFER);
        tpl.setIsActive(true);
        dao.saveEntity(tpl);
        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(tplLine(tpl.getId(), 1, "1403", DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(tplLine(tpl.getId(), 2, "2202", DC_CREDIT, "AMOUNT"));
    }

    private ErpFinVoucherTemplateLine tplLine(Long templateId, int lineNo, String subjectCode,
                                              int dcDirection, String amountKey) {
        ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setSubjectCode(subjectCode);
        line.setDcDirection(dcDirection);
        line.setAmountKey(amountKey);
        line.setAccountKey("X");
        return line;
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private List<ErpFinArApItem> findItems(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q);
    }
}
