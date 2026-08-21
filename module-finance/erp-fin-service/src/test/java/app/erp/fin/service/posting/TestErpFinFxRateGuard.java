package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdCurrency;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外币过账汇率缺失守卫测试（RC-R1.42 / P1-RC-002，L1 UC-FIN-12 断言②「若 汇率缺失 → 报错拒绝过账」）。
 *
 * <p>直断言范式（R1.32），经 {@link IErpFinVoucherBiz#post} 引擎级断言 {@code prepareContext}
 * 的 {@code guardExchangeRate} 守卫行为。覆盖（对齐 plan Phase 3 测试矩阵）：
 * <ol>
 *   <li>外币（非本位币）rate 缺失 → 拒绝 + {@code ERR_EXCHANGE_RATE_REQUIRED} + 凭证/回链零落库；</li>
 *   <li>外币 rate 显式传（6.5）→ 放行 + 行级 rate/币种落库断言；</li>
 *   <li>本位币 rate 缺失 → 放行（rate=1，回归既有单币种语义）；</li>
 *   <li>币种不存在 + rate 缺失 → 保守放行（D2 残余风险回归断言）。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinFxRateGuard extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    // ---------- ① 外币 rate 缺失 → 拒绝 ----------

    @Test
    public void testForeignCurrencyMissingRateRejected() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            seedCurrency("1", "RMB", true);
            seedCurrency("2", "USD", false);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // 外币 USD 且 exchangeRate 缺失 → 守卫拒绝
        PostingEvent event = apInvoiceEvent("FX-REJECT-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"), "2", null);

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.post(event, CTX)),
                "外币汇率缺失应抛 NopException 拒绝过账");
        assertEquals(ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED.getErrorCode(), ex.getErrorCode(),
                "错误码应为 ERR_EXCHANGE_RATE_REQUIRED");
        assertEquals("USD", ex.getParam(ErpFinErrors.ARG_CURRENCY_CODE), "错误参数含币种编码");
        assertEquals(0, countBillLinks("FX-REJECT-001", ErpFinBusinessType.AP_INVOICE.name()), "被拒不应落库回链");
    }

    // ---------- ② 外币 rate 显式传 → 放行 ----------

    @Test
    public void testForeignCurrencyExplicitRatePasses() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        BigDecimal rate = new BigDecimal("6.5");
        seed(() -> {
            seedOpenPeriod(voucherDate);
            seedCurrency("1", "RMB", true);
            seedCurrency("2", "USD", false);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        PostingEvent event = apInvoiceEvent("FX-OK-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"), "2", rate);

        String voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "外币显式传 rate 应放行");
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        assertEquals(3, lines.size(), "行数不变");
        for (ErpFinVoucherLine line : lines) {
            assertEquals(0, line.getExchangeRate().compareTo(rate), "行级 exchangeRate 落库为显式值 6.5");
            assertEquals("2", line.getCurrencyId(), "行级币种为外币 USD(2)");
        }
    }

    // ---------- ③ 本位币 rate 缺失 → 放行 rate=1 ----------

    @Test
    public void testFunctionalCurrencyMissingRateDefaultsToOne() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            seedCurrency("1", "RMB", true);
            seedCurrency("2", "USD", false);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // 本位币 RMB 且 exchangeRate 缺失 → 保留 rate=1 语义（既有单币种主路径零回归）
        PostingEvent event = apInvoiceEvent("FX-FUNC-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"), "1", null);

        String voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "本位币 rate 缺失应放行");
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        assertEquals(3, lines.size(), "行数不变");
        for (ErpFinVoucherLine line : lines) {
            assertEquals(0, line.getExchangeRate().compareTo(BigDecimal.ONE), "本位币回退 rate=1");
            assertEquals("1", line.getCurrencyId(), "行级币种为本位币 RMB(1)");
        }
    }

    // ---------- ④ 币种不存在 + rate 缺失 → 保守放行（D2 残余风险） ----------

    @Test
    public void testUnknownCurrencyMissingRateConservativelyPasses() {
        LocalDate voucherDate = LocalDate.of(2026, 6, 15);
        seed(() -> {
            seedOpenPeriod(voucherDate);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税");
            seedSubject("2202", "应付账款");
            seedApInvoiceTemplate();
        });

        // currencyId=999 无对应币种行 + rate 缺失 → 无法判定本位币归属，保守放行 rate=1
        PostingEvent event = apInvoiceEvent("FX-UNKNOWN-001", voucherDate,
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"), "999", null);

        String voucherId = ormTemplate.runInSession(session -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "币种不存在应保守放行");
        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        for (ErpFinVoucherLine line : lines) {
            assertEquals(0, line.getExchangeRate().compareTo(BigDecimal.ONE), "保守放行 rate=1");
        }
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private PostingEvent apInvoiceEvent(String billHeadCode, LocalDate voucherDate, BigDecimal amount,
                                        BigDecimal tax, BigDecimal total, String currencyId, BigDecimal exchangeRate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId("1");
        event.setOrgId("1");
        event.setCurrencyId(currencyId);
        event.setExchangeRate(exchangeRate);
        event.setVoucherDate(voucherDate);
        event.getBillData().put("AMOUNT", amount);
        event.getBillData().put("TAX", tax);
        event.getBillData().put("TOTAL", total);
        event.getBillData().put("partnerId", "1");
        event.getBillData().put("businessDate", voucherDate);
        return event;
    }

    private void seedApInvoiceTemplate() {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
        tpl.setCode("TPL-FX-TEST");
        tpl.setName("汇率守卫测试模板");
        tpl.setBusinessType(ErpFinBusinessType.AP_INVOICE.name());
        tpl.setVoucherType("TRANSFER");
        tpl.setIsActive(true);
        dao.saveEntity(tpl);

        IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
        lineDao.saveEntity(templateLine(tpl.getId(), 1, "6602", DC_DEBIT, "AMOUNT"));
        lineDao.saveEntity(templateLine(tpl.getId(), 2, "2221", DC_DEBIT, "TAX"));
        lineDao.saveEntity(templateLine(tpl.getId(), 3, "2202", DC_CREDIT, "TOTAL"));
    }

    private ErpFinVoucherTemplateLine templateLine(String templateId, int lineNo, String subjectCode,
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

    private void seedCurrency(String id, String code, boolean isFunctional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(id);
        currency.setCode(code);
        currency.setName(code);
        currency.setIsFunctional(isFunctional);
        dao.saveEntity(currency);
    }

    private void seedOpenPeriod(LocalDate voucherDate) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-06");
        period.setName("2026-06");
        period.setOrgId("1");
        period.setYear(2026);
        period.setMonth(6);
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private List<ErpFinVoucherLine> linesOf(String voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = new java.util.ArrayList<>(dao.findAllByQuery(q));
        lines.sort(java.util.Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));
        return lines;
    }

    private long countBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        return dao.findAllByQuery(q).size();
    }
}
