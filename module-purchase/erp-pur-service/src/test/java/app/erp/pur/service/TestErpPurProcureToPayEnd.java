package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
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
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 端到端集成测试：采购到付款全链 PO→Receive→Invoice→Pay(settle 部分核销) + 反向冲销。
 *
 * <p>完整链路经 {@link IGraphQLEngine} 推进，断言全链状态/posted/paidStatus 一致：
 * 建订单(APPROVED)→入库审核(触发库存移动 DONE + PURCHASE_INPUT 暂估凭证 posted=true)→
 * 发票审核(三单匹配通过 + AP_INVOICE 凭证 posted=true)→付款审核(PAYMENT 凭证 posted=true)→
 * settle 部分核销(发票 paidStatus=PARTIAL)。另覆盖反向：发票 reverseApprove 红字冲销 AP + posted 反转；
 * 付款 reverseSettlement + reverseApprove 红字冲销 PAYMENT。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurProcureToPayEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long SUPPLIER_ID = 2401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long ACCT_SCHEMA_ID = 7401L;
    static final Long LOCATION_ID = 4402L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testProcureToPayPartialSettlement() {
        seedPrereqs();
        long orderLineId = 8401L;
        long[] receive = buildReceiveChain("PO-P2P-001", "PR-P2P-001", 8402L, 8403L, orderLineId,
                new BigDecimal("10"), new BigDecimal("5"));

        // 1. 入库审核：触发库存移动 DONE + PURCHASE_INPUT 暂估凭证 posted=true
        assertEquals(0, submitReceive(receive[0]).getStatus());
        assertEquals(0, approveReceive(receive[0]).getStatus());
        ErpPurReceive approvedReceive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receive[0]);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus());
        assertEquals(true, approvedReceive.getPosted(), "入库 posted=true");

        // 2. 发票审核：三单匹配通过 + AP_INVOICE 凭证 posted=true
        long invoiceId = buildInvoice("PI-P2P-001", receive[1], new BigDecimal("10"), new BigDecimal("5"));
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        ErpPurInvoice approvedInvoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus());
        assertEquals(true, approvedInvoice.getPosted(), "发票 posted=true (AP_INVOICE)");
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, approvedInvoice.getPaidStatus(), "发票初始 UNPAID");

        // 3. 付款审核：PAYMENT 凭证 posted=true
        long paymentId = buildPayment("PY-P2P-001", new BigDecimal("56.5"));
        assertEquals(0, submitPayment(paymentId).getStatus());
        assertEquals(0, approvePayment(paymentId).getStatus());
        ErpPurPayment approvedPayment = daoProvider.daoFor(ErpPurPayment.class).getEntityById(paymentId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedPayment.getApproveStatus());
        assertEquals(true, approvedPayment.getPosted(), "付款 posted=true (PAYMENT)");

        // 4. 部分核销：发票 paidStatus=PARTIAL
        assertEquals(0, settle(paymentId, invoiceId, new BigDecimal("30")).getStatus());
        ErpPurInvoice settledInvoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        ErpPurPayment settledPayment = daoProvider.daoFor(ErpPurPayment.class).getEntityById(paymentId);
        assertEquals(0, new BigDecimal("30").compareTo(settledInvoice.getPaidAmount()), "发票已付=30");
        assertEquals(ErpPurConstants.PAID_STATUS_PARTIAL, settledInvoice.getPaidStatus(), "发票 paidStatus=PARTIAL");
        assertEquals(ErpPurConstants.PAID_STATUS_PARTIAL, settledPayment.getWrittenOffStatus(),
                "付款 writtenOffStatus=PARTIAL");

        // AP_INVOICE 与 PAYMENT 两张凭证均落地
        assertTrue(countVoucherLinks("PI-P2P-001") >= 1, "AP_INVOICE 凭证回链存在");
        assertTrue(countVoucherLinks("PY-P2P-001") >= 1, "PAYMENT 凭证回链存在");
    }

    @Test
    public void testReverseScenarios() {
        seedPrereqs();
        long orderLineId = 8411L;
        long[] receive = buildReceiveChain("PO-REV-001", "PR-REV-001", 8412L, 8413L, orderLineId,
                new BigDecimal("10"), new BigDecimal("5"));

        assertEquals(0, submitReceive(receive[0]).getStatus());
        assertEquals(0, approveReceive(receive[0]).getStatus());

        long invoiceId = buildInvoice("PI-REV-001", receive[1], new BigDecimal("10"), new BigDecimal("5"));
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        assertTrue(Boolean.TRUE.equals(daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId).getPosted()));

        long paymentId = buildPayment("PY-REV-001", new BigDecimal("56.5"));
        assertEquals(0, submitPayment(paymentId).getStatus());
        assertEquals(0, approvePayment(paymentId).getStatus());
        // 先核销，再冲销核销 + 反审核付款
        assertEquals(0, settle(paymentId, invoiceId, new BigDecimal("56.5")).getStatus());
        assertEquals(ErpPurConstants.PAID_STATUS_PAID,
                daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId).getPaidStatus());

        // 反向 1：付款 reverseSettlement 恢复余额 + reverseApprove 红字冲销 PAYMENT
        assertEquals(0, reverseSettlement(paymentId, invoiceId).getStatus());
        ErpPurInvoice invAfter = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(ErpPurConstants.PAID_STATUS_UNPAID, invAfter.getPaidStatus(), "冲销核销后发票回 UNPAID");

        assertEquals(0, reverseApprovePayment(paymentId).getStatus());
        ErpPurPayment payAfter = daoProvider.daoFor(ErpPurPayment.class).getEntityById(paymentId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, payAfter.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(payAfter.getPosted()), "付款反审核后 posted=false (PAYMENT 红冲)");
        long payLinksBefore = countVoucherLinks("PY-REV-001");
        assertTrue(payLinksBefore >= 2, "PAYMENT 原凭证 + 红字冲销凭证均存在");

        // 反向 2：发票 reverseApprove 红字冲销 AP
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        ErpPurInvoice invFinal = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, invFinal.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(invFinal.getPosted()), "发票反审核后 posted=false (AP 红冲)");
        assertTrue(countVoucherLinks("PI-REV-001") >= 2, "AP_INVOICE 原凭证 + 红字冲销凭证均存在");
    }

    // ---------- chain builders ----------

    /**
     * @return [0]=receiveId, [1]=receiveLineId
     */
    private long[] buildReceiveChain(String orderCode, String receiveCode, long receiveId, long receiveLineId,
                                     long orderLineId, BigDecimal qty, BigDecimal price) {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder(orderCode);
            newOrderLine(orderId, orderLineId, qty, price);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, qty, price);
            return null;
        });
        return new long[]{receiveId, receiveLineId};
    }

    private long buildInvoice(String code, long receiveLineId, BigDecimal qty, BigDecimal price) {
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
        BigDecimal amount = qty.multiply(price);
        BigDecimal tax = new BigDecimal("6.5");
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(amount.add(tax));
        invoice.setPosted(false);
        ormTemplate.runInSession(session -> {
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
            line.setTaxRate(new BigDecimal("13"));
            lineDao.saveEntity(line);
            return null;
        });
        return invoice.getId();
    }

    private long buildPayment(String code, BigDecimal total) {
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
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        payment.setWrittenOffStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        payment.setPosted(false);
        ormTemplate.runInSession(session -> {
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
            return null;
        });
        return payment.getId();
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submitReceive(Long id) {
        return rpc(mutation, "ErpPurReceive__submit", Map.of("receiveId", id));
    }

    private ApiResponse<?> approveReceive(Long id) {
        return rpc(mutation, "ErpPurReceive__approve", Map.of("receiveId", id));
    }

    private ApiResponse<?> submitInvoice(Long id) {
        return rpc(mutation, "ErpPurInvoice__submit", Map.of("invoiceId", id));
    }

    private ApiResponse<?> approveInvoice(Long id) {
        return rpc(mutation, "ErpPurInvoice__approve", Map.of("invoiceId", id));
    }

    private ApiResponse<?> reverseApproveInvoice(Long id) {
        return rpc(mutation, "ErpPurInvoice__reverseApprove", Map.of("invoiceId", id));
    }

    private ApiResponse<?> submitPayment(Long id) {
        return rpc(mutation, "ErpPurPayment__submit", Map.of("paymentId", id));
    }

    private ApiResponse<?> approvePayment(Long id) {
        return rpc(mutation, "ErpPurPayment__approve", Map.of("paymentId", id));
    }

    private ApiResponse<?> reverseApprovePayment(Long id) {
        return rpc(mutation, "ErpPurPayment__reverseApprove", Map.of("paymentId", id));
    }

    private ApiResponse<?> settle(Long paymentId, Long invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("paymentId", paymentId);
        req.put("allocations", Collections.singletonList(alloc));
        return rpc(mutation, "ErpPurPayment__settle", req);
    }

    private ApiResponse<?> reverseSettlement(Long paymentId, Long invoiceId) {
        return rpc(mutation, "ErpPurPayment__reverseSettlement", Map.of("paymentId", paymentId, "invoiceId", invoiceId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            // PURCHASE_INPUT（1401/2202）+ AP_INVOICE（1403/2221/2202）+ PAYMENT（2202/1002）
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款");
            seedSubject("1403", "在途物资");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("1002", "银行存款");
            seedAcctSchema();
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, int status) {
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
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private Long newOrder(String code) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        dao.saveEntity(order);
        return order.getId();
    }

    private void newOrderLine(Long orderId, Long lineId, BigDecimal qty, BigDecimal price) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(price);
        line.setAmount(qty.multiply(price));
        dao.saveEntity(line);
    }

    private void newReceive(String code, Long receiveId, Long orderId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setId(receiveId);
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setOrderId(orderId);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(BigDecimal.ONE);
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty, BigDecimal price) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(price);
        dao.saveEntity(line);
    }

    // ---------- query helpers ----------

    private long countVoucherLinks(String billCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> billCode.equals(l.getBillCode())).count();
    }
}
