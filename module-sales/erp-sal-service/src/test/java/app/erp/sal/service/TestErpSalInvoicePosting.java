package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
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
 * Phase 1 服务层集成测试：销售发票 AR_INVOICE 过账端到端。
 *
 * <p>seed 会计期间+科目（sales→finance，用 DAO 直建 master-data，合法），经 {@link IGraphQLEngine}
 * 调 {@code ErpSalInvoice__approve}，断言凭证落库、发票 posted=true、凭证分录方向与金额（AR_INVOICE
 * 借 应收 / 贷 收入 + 贷 销项税）。另覆盖反审核红字冲销反转 posted。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalInvoicePosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1203L;
    static final Long CUSTOMER_ID = 2201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7103L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveGeneratesArInvoiceVoucherAndPosted() {
        seedPeriodAndSubjects();

        ErpSalInvoice invoice = invoiceOf("SI-POST-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, approve(invoice.getId()).getStatus(), "approve 应成功");

        ErpSalInvoice reloaded = reload(invoice);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertEquals(true, reloaded.getPosted(), "审核应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(invoice.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        // 借方 = 应收 113；贷方 = 收入100 + 销项税13 = 113
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0,
                "借方合计=应收 113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0,
                "贷方合计=100+13=113");
        assertEquals(3, countLines(voucher.getId()), "AR_INVOICE 凭证 3 行（借应收/贷收入/贷销项税）");
    }

    @Test
    public void testReverseApproveGeneratesRedVoucher() {
        seedPeriodAndSubjects();

        ErpSalInvoice invoice = invoiceOf("SI-REV-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus());
        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()), "先过账 posted=true");
        long voucherCountBefore = countAllVoucherLinks(invoice.getCode());

        assertEquals(0, reverseApprove(invoice.getId()).getStatus(), "反审核应成功（先红字冲销）");
        ErpSalInvoice reloaded = reload(invoice);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus(),
                "反审核 → REJECTED");
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()), "反审核后 posted 反转为 false");

        long voucherCountAfter = countAllVoucherLinks(invoice.getCode());
        assertTrue(voucherCountAfter > voucherCountBefore, "反审核应生成红字冲销凭证（新增业财回链）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__approve", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__submit", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__reverseApprove", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

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
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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
