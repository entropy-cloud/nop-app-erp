package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：采购发票 AP_INVOICE 过账端到端。
 *
 * <p>seed 会计期间+科目（purchase→finance，用 DAO 直建 master-data，合法），经 {@link IGraphQLEngine}
 * 调 {@code ErpPurInvoice__approve}，断言凭证落库、发票 posted=true、凭证分录方向与金额（AP_INVOICE
 * 借 费用/采购 + 借 进项税 / 贷 应付）。另覆盖反审核红字冲销反转 posted。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurInvoicePosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1003L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7003L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveGeneratesApInvoiceVoucherAndPosted() {
        seedPeriodAndSubjects();

        ErpPurInvoice invoice = invoiceOf("PI-POST-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, approve(invoice.getId()).getStatus(), "approve 应成功");

        ErpPurInvoice reloaded = reload(invoice);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertEquals(true, reloaded.getPosted(), "审核应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(invoice.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        // 借方 = 费用/采购100 + 进项税13 = 113；贷方 = 应付 113
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0,
                "借方合计=100+13=113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0,
                "贷方合计=应付 113");
        assertEquals(3, countLines(voucher.getId()), "AP_INVOICE 凭证 3 行（借采购/借进项税/贷应付）");
    }

    @Test
    public void testReverseApproveGeneratesRedVoucher() {
        seedPeriodAndSubjects();

        ErpPurInvoice invoice = invoiceOf("PI-REV-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus());
        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()), "先过账 posted=true");
        long voucherCountBefore = countAllVoucherLinks(invoice.getCode());

        assertEquals(0, reverseApprove(invoice.getId()).getStatus(), "反审核应成功（先红字冲销）");
        ErpPurInvoice reloaded = reload(invoice);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus(),
                "反审核 → REJECTED");
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()), "反审核后 posted 反转为 false");

        long voucherCountAfter = countAllVoucherLinks(invoice.getCode());
        assertTrue(voucherCountAfter > voucherCountBefore, "反审核应生成红字冲销凭证（新增业财回链）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reload(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private ErpPurInvoice invoiceOf(String code, BigDecimal amount, BigDecimal tax, BigDecimal withTax) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private void saveInvoiceWithLine(ErpPurInvoice invoice) {
        daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
        IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        lineDao.saveEntity(line);
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1403", "在途物资");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2202", "应付账款");
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

    private ErpFinVoucherBillR findBillLink(String invoiceCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(
                new QueryBean());
        return links.stream().filter(l -> invoiceCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countAllVoucherLinks(String invoiceCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> invoiceCode.equals(l.getBillCode())).count();
    }

    private long countLines(Long voucherId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> dao = daoProvider
                .daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }
}
