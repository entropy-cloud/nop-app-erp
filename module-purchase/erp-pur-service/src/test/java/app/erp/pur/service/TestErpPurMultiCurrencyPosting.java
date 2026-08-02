package app.erp.pur.service;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdPartner;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R1.9 多币种 P2P 本位币凭证路径集成测试（P1-MA2-002 / P1-MA3-039 闭合）。
 *
 * <p>外币 AP_INVOICE + PAYMENT 过账（exchangeRate=7.0≠ONE），断言凭证行级：
 * <ul>
 *   <li>{@code amountSource} = 源币种金额（如 100）</li>
 *   <li>{@code amountFunctional} = source × rate（如 700）—— amountSource ≠ amountFunctional</li>
 *   <li>{@code debitAmount}/{@code creditAmount} 按本位币（700）</li>
 *   <li>凭证头 totalDebit/totalCredit 按本位币平衡</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurMultiCurrencyPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long SUPPLIER_ID = 2601L;
    static final Long CURRENCY_FC = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;
    static final BigDecimal RATE = new BigDecimal("7.0");

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testMultiCurrencyApInvoiceLineAmounts() {
        seedPrereqs();

        // 源币种：amount=100, tax=13, withTax=113。本位币：700, 91, 791。
        PostingEvent event = apInvoiceEvent("AP-MC-001", new BigDecimal("100"), new BigDecimal("13"),
                new BigDecimal("113"), RATE);

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));

        assertNotNull(voucherId, "外币 AP_INVOICE 应生成凭证");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(voucherId);
        // 头合计按本位币：借 700+91=791，贷 791
        assertEquals(0, new BigDecimal("791").compareTo(voucher.getTotalDebit()), "借方合计按本位币 791");
        assertEquals(0, new BigDecimal("791").compareTo(voucher.getTotalCredit()), "贷方合计按本位币 791");

        List<ErpFinVoucherLine> lines = loadLines(voucherId);
        assertEquals(3, lines.size(), "AP_INVOICE 3 行");

        Map<String, ErpFinVoucherLine> byCode = new LinkedHashMap<>();
        for (ErpFinVoucherLine l : lines) {
            byCode.put(l.getSubjectCode(), l);
        }

        // 1403 在途物资：Dr，source=100, functional=700
        ErpFinVoucherLine purchase = byCode.get("1403");
        assertNotNull(purchase, "应有在途物资行");
        assertEquals(0, new BigDecimal("700").compareTo(purchase.getDebitAmount()), "在途物资 debit=functional 700");
        assertEquals(0, new BigDecimal("100").compareTo(purchase.getAmountSource()), "在途物资 amountSource=源币 100");
        assertEquals(0, new BigDecimal("700").compareTo(purchase.getAmountFunctional()), "在途物资 amountFunctional=700");
        assertTrue(purchase.getAmountSource().compareTo(purchase.getAmountFunctional()) != 0,
                "多币种 amountSource ≠ amountFunctional");
        assertEquals(0, RATE.compareTo(purchase.getExchangeRate()), "行汇率=7.0");

        // 2221 进项税：Dr，source=13, functional=91
        ErpFinVoucherLine vat = byCode.get("2221");
        assertEquals(0, new BigDecimal("91").compareTo(vat.getDebitAmount()), "进项税 debit=91");
        assertEquals(0, new BigDecimal("13").compareTo(vat.getAmountSource()), "进项税 amountSource=13");

        // 2202 应付账款：Cr，source=113, functional=791
        ErpFinVoucherLine ap = byCode.get("2202");
        assertEquals(0, new BigDecimal("791").compareTo(ap.getCreditAmount()), "应付 credit=791");
        assertEquals(0, new BigDecimal("113").compareTo(ap.getAmountSource()), "应付 amountSource=113");
    }

    @Test
    public void testMultiCurrencyPaymentLineAmounts() {
        seedPrereqs();

        PostingEvent event = paymentEvent("AP-MC-PAY-001", new BigDecimal("113"), RATE);

        Long voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "外币 PAYMENT 应生成凭证");

        List<ErpFinVoucherLine> lines = loadLines(voucherId);
        assertEquals(2, lines.size(), "PAYMENT 2 行");
        // 借应付 791 / 贷银行 791（本位币），源币各 113
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lines) {
            totalDebit = totalDebit.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
            totalCredit = totalCredit.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
            assertEquals(0, new BigDecimal("113").compareTo(l.getAmountSource()), "源币 113");
            assertEquals(0, new BigDecimal("791").compareTo(l.getAmountFunctional()), "本位币 791");
        }
        assertEquals(0, new BigDecimal("791").compareTo(totalDebit), "借合计本位币 791");
        assertEquals(0, new BigDecimal("791").compareTo(totalCredit), "贷合计本位币 791");
    }

    // ---------- helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1403", "在途物资", "ASSET", "DEBIT");
            seedSubject("2221", "应交税费-进项税额", "LIABILITY", "DEBIT");
            seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
            seedSubject("1002", "银行存款", "ASSET", "DEBIT");
            seedAcctSchema();
            seedActivePartner();
        });
    }

    private PostingEvent apInvoiceEvent(String code, BigDecimal amount, BigDecimal tax, BigDecimal withTax,
                                        BigDecimal rate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(code);
        event.setAcctSchemaId(ACCT_SCHEMA_ID);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_FC);
        event.setExchangeRate(rate);
        event.setVoucherDate(LocalDate.of(2026, 7, 15));
        event.getBillData().put("TOTAL_AMOUNT", amount);
        event.getBillData().put("TOTAL_TAX_AMOUNT", tax);
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", withTax);
        event.getBillData().put("partnerId", SUPPLIER_ID);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 15));
        return event;
    }

    private PostingEvent paymentEvent(String code, BigDecimal total, BigDecimal rate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PAYMENT);
        event.setBillHeadCode(code);
        event.setAcctSchemaId(ACCT_SCHEMA_ID);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_FC);
        event.setExchangeRate(rate);
        event.setVoucherDate(LocalDate.of(2026, 7, 20));
        event.getBillData().put("TOTAL", total);
        event.getBillData().put("partnerId", SUPPLIER_ID);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 20));
        return event;
    }

    private List<ErpFinVoucherLine> loadLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
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
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedAcctSchema() {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao = daoProvider.daoFor(
                app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-MC");
        schema.setName("多币种账套");
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_FC);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActivePartner() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-MC");
        partner.setName("外币供应商");
        partner.setPartnerType("SUPPLIER");
        partner.setStatus("ACTIVE");
        dao.saveEntity(partner);
    }
}
