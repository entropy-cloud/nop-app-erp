package app.erp.sal.service;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R1.9 多币种 O2C + 收款核销汇兑损益集成测试（P1-MA2-009 闭合）。
 *
 * <p>外币 AR_INVOICE（rate=7.0）+ 外币 RECEIPT（rate=7.1）+ 核销，config-gated
 * {@code erp-fin.recon-fx-gain-loss-enabled=true}。断言：
 * <ul>
 *   <li>凭证行级 amountSource ≠ amountFunctional（多币种折算正确）</li>
 *   <li>核销生成 EXCHANGE_GAIN_LOSS 凭证（6051 汇兑损益科目），金额 = 收付款 functional − 发票 functional</li>
 *   <li>核销单 fxGainLoss 回写正确</li>
 * </ul>
 *
 * <p>金额设计：发票源币 1130 @7.0 → functional 7910；收款源币 1130 @7.1 → functional 8023。
 * 核销 FX = 8023 − 7910 = 113（应收收益 → Dr 应收 113 / Cr 汇兑损益 113）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalMultiCurrencyReconFx extends JunitAutoTestCase {

    static final Long ORG_ID = 1701L;
    static final Long CUSTOMER_ID = 2701L;
    static final Long CURRENCY_FC = 6701L;
    static final Long ACCT_SCHEMA_ID = 7701L;
    static final BigDecimal RATE_INV = new BigDecimal("7.0");
    static final BigDecimal RATE_RECV = new BigDecimal("7.1");
    static final BigDecimal SOURCE_AMT = new BigDecimal("1130");

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testMultiCurrencyReconciliationFxGain() {
        seedPrereqs();
        // 启用核销汇兑损益 + 配置科目编码
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_RECON_FX_GAIN_LOSS_ENABLED, "true");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_AR_SUBJECT_CODE, "1131");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_FX_GAIN_LOSS_SUBJECT_CODE, "6051");

        // 1. 外币 AR_INVOICE（rate=7.0）
        PostingEvent invEvent = arInvoiceEvent("AR-MC-001", RATE_INV);
        Long invVoucherId = ormTemplate.runInSession(s -> voucherBiz.post(invEvent, CTX));
        assertNotNull(invVoucherId, "外币 AR_INVOICE 凭证生成");
        assertLineMultiCurrency(invVoucherId, RATE_INV);

        // 2. 外币 RECEIPT（rate=7.1）
        PostingEvent rcvEvent = receiptEvent("RC-MC-001", RATE_RECV);
        Long rcvVoucherId = ormTemplate.runInSession(s -> voucherBiz.post(rcvEvent, CTX));
        assertNotNull(rcvVoucherId, "外币 RECEIPT 凭证生成");

        // 3. 查找辅助账项
        ErpFinArApItem invoiceItem = findArApItem("AR-MC-001");
        ErpFinArApItem receiptItem = findArApItem("RC-MC-001");
        assertNotNull(invoiceItem, "AR_INVOICE 辅助账生成");
        assertNotNull(receiptItem, "RECEIPT 辅助账生成");
        assertEquals(0, new BigDecimal("7910").compareTo(invoiceItem.getAmountFunctional()),
                "发票 functional = 1130×7.0 = 7910");
        assertEquals(0, new BigDecimal("8023").compareTo(receiptItem.getAmountFunctional()),
                "收款 functional = 1130×7.1 = 8023");

        // 4. 核销
        ReconciliationLineInput line = new ReconciliationLineInput();
        line.setInvoiceItemId(invoiceItem.getId());
        line.setPaymentItemId(receiptItem.getId());
        line.setSettledAmountSource(SOURCE_AMT);
        line.setSettledAmountFunctional(new BigDecimal("7910"));

        ErpFinReconciliation head = ormTemplate.runInSession(s ->
                reconciliationBiz.create(ErpFinConstants.DIRECTION_RECEIVABLE, CUSTOMER_ID,
                        LocalDate.of(2026, 7, 25), Collections.singletonList(line), CTX));
        ormTemplate.flushSession();

        ErpFinReconciliation posted = ormTemplate.runInSession(s ->
                reconciliationBiz.post(head.getId(), CTX));

        // 5. 断言核销单 fxGainLoss
        assertEquals(0, new BigDecimal("113").compareTo(posted.getFxGainLoss()),
                "fxGainLoss = 8023 − 7910 = 113（收益）");

        // 6. 断言 FX 凭证生成
        ErpFinVoucherBillR fxLink = findBillLink("RECON-FX-" + head.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name());
        assertNotNull(fxLink, "核销汇兑损益凭证已生成");
        ErpFinVoucher fxVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(fxLink.getVoucherId());
        assertEquals(0, new BigDecimal("113").compareTo(fxVoucher.getTotalDebit()), "FX 凭证借方 113");
        assertEquals(0, new BigDecimal("113").compareTo(fxVoucher.getTotalCredit()), "FX 凭证贷方 113");

        // FX 凭证行：Dr 应收 113 / Cr 汇兑损益 113（应收收益）
        List<ErpFinVoucherLine> fxLines = loadLines(fxLink.getVoucherId());
        boolean hasArDebit = false;
        boolean hasFxCredit = false;
        for (ErpFinVoucherLine l : fxLines) {
            if ("1131".equals(l.getSubjectCode()) && l.getDebitAmount().compareTo(new BigDecimal("113")) == 0) {
                hasArDebit = true;
            }
            if ("6051".equals(l.getSubjectCode()) && l.getCreditAmount().compareTo(new BigDecimal("113")) == 0) {
                hasFxCredit = true;
            }
        }
        assertTrue(hasArDebit, "FX 凭证含 Dr 应收 113（收益）");
        assertTrue(hasFxCredit, "FX 凭证含 Cr 汇兑损益 113");

        // 恢复 config
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_RECON_FX_GAIN_LOSS_ENABLED, "false");
    }

    /** 断言凭证行级多币种分离：amountSource ≠ amountFunctional，functional = source × rate（按行自身 source）。 */
    private void assertLineMultiCurrency(Long voucherId, BigDecimal rate) {
        List<ErpFinVoucherLine> lines = loadLines(voucherId);
        for (ErpFinVoucherLine l : lines) {
            if (l.getAmountSource() != null && l.getAmountSource().compareTo(BigDecimal.ZERO) != 0) {
                assertTrue(l.getAmountSource().compareTo(l.getAmountFunctional()) != 0,
                        "多币种行 amountSource≠amountFunctional: " + l.getSubjectCode());
                BigDecimal expectedFunctional = l.getAmountSource().multiply(rate);
                assertEquals(0, expectedFunctional.compareTo(l.getAmountFunctional()),
                        "functional = source×rate: " + l.getSubjectCode());
            }
        }
    }

    // ---------- helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1131", "应收账款", "ASSET", "DEBIT");
            seedSubject("6001", "主营业务收入", "REVENUE", "CREDIT");
            seedSubject("2221", "应交税费-销项税额", "LIABILITY", "CREDIT");
            seedSubject("1002", "银行存款", "ASSET", "DEBIT");
            seedSubject("6051", "汇兑损益", "EXPENSE", "DEBIT");
            seedAcctSchema();
            seedActivePartner();
        });
    }

    private PostingEvent arInvoiceEvent(String code, BigDecimal rate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AR_INVOICE);
        event.setBillHeadCode(code);
        event.setAcctSchemaId(ACCT_SCHEMA_ID);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_FC);
        event.setExchangeRate(rate);
        event.setVoucherDate(LocalDate.of(2026, 7, 10));
        event.getBillData().put("TOTAL_AMOUNT", new BigDecimal("1000"));
        event.getBillData().put("TOTAL_TAX_AMOUNT", new BigDecimal("130"));
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", SOURCE_AMT);
        event.getBillData().put("CUSTOMER_ID", CUSTOMER_ID);
        event.getBillData().put("partnerId", CUSTOMER_ID);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 10));
        return event;
    }

    private PostingEvent receiptEvent(String code, BigDecimal rate) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.RECEIPT);
        event.setBillHeadCode(code);
        event.setAcctSchemaId(ACCT_SCHEMA_ID);
        event.setOrgId(ORG_ID);
        event.setCurrencyId(CURRENCY_FC);
        event.setExchangeRate(rate);
        event.setVoucherDate(LocalDate.of(2026, 7, 20));
        event.getBillData().put("TOTAL", SOURCE_AMT);
        event.getBillData().put("CUSTOMER_ID", CUSTOMER_ID);
        event.getBillData().put("partnerId", CUSTOMER_ID);
        event.getBillData().put("businessDate", LocalDate.of(2026, 7, 20));
        return event;
    }

    private ErpFinArApItem findArApItem(String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        q.setLimit(1);
        List<ErpFinArApItem> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", businessType));
        q.setLimit(1);
        List<ErpFinVoucherBillR> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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
        schema.setCode("AS-O2CMC");
        schema.setName("O2C多币种账套");
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_FC);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActivePartner() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-MC");
        partner.setName("外币客户");
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        dao.saveEntity(partner);
    }
}
