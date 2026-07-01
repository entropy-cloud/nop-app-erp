package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurPaymentLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：付款→发票域级核销（{@link app.erp.pur.service.entity.PaymentSettler}）。
 *
 * <p>覆盖部分核销（PARTIAL）、全额核销（PAID）、跨供应商拒绝、超额拒绝、reverseSettlement 恢复余额。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentSettlement extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long SUPPLIER_ID_2 = 2102L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testPartialSettlement() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-SET-001", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-SET-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        // 部分核销 60
        assertEquals(0, settle(payment.getId(), invoice.getId(), new BigDecimal("60")).getStatus());
        ErpPurInvoice inv = reloadInvoice(invoice);
        ErpPurPayment pay = reloadPayment(payment);
        assertEquals(0, new BigDecimal("60").compareTo(inv.getPaidAmount()), "发票已付=60");
        assertEquals(ErpPurConstants.PAID_STATUS_PARTIAL, inv.getPaidStatus(), "发票 paidStatus=PARTIAL");
        assertEquals(ErpPurConstants.PAID_STATUS_PARTIAL, pay.getWrittenOffStatus(), "付款 writtenOffStatus=PARTIAL");
    }

    @Test
    public void testFullSettlement() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-SET-002", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-SET-002", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, settle(payment.getId(), invoice.getId(), new BigDecimal("113")).getStatus());
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadInvoice(invoice).getPaidStatus(), "发票 paidStatus=PAID");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadPayment(payment).getWrittenOffStatus(),
                "付款 writtenOffStatus=PAID");
    }

    @Test
    public void testCrossSupplierRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-SET-003", new BigDecimal("113"));
        invoice.setSupplierId(SUPPLIER_ID_2);
        ErpPurPayment payment = newApprovedPayment("PY-SET-003", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            seedActiveSupplier(SUPPLIER_ID_2);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        ApiResponse<?> bad = settle(payment.getId(), invoice.getId(), new BigDecimal("50"));
        assertEquals(ErpPurErrors.ERR_SETTLE_SUPPLIER_MISMATCH.getErrorCode(), bad.getCode(),
                "跨供应商核销应拒绝");
    }

    @Test
    public void testOverInvoiceBalanceRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-SET-004", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-SET-004", new BigDecimal("500"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        // 核销 200 > 发票余额 113
        ApiResponse<?> bad = settle(payment.getId(), invoice.getId(), new BigDecimal("200"));
        assertEquals(ErpPurErrors.ERR_SETTLE_OVER_INVOICE_BALANCE.getErrorCode(), bad.getCode(),
                "超发票未付余额应拒绝");
    }

    @Test
    public void testReverseSettlementRestoresBalance() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-SET-005", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-SET-005", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, settle(payment.getId(), invoice.getId(), new BigDecimal("113")).getStatus());
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadInvoice(invoice).getPaidStatus());

        // 冲销核销 → 余额恢复
        assertEquals(0, reverseSettlement(payment.getId(), invoice.getId()).getStatus());
        ErpPurInvoice inv = reloadInvoice(invoice);
        ErpPurPayment pay = reloadPayment(payment);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getPaidAmount()), "冲销后发票已付回 0");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, inv.getPaidStatus(), "发票回 UNPAID");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, pay.getWrittenOffStatus(), "付款回 UNPAID");
        // 反向负金额行存在（审计轨迹）
        assertTrue(hasNegativeLine(payment.getId(), invoice.getId()), "应存在反向负金额 PaymentLine");
    }

    // ---------- helpers ----------

    private boolean hasNegativeLine(Long paymentId, Long invoiceId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("paymentId", paymentId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("invoiceId", invoiceId));
        for (ErpPurPaymentLine l : daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q)) {
            if (l.getAmount() != null && l.getAmount().signum() < 0) {
                return true;
            }
        }
        return false;
    }

    private ApiResponse<?> settle(Long paymentId, Long invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("paymentId", paymentId);
        req.put("allocations", Collections.singletonList(alloc));
        return executeRpc(mutation, "ErpPurPayment__settle", ApiRequest.build(req));
    }

    private ApiResponse<?> reverseSettlement(Long paymentId, Long invoiceId) {
        return executeRpc(mutation, "ErpPurPayment__reverseSettlement",
                ApiRequest.build(Map.of("paymentId", paymentId, "invoiceId", invoiceId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reloadInvoice(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private ErpPurPayment reloadPayment(ErpPurPayment payment) {
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    private ErpPurInvoice newApprovedInvoice(String code, BigDecimal withTax) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(withTax);
        invoice.setTotalTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private ErpPurPayment newApprovedPayment(String code, BigDecimal total) {
        ErpPurPayment payment = new ErpPurPayment();
        payment.setCode(code);
        payment.setOrgId(ORG_ID);
        payment.setSupplierId(SUPPLIER_ID);
        payment.setBusinessDate(LocalDate.of(2026, 7, 1));
        payment.setCurrencyId(CURRENCY_ID);
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setTotalAmount(total);
        payment.setAmountSource(total);
        payment.setAmountFunctional(total);
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        payment.setWrittenOffStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        payment.setPosted(false);
        return payment;
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }
}
