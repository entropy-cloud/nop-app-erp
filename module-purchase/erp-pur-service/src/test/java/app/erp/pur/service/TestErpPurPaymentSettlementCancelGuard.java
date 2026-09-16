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
 * P2-CK-pur-005：Payment cancel/reverseApprove 与 Invoice cancel 的核销守卫
 * （{@code docs/design/purchase/returns.md §异常处理}「已核销退货：需先撤回核销」拒绝语义）。
 * 修复前三入口均不校验 PaymentLine 净核销——已核销付款单被作废/反审核后 GL 凭证红冲但发票
 * paidAmount/paidStatus 派生态陈旧，后续核销/余额判断失真。
 *
 * <p>净额口径：{@code sumNetSettledForPayment/ForInvoice} 含反向负金额行——全额反核销后净额=0 放行。
 * fixture 为 case 级直接落库（APPROVED 态）+ RPC settle 造核销，与 finding 缺陷面一致。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentSettlementCancelGuard extends JunitAutoTestCase {

    static final String ORG_ID = "1201";
    static final String SUPPLIER_ID = "2201";
    static final String CURRENCY_ID = "6201";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 负路径：已核销付款单作废拒绝 ----------

    @Test
    public void testSettledPaymentCancelRejected() {
        String[] ids = seedAndSettle("PI-CG-001", "PY-CG-001", new BigDecimal("113"));
        String invoiceId = ids[0];
        String paymentId = ids[1];

        ApiResponse<?> resp = cancelPayment(paymentId);
        assertEquals(ErpPurErrors.ERR_PAYMENT_SETTLED_EXISTS.getErrorCode(), resp.getCode(),
                "存在净核销的付款单作废应拒绝（修复前 GL 红冲但发票派生态陈旧）");
        assertEquals(ErpPurConstants.DOC_STATUS_ACTIVE, reloadPayment(paymentId).getDocStatus(),
                "拒绝路径付款单 docStatus 保持 ACTIVE");
    }

    // ---------- 负路径：已核销付款单反审核拒绝 ----------

    @Test
    public void testSettledPaymentReverseApproveRejected() {
        String[] ids = seedAndSettle("PI-CG-002", "PY-CG-002", new BigDecimal("113"));
        String paymentId = ids[1];

        ApiResponse<?> resp = reverseApprovePayment(paymentId);
        assertEquals(ErpPurErrors.ERR_PAYMENT_SETTLED_EXISTS.getErrorCode(), resp.getCode(),
                "存在净核销的付款单反审核应拒绝");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                reloadPayment(paymentId).getApproveStatus(), "拒绝路径 approveStatus 保持 APPROVED");
    }

    // ---------- 正路径：反核销后作废放行（净额回 0） ----------

    @Test
    public void testReverseSettlementThenCancelSucceeds() {
        String[] ids = seedAndSettle("PI-CG-003", "PY-CG-003", new BigDecimal("113"));
        String invoiceId = ids[0];
        String paymentId = ids[1];

        assertEquals(0, reverseSettlement(paymentId, invoiceId).getStatus(),
                "全额反核销（负金额行冲平净额）路径行为不变");
        assertEquals(0, cancelPayment(paymentId).getStatus(),
                "净核销=0 后作废放行");
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, reloadPayment(paymentId).getDocStatus(),
                "作废 → docStatus=CANCELLED");
    }

    // ---------- 负路径：已核销发票作废拒绝 ----------

    @Test
    public void testSettledInvoiceCancelRejected() {
        String[] ids = seedAndSettle("PI-CG-004", "PY-CG-004", new BigDecimal("113"));
        String invoiceId = ids[0];

        ApiResponse<?> resp = cancelInvoice(invoiceId);
        assertEquals(ErpPurErrors.ERR_INVOICE_SETTLED_EXISTS.getErrorCode(), resp.getCode(),
                "存在净核销的发票作废应拒绝");
        assertEquals(ErpPurConstants.DOC_STATUS_ACTIVE, reloadInvoice(invoiceId).getDocStatus(),
                "拒绝路径发票 docStatus 保持 ACTIVE");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadInvoice(invoiceId).getPaidStatus(),
                "发票 paidStatus 保持 PAID（派生态未被作废破坏）");
    }

    // ---------- 合法路径控制组：无核销作废行为不变 ----------

    @Test
    public void testUnsettledPaymentAndInvoiceCancelStillPass() {
        ErpPurInvoice invoice = newApprovedInvoice("PI-CG-005", new BigDecimal("113"));
        ErpPurPayment payment = newApprovedPayment("PY-CG-005", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, cancelInvoice(invoice.getId()).getStatus(), "无核销发票作废路径行为不变");
        assertEquals(0, cancelPayment(payment.getId()).getStatus(), "无核销付款单作废路径行为不变");
    }

    // ---------- helpers：seed + settle（RPC 造核销） ----------

    /** 落库 APPROVED 发票+付款单并经 RPC settle 全额核销，返回 {invoiceId, paymentId}。 */
    private String[] seedAndSettle(String invoiceCode, String paymentCode, BigDecimal withTax) {
        ErpPurInvoice invoice = newApprovedInvoice(invoiceCode, withTax);
        ErpPurPayment payment = newApprovedPayment(paymentCode, withTax);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });
        ApiResponse<?> resp = settle(payment.getId(), invoice.getId(), withTax);
        assertEquals(0, resp.getStatus(), "settle 前置造核销成功");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, reloadInvoice(invoice.getId()).getPaidStatus(),
                "核销后发票 paidStatus=PAID");
        return new String[]{invoice.getId(), payment.getId()};
    }

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

    private ApiResponse<?> cancelPayment(String paymentId) {
        return executeRpc(mutation, "ErpPurPayment__cancel", ApiRequest.build(Map.of("paymentId", paymentId)));
    }

    private ApiResponse<?> reverseApprovePayment(String paymentId) {
        return executeRpc(mutation, "ErpPurPayment__reverseApprove",
                ApiRequest.build(Map.of("id", paymentId)));
    }

    private ApiResponse<?> cancelInvoice(String invoiceId) {
        return executeRpc(mutation, "ErpPurInvoice__cancel", ApiRequest.build(Map.of("invoiceId", invoiceId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reloadInvoice(String invoiceId) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
    }

    private ErpPurPayment reloadPayment(String paymentId) {
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(paymentId);
    }

    @SuppressWarnings("unused")
    private List<ErpPurPaymentLine> findLines(String paymentId, String invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("paymentId", paymentId));
        q.addFilter(eq("invoiceId", invoiceId));
        return daoProvider.daoFor(ErpPurPaymentLine.class).findAllByQuery(q);
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
