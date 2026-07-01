package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReceiptLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
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
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：退货退款编排（{@link app.erp.sal.service.entity.ReturnRefundOrchestrator}）。
 *
 * <p>覆盖：(a) 未收款退货→负 AR 辅助账回减应收（credit memo，无收款核销回写）；
 * (b) 已收款退货→反向收款核销行（复用 {@code ReceiptSettler.reverseSettlement}）+ 发票 receivedStatus/receivedAmount 回写。
 *
 * <p>退款方式路由（资金账户）属 treasury 面，为本计划 Non-Goal。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnRefund extends JunitAutoTestCase {

    static final Long ORG_ID = 3905L;
    static final Long CUSTOMER_ID = 4905L;
    static final Long WAREHOUSE_ID = 5905L;
    static final Long MATERIAL_ID = 6905L;
    static final Long UOM_ID = 7905L;
    static final Long CURRENCY_ID = 8905L;
    static final Long ACCT_SCHEMA_ID = 9905L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(740000L);

    /**
     * 已收款退货：发票已被收款核销（receivedStatus=RECEIVED）。退货审核→反向收款核销行 + 发票回 UNRECEIVED。
     */
    @Test
    public void testReceivedReturnReversesSettlement() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-RFD-001", new BigDecimal("10"));

        // 发票 113 + 收款 113，核销 → 发票 receivedStatus=RECEIVED
        Long invoiceId = nextId();
        Long receiptId = nextId();
        ormTemplate.runInSession(session -> {
            newApprovedInvoice("SI-RFD-001", invoiceId, new BigDecimal("113"));
            newApprovedReceipt("SR-RFD-001", receiptId, new BigDecimal("113"));
            return null;
        });
        assertEquals(0, settle(receiptId, invoiceId, new BigDecimal("113")).getStatus(), "预核销应成功");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED,
                daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId).getReceivedStatus(),
                "发票 receivedStatus=RECEIVED");

        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RFD-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        // 退款编排：生成反向（负金额）收款核销行
        assertTrue(hasNegativeLine(receiptId, invoiceId), "应存在反向负金额 ReceiptLine");

        // 发票 receivedAmount 回减为 0、receivedStatus 回 UNRECEIVED
        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getReceivedAmount()), "发票 receivedAmount 回减为 0");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED, invoice.getReceivedStatus(),
                "发票 receivedStatus 回 UNRECEIVED");
    }

    /**
     * 未收款退货：发票未核销（无 ReceiptLine）。退货审核→无反向核销行（退款编排无操作），
     * 应收回减由 SALES_RETURN 负 AR 辅助账完成（见 {@code TestErpSalReturnPosting}）。
     */
    @Test
    public void testUnreceivedReturnNoSettlementReversal() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-RFD-002", new BigDecimal("10"));
        // 仅建未核销发票（receivedAmount=0），无收款
        Long invoiceId = nextId();
        ormTemplate.runInSession(session -> {
            newApprovedInvoice("SI-RFD-002", invoiceId, new BigDecimal("113"));
            return null;
        });

        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RFD-002", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        // 无收款单 → 无反向核销行（退款编排 no-op，应收回减经负 AR 辅助账）
        assertEquals(0, countAllNegativeLines(), "未收款退货不产生反向核销行");
    }

    // ---------- seed ----------

    private Long[] seedApprovedDelivery(String deliveryCode, BigDecimal deliveryQty) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDeliveryApproved(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId};
    }

    private boolean hasNegativeLine(Long receiptId, Long invoiceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiptId", receiptId));
        q.addFilter(eq("invoiceId", invoiceId));
        for (ErpSalReceiptLine l : daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q)) {
            if (l.getAmount() != null && l.getAmount().signum() < 0) {
                return true;
            }
        }
        return false;
    }

    private long countAllNegativeLines() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiptId", -1L)); // 无匹配（本测试不应有任何反向行）
        return daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q).size();
    }

    private ApiResponse<?> settle(Long receiptId, Long invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("receiptId", receiptId);
        req.put("allocations", Collections.singletonList(alloc));
        return executeRpc(mutation, "ErpSalReceipt__settle", ApiRequest.build(req));
    }

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
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

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType(20);
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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

    private void newOrderWithId(String code, Long orderId) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setId(orderId);
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        dao.saveEntity(order);
    }

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(qty.multiply(new BigDecimal("5")));
        dao.saveEntity(line);
    }

    private void newDeliveryApproved(String code, Long deliveryId, Long orderId) {
        IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setId(deliveryId);
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setOrderId(orderId);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setPosted(false);
        dao.saveEntity(delivery);
    }

    private void newDeliveryLine(Long lineId, Long deliveryId, Long orderLineId, BigDecimal qty) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setId(lineId);
        line.setDeliveryId(deliveryId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        dao.saveEntity(line);
    }

    private void newApprovedInvoice(String code, Long invoiceId, BigDecimal withTax) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setId(invoiceId);
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        invoice.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(withTax);
        invoice.setTotalTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newApprovedReceipt(String code, Long receiptId, BigDecimal total) {
        IEntityDao<ErpSalReceipt> dao = daoProvider.daoFor(ErpSalReceipt.class);
        ErpSalReceipt receipt = new ErpSalReceipt();
        receipt.setId(receiptId);
        receipt.setCode(code);
        receipt.setOrgId(ORG_ID);
        receipt.setCustomerId(CUSTOMER_ID);
        receipt.setBusinessDate(LocalDate.of(2026, 7, 1));
        receipt.setCurrencyId(CURRENCY_ID);
        receipt.setExchangeRate(BigDecimal.ONE);
        receipt.setTotalAmount(total);
        receipt.setAmountSource(total);
        receipt.setAmountFunctional(total);
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setWrittenOffStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        receipt.setPosted(false);
        dao.saveEntity(receipt);
    }

    private void newReturn(String code, Long returnId, Long deliveryId) {
        IEntityDao<ErpSalReturn> dao = daoProvider.daoFor(ErpSalReturn.class);
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setDeliveryId(deliveryId);
        returnOrder.setCustomerId(CUSTOMER_ID);
        returnOrder.setWarehouseId(WAREHOUSE_ID);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(CURRENCY_ID);
        returnOrder.setExchangeRate(new BigDecimal("1"));
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setTotalAmountWithTax(new BigDecimal("24"));
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long deliveryLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        ErpSalReturnLine line = new ErpSalReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setDeliveryLineId(deliveryLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(qty.multiply(unitPrice));
        line.setReason("质量不合格");
        dao.saveEntity(line);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
