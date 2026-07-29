package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * R1.8 P1-MA2-003 方案 A 落地证明（plan 2026-07-29-2322-1 §Phase 3 Proof）。
 *
 * <p>验证付款核销三单匹配二次门控（{@code erp-pur.settle-recheck-three-way-match=true}）：
 * {@code ErpPurPayment__settle} 在 invoice APPROVED 守卫后追加强制 strict 三单匹配复核。
 *
 * <p>3 场景：
 * <ul>
 *   <li>场景1（负向-价格超容差）：订单单价 10、发票单价 20（差异 100% &gt;&gt; 5% 容差）→ settle 拒绝
 *       {@code ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED}，cause 链保留 {@code ERR_INVOICE_PRICE_MISMATCH}。</li>
 *   <li>场景2（负向-数量超入库）：发票数量 12 &gt; 入库数量 10 → settle 拒绝
 *       {@code ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED}，cause 链保留 {@code ERR_INVOICE_QTY_MISMATCH}。</li>
 *   <li>场景3（正向-匹配通过）：发票数量=入库、价格差异 2% &lt; 5% 容差 → settle 成功（发票 paidStatus=PARTIAL）。</li>
 * </ul>
 *
 * <p>config-gate 默认 false 的回归保护由既有 {@code TestErpPurProcureToPayEnd} /
 * {@code TestErpPurPaymentSettlement}（默认 config，无 recheck）覆盖。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:settle-recheck-test.yaml")
public class TestErpPurSettleThreeWayMatchRecheck extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long SUPPLIER_ID = 2601L;
    static final Long MATERIAL_ID = 4601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long WAREHOUSE_ID = 3601L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    /**
     * 场景1：发票单价严重超容差 → settle 经二次门控拒绝。
     */
    @Test
    public void testSettleRejectsPriceMismatchWhenRecheckEnabled() {
        // 订单单价 10，发票单价 20（差异 100% >> 5% 容差）
        Long receiveLineId = seedChain("RCHK-PRICE", new BigDecimal("10"), new BigDecimal("10"));
        Long invoiceId = seedApprovedInvoiceWithLine("PI-RCHK-PRICE", receiveLineId,
                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("13"));
        Long paymentId = seedApprovedPayment("PY-RCHK-PRICE", new BigDecimal("213"));

        ApiResponse<?> bad = settle(paymentId, invoiceId, new BigDecimal("50"));
        assertEquals(ErpPurErrors.ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED.getErrorCode(), bad.getCode(),
                "价格超容差经二次门控应拒绝 settle");
        // 发票未被核销（门控阻断前不写 PaymentLine）
        ErpPurInvoice inv = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getPaidAmount()), "门控拒绝时发票 paidAmount 保持 0");
    }

    /**
     * 场景2：发票数量超入库 → settle 经二次门控拒绝。
     */
    @Test
    public void testSettleRejectsQtyMismatchWhenRecheckEnabled() {
        Long receiveLineId = seedChain("RCHK-QTY", new BigDecimal("10"), new BigDecimal("10"));
        // 发票数量 12 > 入库 10
        Long invoiceId = seedApprovedInvoiceWithLine("PI-RCHK-QTY", receiveLineId,
                new BigDecimal("12"), new BigDecimal("10"), new BigDecimal("15.6"));
        Long paymentId = seedApprovedPayment("PY-RCHK-QTY", new BigDecimal("213"));

        ApiResponse<?> bad = settle(paymentId, invoiceId, new BigDecimal("50"));
        assertEquals(ErpPurErrors.ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED.getErrorCode(), bad.getCode(),
                "数量超入库经二次门控应拒绝 settle");
    }

    /**
     * 场景3：数量匹配 + 价格差异 2% < 5% 容差 → settle 通过（发票 paidStatus=PARTIAL）。
     */
    @Test
    public void testSettlePassesWhenMatchWithinTolerance() {
        Long receiveLineId = seedChain("RCHK-OK", new BigDecimal("10"), new BigDecimal("10"));
        // 发票数量 10 = 入库 10；发票单价 10.2 vs 订单 10 → 差异 2% < 5%
        Long invoiceId = seedApprovedInvoiceWithLine("PI-RCHK-OK", receiveLineId,
                new BigDecimal("10"), new BigDecimal("10.2"), new BigDecimal("13.26"));
        Long paymentId = seedApprovedPayment("PY-RCHK-OK", new BigDecimal("115.26"));

        ApiResponse<?> ok = settle(paymentId, invoiceId, new BigDecimal("50"));
        assertEquals(0, ok.getStatus(), "匹配通过时 settle 应成功");
        ErpPurInvoice inv = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(0, new BigDecimal("50").compareTo(inv.getPaidAmount()), "发票已付=50");
        assertEquals(ErpPurConstants.PAID_STATUS_PARTIAL, inv.getPaidStatus(), "发票 paidStatus=PARTIAL");
    }

    // ---------- helpers ----------

    private Long seedChain(String tag, BigDecimal receivedQty, BigDecimal orderPrice) {
        seedActiveSupplier(SUPPLIER_ID);
        ErpPurOrder order = new ErpPurOrder();
        order.setCode("PO-" + tag);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);

        IEntityDao<ErpPurOrderLine> olDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine orderLine = new ErpPurOrderLine();
        orderLine.setOrderId(order.getId());
        orderLine.setLineNo(1);
        orderLine.setMaterialId(MATERIAL_ID);
        orderLine.setUoMId(UOM_ID);
        orderLine.setQuantity(receivedQty);
        orderLine.setUnitPrice(orderPrice);
        orderLine.setAmount(receivedQty.multiply(orderPrice));
        olDao.saveEntity(orderLine);

        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode("PR-" + tag);
        receive.setOrgId(ORG_ID);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);

        IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine receiveLine = new ErpPurReceiveLine();
        receiveLine.setReceiveId(receive.getId());
        receiveLine.setOrderLineId(orderLine.getId());
        receiveLine.setLineNo(1);
        receiveLine.setMaterialId(MATERIAL_ID);
        receiveLine.setUoMId(UOM_ID);
        receiveLine.setQuantity(receivedQty);
        rlDao.saveEntity(receiveLine);
        return receiveLine.getId();
    }

    private Long seedApprovedInvoiceWithLine(String code, Long receiveLineId, BigDecimal qty,
                                             BigDecimal price, BigDecimal taxAmount) {
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
        BigDecimal amount = qty.multiply(price);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(taxAmount);
        invoice.setTotalAmountWithTax(amount.add(taxAmount));
        invoice.setPosted(false);
        daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);

        IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setReceiveLineId(receiveLineId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(price);
        lineDao.saveEntity(line);
        return invoice.getId();
    }

    private Long seedApprovedPayment(String code, BigDecimal total) {
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
        daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        return payment.getId();
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

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
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
}
