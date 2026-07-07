package app.erp.sal.service;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财闭环方向二集成测试（计划 {@code 2026-07-04-1452-2} Phase 3）——销售域。
 *
 * <p>验证财务侧直接红冲已过账凭证时，{@code SalReversalListener} 监听 {@link IErpFinVoucherBiz#reverse}
 * 派发的 {@code VoucherReversedEvent}，回退销售发票状态（posted=false + approveStatus APPROVED→REJECTED）。
 * 对标采购域 {@code TestErpPurFinanceReversalWriteback}，断言同型。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalFinanceReversalWriteback extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1203L;
    static final Long CUSTOMER_ID = 2201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7103L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testFinanceReverseRollsBackSalesInvoicePostedAndApproveStatus() {
        seedPeriodAndSubjects();
        ormTemplate.runInSession(() -> seedActiveCustomer(CUSTOMER_ID));

        ErpSalInvoice invoice = invoiceOf("SI-FIN-REV-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
            invoice.setPosted(true);
            invoice.setPostedAt(CoreMetrics.currentDateTime());
            invoice.setPostedBy("test-user");
            saveInvoiceWithLine(invoice);
        });

        Long originalVoucherId = seedPostedVoucherFor(invoice.getCode(),
                ErpFinBusinessType.AR_INVOICE, new BigDecimal("113"));

        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()),
                "前置：发票已过账 posted=true");

        Long redVoucherId = voucherBiz.reverse(invoice.getCode(), ErpFinBusinessType.AR_INVOICE, CTX);

        assertNotNull(redVoucherId);
        assertNotEquals(originalVoucherId, redVoucherId);

        ErpSalInvoice reloaded = reload(invoice);
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()),
                "方向二：财务红冲后销售发票 posted 应被监听者回退为 false");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus(),
                "方向二：财务红冲后销售发票 approveStatus APPROVED→REJECTED");
    }

    // ---------- helpers ----------

    private ErpSalInvoice reload(ErpSalInvoice invoice) {
        return daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoice.getId());
    }

    private ErpSalInvoice invoiceOf(String code, BigDecimal amount, BigDecimal tax, BigDecimal withTax) {
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private void saveInvoiceWithLine(ErpSalInvoice invoice) {
        daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
        IEntityDao<ErpSalInvoiceLine> lineDao = daoProvider.daoFor(ErpSalInvoiceLine.class);
        ErpSalInvoiceLine line = new ErpSalInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        lineDao.saveEntity(line);
    }

    private Long seedPostedVoucherFor(String billCode, ErpFinBusinessType businessType, BigDecimal total) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        IEntityDao<ErpFinAccountingPeriod> periodDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(eq("code", "2026-07"));
        pq.setLimit(1);
        ErpFinAccountingPeriod period = periodDao.findAllByQuery(pq).get(0);
        return ormTemplate.runInSession(session -> {
            ErpFinVoucher voucher = new ErpFinVoucher();
            voucher.setCode("PST-SEED-" + billCode);
            voucher.setVoucherType("TRANSFER");
            voucher.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
            voucher.setVoucherDate(LocalDate.of(2026, 7, 1));
            voucher.setOrgId(ORG_ID);
            voucher.setAcctSchemaId(ACCT_SCHEMA_ID);
            voucher.setPeriodId(period.getId());
            voucher.setTotalDebit(total);
            voucher.setTotalCredit(total);
            voucher.setIsReversed(false);
            voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
            voucher.setPostedAt(CoreMetrics.currentDateTime());
            vDao.saveEntity(voucher);

            ErpFinVoucherBillR billR = new ErpFinVoucherBillR();
            billR.setVoucherId(voucher.getId());
            billR.setBillType(businessType.name());
            billR.setBillCode(billCode);
            billR.setBusinessType(businessType.name());
            billRDao.saveEntity(billR);
            return voucher.getId();
        });
    }

    private void seedActiveCustomer(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1131", "应收账款");
            seedSubject("6001", "主营业务收入");
            seedSubject("2221", "应交税费-销项税额");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao = daoProvider.daoFor(
                app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
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
