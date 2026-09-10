package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurPaymentLine;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-CK-pur-015-r3：核销 docStatus 守卫（{@code docs/design/purchase/state-machine.md} §异常路径
 * 「付款核销时发票已作废→拒绝核销」）。修复前 {@link PaymentSettler}#settle/#reverseSettlement 仅守卫
 * approveStatus——已作废（docStatus=CANCELLED）发票可被继续核销、已作废付款单可继续核销/反核销，
 * AR/AP 派生态（paidAmount/paidStatus/writtenOffStatus）与作废语义冲突。
 *
 * <p>与 r1 P2-CK-sal-010（sal 核销侧）/ P2-CK-pur-005（cancel 侧）统一批次设计：域内专用错误码
 * {@code erp.err.pur.settle-invoice-cancelled} / {@code erp.err.pur.settle-payment-cancelled}，参数传码。
 *
 * <p>fixture 为 case 级直接落库（docStatus=CANCELLED + approveStatus=APPROVED 组合，绕过状态机迁移面），
 * 与 finding 缺陷面一致；合法路径控制组断言现状保持。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentSettlementDocStatusGuard extends JunitAutoTestCase {

    static final String ORG_ID = "1101";
    static final String SUPPLIER_ID = "2101";
    static final String CURRENCY_ID = "6101";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 负路径：已作废发票核销拒绝（owner doc §异常路径 L99） ----------

    @Test
    public void testCancelledInvoiceSettleRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-DOC-001", new BigDecimal("113"));
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        ErpPurPayment payment = newApprovedPayment("PY-DOC-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        ApiResponse<?> resp = settle(payment.getId(), invoice.getId(), new BigDecimal("60"));
        assertEquals(ErpPurErrors.ERR_SETTLE_INVOICE_CANCELLED.getErrorCode(), resp.getCode(),
                "已作废发票核销应拒绝（docStatus 守卫，修复前放行）");

        // AR/AP 派生态不变：拒绝路径不得回写 paidAmount/paidStatus/writtenOffStatus
        ErpPurInvoice inv = reloadInvoice(invoice);
        ErpPurPayment pay = reloadPayment(payment);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getPaidAmount()), "拒绝路径发票 paidAmount 保持 0");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, inv.getPaidStatus(), "发票 paidStatus 保持 UNPAID");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, pay.getWrittenOffStatus(), "付款 writtenOffStatus 保持 UNPAID");
        assertTrue(findLines(payment.getId(), invoice.getId()).isEmpty(), "拒绝路径不得产生 PaymentLine");
    }

    // ---------- 负路径：已作废付款单核销拒绝 ----------

    @Test
    public void testCancelledPaymentSettleRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-DOC-002", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-DOC-002", new BigDecimal("113"));
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        ApiResponse<?> resp = settle(payment.getId(), invoice.getId(), new BigDecimal("60"));
        assertEquals(ErpPurErrors.ERR_SETTLE_PAYMENT_CANCELLED.getErrorCode(), resp.getCode(),
                "已作废付款单核销应拒绝（docStatus 守卫，修复前放行）");

        ErpPurInvoice inv = reloadInvoice(invoice);
        ErpPurPayment pay = reloadPayment(payment);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getPaidAmount()), "拒绝路径发票 paidAmount 保持 0");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, inv.getPaidStatus(), "发票 paidStatus 保持 UNPAID");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, pay.getWrittenOffStatus(), "付款 writtenOffStatus 保持 UNPAID");
        assertTrue(findLines(payment.getId(), invoice.getId()).isEmpty(), "拒绝路径不得产生 PaymentLine");
    }

    // ---------- 负路径：已作废单反核销拒绝 ----------

    @Test
    public void testCancelledInvoiceReverseSettlementRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-DOC-003", new BigDecimal("113"));
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        ErpPurPayment payment = newApprovedPayment("PY-DOC-003", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
            seedLine(payment.getId(), invoice.getId(), new BigDecimal("113"));
        });

        ApiResponse<?> resp = reverseSettlement(payment.getId(), invoice.getId());
        assertEquals(ErpPurErrors.ERR_SETTLE_INVOICE_CANCELLED.getErrorCode(), resp.getCode(),
                "已作废发票反核销应拒绝（docStatus 守卫，修复前放行并回写派生态）");

        ErpPurInvoice inv = reloadInvoice(invoice);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getPaidAmount()),
                "拒绝路径不得触发 recomputeInvoicePaid（修复前 paidAmount 被回写 113）");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, inv.getPaidStatus(), "发票 paidStatus 保持 UNPAID");
        assertTrue(findLines(payment.getId(), invoice.getId()).size() == 1,
                "拒绝路径不得追加反向负金额行（仍只有既有 1 条）");
    }

    @Test
    public void testCancelledPaymentReverseSettlementRejected() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-DOC-004", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-DOC-004", new BigDecimal("113"));
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
            seedLine(payment.getId(), invoice.getId(), new BigDecimal("113"));
        });

        ApiResponse<?> resp = reverseSettlement(payment.getId(), invoice.getId());
        assertEquals(ErpPurErrors.ERR_SETTLE_PAYMENT_CANCELLED.getErrorCode(), resp.getCode(),
                "已作废付款单反核销应拒绝（docStatus 守卫，修复前放行）");

        ErpPurPayment pay = reloadPayment(payment);
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, pay.getWrittenOffStatus(),
                "拒绝路径不得触发 recomputePaymentWrittenOff");
        assertTrue(findLines(payment.getId(), invoice.getId()).size() == 1,
                "拒绝路径不得追加反向负金额行（仍只有既有 1 条）");
    }

    // ---------- 合法路径控制组（非作废，现状保持） ----------

    @Test
    public void testActiveDocsSettleAndReverseStillPass() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-DOC-005", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-DOC-005", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, settle(payment.getId(), invoice.getId(), new BigDecimal("113")).getStatus(),
                "非作废单核销路径行为不变");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadInvoice(invoice).getPaidStatus());

        assertEquals(0, reverseSettlement(payment.getId(), invoice.getId()).getStatus(),
                "非作废单反核销路径行为不变");
        assertEquals(0, BigDecimal.ZERO.compareTo(reloadInvoice(invoice).getPaidAmount()), "冲销后发票已付回 0");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, reloadPayment(payment).getWrittenOffStatus());
    }

    // ---------- helpers ----------

    private ApiResponse<?> settle(String paymentId, String invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("paymentId", paymentId);
        req.put("allocations", Collections.singletonList(alloc));
        return executeRpc(mutation, "ErpPurPayment__settle", ApiRequest.build(req));
    }

    private ApiResponse<?> reverseSettlement(String paymentId, String invoiceId) {
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

    private List<ErpPurPaymentLine> findLines(String paymentId, String invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("paymentId", paymentId));
        q.addFilter(eq("invoiceId", invoiceId));
        return daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q);
    }

    private void seedLine(String paymentId, String invoiceId, BigDecimal amount) {
        IEntityDao<ErpPurPaymentLine> dao = daoProvider.daoFor(ErpPurPaymentLine.class);
        ErpPurPaymentLine line = dao.newEntity();
        line.setPaymentId(paymentId);
        line.setInvoiceId(invoiceId);
        line.setAmount(amount);
        dao.saveEntity(line);
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

    private void seedActiveSupplier(String id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }
}
