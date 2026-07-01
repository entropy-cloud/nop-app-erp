package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReceipt;
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
 * Phase 3 端到端集成测试：销售到收款全链 SO→Delivery→Invoice→Receipt（部分核销），含反向冲销。
 *
 * <p>完整正向链路：订单(预 APPROVED)→出库审核(SALES_OUTPUT 凭证 + 库存移动 + posted)→发票审核(AR_INVOICE 凭证 + posted)
 * →收款审核(RECEIPT 凭证 + posted)→部分核销(invoice.receivedStatus=PARTIAL)。断言全链状态/posted/receivedStatus 一致。
 *
 * <p>反向链路：发票反审核（红字冲销 AR 凭证，posted=false）、收款 reverseSettlement + 反审核（红字冲销 RECEIPT 凭证）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderToCashEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long CUSTOMER_ID = 2401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long ACCT_SCHEMA_ID = 7401L;
    static final int VOUCHER_STATUS_POSTED = 20;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testOrderToCashEndPartialReceipt() {
        seedPrereqs();
        long orderLine = 9401L;
        long deliveryId = 9402L;
        long deliveryLineId = 9403L;
        long invoiceId = 9404L;
        long invoiceLineId = 9405L;
        long receiptId = 9406L;

        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-CASH-001");
            newOrderLine(orderId, orderLine, 1, new BigDecimal("10"));
            newDelivery("SD-CASH-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLine, new BigDecimal("10"));
            return null;
        });
        // 预置库存 20 @ 5 = 100（出库校验可用量 + SALES_OUTPUT 成本快照）
        seedStock("SEED-CASH-001", new BigDecimal("20"), new BigDecimal("5"));

        // 1. 出库审核 → SALES_OUTPUT 凭证 + 库存扣减 + posted + 订单 DELIVERED
        assertEquals(0, submitDelivery(deliveryId).getStatus(), "出库提交");
        assertEquals(0, approveDelivery(deliveryId).getStatus(), "出库审核");
        ErpSalDelivery delivered = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, delivered.getApproveStatus());
        assertEquals(true, delivered.getPosted(), "出库 posted=true（SALES_OUTPUT 凭证经库存移动单调起，凭证回链键为移动单编码，见 TestErpSalOrderToDeliveryEnd）");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_DELIVERED,
                daoProvider.daoFor(ErpSalOrder.class).findAllByQuery(new QueryBean()).stream()
                        .filter(o -> "SO-CASH-001".equals(o.getCode())).findFirst().orElseThrow()
                        .getDeliveryStatus(),
                "订单发货状态回写 DELIVERED");

        // 2. 发票审核 → AR_INVOICE 凭证 + posted（回链出库行）
        ormTemplate.runInSession(session -> {
            newInvoice("SI-CASH-001", invoiceId);
            newInvoiceLine(invoiceLineId, invoiceId, deliveryLineId);
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus(), "发票提交");
        assertEquals(0, approveInvoice(invoiceId).getStatus(), "发票审核");
        ErpSalInvoice invoiced = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, invoiced.getApproveStatus());
        assertEquals(true, invoiced.getPosted(), "发票 posted=true");
        ErpFinVoucher arVoucher = findVoucher("SI-CASH-001");
        assertNotNull(arVoucher, "AR_INVOICE 凭证落地");
        assertEquals(0, arVoucher.getTotalDebit().compareTo(new BigDecimal("113")), "AR 借应收 113");

        // 3. 收款审核 → RECEIPT 凭证 + posted
        ormTemplate.runInSession(session -> {
            newReceipt("SR-CASH-001", receiptId, new BigDecimal("113"));
            return null;
        });
        assertEquals(0, submitReceipt(receiptId).getStatus(), "收款提交");
        assertEquals(0, approveReceipt(receiptId).getStatus(), "收款审核");
        ErpSalReceipt receipted = daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receiptId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, receipted.getApproveStatus());
        assertEquals(true, receipted.getPosted(), "收款 posted=true");
        ErpFinVoucher rcVoucher = findVoucher("SR-CASH-001");
        assertNotNull(rcVoucher, "RECEIPT 凭证落地");

        // 4. 部分核销 60 → invoice receivedStatus=PARTIAL
        assertEquals(0, settle(receiptId, invoiceId, new BigDecimal("60")).getStatus(), "部分核销 60");
        ErpSalInvoice inv = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        ErpSalReceipt rec = daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receiptId);
        assertEquals(0, new BigDecimal("60").compareTo(inv.getReceivedAmount()), "发票已收=60");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_PARTIAL, inv.getReceivedStatus(), "发票 PARTIAL");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_PARTIAL, rec.getWrittenOffStatus(), "收款 PARTIAL");
    }

    @Test
    public void testReverseInvoiceAndReceipt() {
        seedPrereqs();
        long orderLine = 9411L;
        long deliveryId = 9412L;
        long deliveryLineId = 9413L;
        long invoiceId = 9414L;
        long invoiceLineId = 9415L;
        long receiptId = 9416L;

        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            Long orderId = newOrder("SO-CASH-002");
            newOrderLine(orderId, orderLine, 1, new BigDecimal("10"));
            newDelivery("SD-CASH-002", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLine, new BigDecimal("10"));
            return null;
        });
        seedStock("SEED-CASH-002", new BigDecimal("20"), new BigDecimal("5"));
        assertEquals(0, submitDelivery(deliveryId).getStatus());
        assertEquals(0, approveDelivery(deliveryId).getStatus());

        ormTemplate.runInSession(session -> {
            newInvoice("SI-CASH-002", invoiceId);
            newInvoiceLine(invoiceLineId, invoiceId, deliveryLineId);
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        assertTrue(Boolean.TRUE.equals(daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId).getPosted()));

        ormTemplate.runInSession(session -> {
            newReceipt("SR-CASH-002", receiptId, new BigDecimal("113"));
            return null;
        });
        assertEquals(0, submitReceipt(receiptId).getStatus());
        assertEquals(0, approveReceipt(receiptId).getStatus());
        assertEquals(0, settle(receiptId, invoiceId, new BigDecimal("113")).getStatus(), "先全额核销");

        // 反向 1：发票反审核 → 红字冲销 AR 凭证，posted=false
        long arLinksBefore = countVoucherLinks("SI-CASH-002");
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus(), "发票反审核");
        ErpSalInvoice invRev = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, invRev.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(invRev.getPosted()), "发票 posted 反转 false");
        assertTrue(countVoucherLinks("SI-CASH-002") > arLinksBefore, "生成红字 AR 冲销凭证");

        // 反向 2：收款 reverseSettlement 恢复余额 + 反审核冲销 RECEIPT 凭证
        assertEquals(0, reverseSettlement(receiptId, invoiceId).getStatus(), "收款核销冲销");
        ErpSalInvoice invAfter = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(invAfter.getReceivedAmount()), "冲销后发票已收回 0");

        long rcLinksBefore = countVoucherLinks("SR-CASH-002");
        assertEquals(0, reverseApproveReceipt(receiptId).getStatus(), "收款反审核");
        ErpSalReceipt recRev = daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receiptId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, recRev.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(recRev.getPosted()), "收款 posted 反转 false");
        assertTrue(countVoucherLinks("SR-CASH-002") > rcLinksBefore, "生成红字 RECEIPT 冲销凭证");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submitDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__submit", ApiRequest.build(Map.of("deliveryId", id)));
    }

    private ApiResponse<?> approveDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("deliveryId", id)));
    }

    private ApiResponse<?> submitInvoice(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__submit", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> approveInvoice(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__approve", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> reverseApproveInvoice(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__reverseApprove", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> submitReceipt(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__submit", ApiRequest.build(Map.of("receiptId", id)));
    }

    private ApiResponse<?> approveReceipt(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__approve", ApiRequest.build(Map.of("receiptId", id)));
    }

    private ApiResponse<?> reverseApproveReceipt(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__reverseApprove", ApiRequest.build(Map.of("receiptId", id)));
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

    private ApiResponse<?> reverseSettlement(Long receiptId, Long invoiceId) {
        return executeRpc(mutation, "ErpSalReceipt__reverseSettlement",
                ApiRequest.build(Map.of("receiptId", receiptId, "invoiceId", invoiceId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            // SALES_OUTPUT
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
            // AR_INVOICE
            seedSubject("1131", "应收账款");
            seedSubject("6001", "主营业务收入");
            seedSubject("2221", "应交税费-销项税额");
            // RECEIPT
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

    private void seedStock(String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "SEED_STOCK");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
    }

    private Long newOrder(String code) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
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
        return order.getId();
    }

    private void newOrderLine(Long orderId, long lineId, int lineNo, BigDecimal qty) {
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

    private void newDelivery(String code, long deliveryId, Long orderId) {
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
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        delivery.setPosted(false);
        dao.saveEntity(delivery);
    }

    private void newDeliveryLine(long lineId, long deliveryId, long orderLineId, BigDecimal qty) {
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

    private void newInvoice(String code, long invoiceId) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setId(invoiceId);
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
        // 含税 113 = 不含税 100 + 税 13
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newInvoiceLine(long lineId, long invoiceId, long deliveryLineId) {
        IEntityDao<ErpSalInvoiceLine> dao = daoProvider.daoFor(ErpSalInvoiceLine.class);
        ErpSalInvoiceLine line = new ErpSalInvoiceLine();
        line.setId(lineId);
        line.setInvoiceId(invoiceId);
        line.setDeliveryLineId(deliveryLineId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        dao.saveEntity(line);
    }

    private void newReceipt(String code, long receiptId, BigDecimal total) {
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
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        receipt.setWrittenOffStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        receipt.setPosted(false);
        dao.saveEntity(receipt);
    }

    // ---------- query helpers ----------

    private ErpFinVoucherBillR findVoucherLink(String billCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> billCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private ErpFinVoucher findVoucher(String billCode) {
        ErpFinVoucherBillR link = findVoucherLink(billCode);
        if (link == null) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
    }

    private long countVoucherLinks(String billCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> billCode.equals(l.getBillCode())).count();
    }
}
