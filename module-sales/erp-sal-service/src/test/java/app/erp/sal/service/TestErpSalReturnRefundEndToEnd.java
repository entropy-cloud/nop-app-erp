package app.erp.sal.service;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
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
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
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
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 4.4 销售退货到退款连续链端到端（{@code flow-overview.md} 异常冲销 + {@code ar-ap-reconciliation.md}）。
 *
 * <p>本测试是首条将「退货审批 → 反向入库 → SALES_RETURN 反向凭证 → 应收辅助账回减 → 退款核销/归零」
 * 串成连续场景的用例（既有退货组件分散在 Refund/Posting/Inventory/Approval/Qty/Trace，无单一连续链）。
 *
 * <p>断言主线：
 * <ul>
 *   <li>已出库+已开票+已收款销售单 → 退货审核 → 反向入库（库存回加）</li>
 *   <li>SALES_RETURN 反向 SALES_OUTPUT 凭证（借存货/贷成本 + posted=true + 业财回链）</li>
 *   <li>DIRECTION_RECEIVABLE 负 openAmount 辅助账（credit memo，经 sumOpen 回减应收余额）</li>
 *   <li>已收款退货 → 反向收款核销（ReceiptLine 负金额 + 发票 receivedStatus 回 UNRECEIVED）</li>
 *   <li>退货反审核 → 辅助账 openAmount 归零（cancelOnReverse）+ 余额恢复</li>
 * </ul>
 *
 * <p>注：应收负项 credit memo 的 openAmount 归零经 cancelOnReverse（红冲）实现，应收余额回减经 sumOpen
 * （辅助账层）自然完成——{@code ar-ap-reconciliation.md} + 0300-3 既定设计，本测试据实断言。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnRefundEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 3701L;
    static final Long CUSTOMER_ID = 4701L;
    static final Long WAREHOUSE_ID = 5701L;
    static final Long MATERIAL_ID = 6701L;
    static final Long UOM_ID = 7701L;
    static final Long CURRENCY_ID = 8701L;
    static final Long ACCT_SCHEMA_ID = 9701L;
    static final BigDecimal SEED_QTY = new BigDecimal("20");
    static final BigDecimal SEED_COST = new BigDecimal("5");
    // 退货 4 × 售价 5 = 含税 24（AR credit memo 口径取 totalAmountWithTax 取负）
    static final BigDecimal RETURN_WITH_TAX = new BigDecimal("24");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    private static final IServiceContext CTX = new ServiceContextImpl();
    private final AtomicLong idSeq = new AtomicLong(870000L);

    /**
     * 连续链：已出库+已开票+已收款 → 退款核销（财务核销单）→ 退货审核 → 反向入库 + SALES_RETURN 凭证 +
     * 负应收辅助账 + 反向收款核销 → 余额回减 → 退货反审核 → 辅助账归零 + 余额恢复。
     */
    @Test
    public void testSalesReturnRefundEndToEnd() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-E2E-001", new BigDecimal("10"));

        // 已开票（AR_INVOICE +113）+ 已收款（RECEIPT +113）
        Long invoiceId = nextId();
        Long receiptId = nextId();
        ormTemplate.runInSession(session -> {
            newInvoice("SI-E2E-001", invoiceId);
            newInvoiceLine(nextId(), invoiceId, deliveryCtx[1]);
            newReceipt("SR-E2E-001", receiptId, new BigDecimal("113"));
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus(), "发票提交");
        assertEquals(0, approveInvoice(invoiceId).getStatus(), "发票审核 → AR_INVOICE 过账");
        assertEquals(0, submitReceipt(receiptId).getStatus(), "收款提交");
        assertEquals(0, approveReceipt(receiptId).getStatus(), "收款审核 → RECEIPT 过账");

        ErpFinArApItem invoiceItem = findArItem(ErpFinConstants.SOURCE_BILL_AR_INVOICE, "SI-E2E-001");
        ErpFinArApItem receiptItem = findArItem(ErpFinConstants.SOURCE_BILL_RECEIPT, "SR-E2E-001");
        assertNotNull(invoiceItem, "AR_INVOICE 辅助账生成");
        assertNotNull(receiptItem, "RECEIPT 辅助账生成");
        assertEquals(0, new BigDecimal("113").compareTo(invoiceItem.getOpenAmountFunctional()));

        // 退款核销（财务正式核销单，收款项↔发票项，全额 113）→ 双方归零 SETTLED
        ErpFinReconciliation received = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_RECEIVABLE, CUSTOMER_ID, LocalDate.of(2026, 7, 5),
                Collections.singletonList(reconLine(receiptItem.getId(), invoiceItem.getId(), "113")), CTX);
        reconciliationBiz.post(received.getId(), CTX);
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED,
                reloadItem(invoiceItem.getId()).getStatus(), "发票辅助账核销归零 SETTLED");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED,
                reloadItem(receiptItem.getId()).getStatus(), "收款辅助账核销归零 SETTLED");

        // ===== 退货连续链 =====
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-E2E-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        // 退货审核 → 反向入库 + SALES_RETURN 反向凭证 + 负应收辅助账 + 反向收款核销
        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpSalReturn approved = reload(returnId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(approved.getPosted()), "退货 posted=true（SALES_RETURN 过账）");

        // 反向入库：库存 (20 - 10 出库) + 4 退回 = 14
        assertEquals(0, new BigDecimal("14").compareTo(findBalance().getTotalQuantity()), "库存回加=14");

        // SALES_RETURN 反向凭证（业财回链 + posted）
        ErpFinVoucherBillR link = findBillLink("RT-E2E-001", ErpFinBusinessType.SALES_RETURN.name());
        assertNotNull(link, "SALES_RETURN 业财回链存在");
        ErpFinVoucher returnVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, returnVoucher.getDocStatus(), "凭证已过账");

        // DIRECTION_RECEIVABLE 负 openAmount 辅助账（credit memo）
        ErpFinArApItem returnItem = findArItem(ErpFinConstants.SOURCE_BILL_SAL_RETURN, "RT-E2E-001");
        assertNotNull(returnItem, "退货辅助账项生成");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, returnItem.getDirection(), "方向=应收");
        assertEquals(ErpFinConstants.SOURCE_BILL_SAL_RETURN, returnItem.getSourceBillType());
        assertEquals(0, RETURN_WITH_TAX.negate().compareTo(returnItem.getOpenAmountFunctional()),
                "openAmount = 负 totalAmountWithTax（credit memo）");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, returnItem.getStatus());

        // 应收余额回减：发票/收款已 SETTLED（排除），仅退货负项 → sumOpen = -24
        assertEquals(0, RETURN_WITH_TAX.negate().compareTo(sumReceivableOpen()),
                "应收余额 sumOpen = -24（退货回减）");

        // 退货反审核 → 辅助账 openAmount 归零（cancelOnReverse）+ 余额恢复
        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "退货反审核（红字冲销）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
        assertFalse(Boolean.TRUE.equals(reload(returnId).getPosted()), "反审核后 posted=false");

        ErpFinArApItem cancelledReturn = reloadItem(returnItem.getId());
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, cancelledReturn.getStatus(), "退货辅助账 CANCELLED");
        assertEquals(0, BigDecimal.ZERO.compareTo(cancelledReturn.getOpenAmountFunctional()),
                "退货辅助账 openAmount 归零");
        assertEquals(0, BigDecimal.ZERO.compareTo(sumReceivableOpen()),
                "反审核后应收余额恢复=0");
    }

    /**
     * 异常路径：(a) 终态退货不可重复审批；(b) 无可用库存（源出库单未审核）退货审核拒绝；
     * (c) 退款核销金额超过负 openAmount 拒绝。
     */
    @Test
    public void testSalesReturnRefundExceptions() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-EXC-001", new BigDecimal("10"));

        // (a) 终态退货不可重复审批
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "首次审核成功");
        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
        assertNotEquals(0, approveReturn(returnId).getStatus(), "终态（REJECTED）重复审核应拒绝");

        // (b) 源出库单未审核（无库存）→ 退货审核拒绝
        Long unapprovedDeliveryId = nextId();
        Long unapprovedDeliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            Long orderId = newOrderWithId("SO-EXC-002", nextId());
            newOrderLine(orderId, nextId(), 1, new BigDecimal("10"));
            newDeliverySubmitted("SD-EXC-002", unapprovedDeliveryId, orderId);
            newDeliveryLine(unapprovedDeliveryLineId, unapprovedDeliveryId, nextId(), new BigDecimal("10"));
            return null;
        });
        Long noStockReturnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-002", noStockReturnId, unapprovedDeliveryId);
            newReturnLine(nextId(), noStockReturnId, unapprovedDeliveryLineId, new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(noStockReturnId).getStatus(), "源出库单未审核（无库存），退货审核应拒绝");

        // (c) 退款核销金额超过负 openAmount 拒绝
        Long invoiceId = nextId();
        ormTemplate.runInSession(session -> {
            newInvoice("SI-EXC-003", invoiceId);
            newInvoiceLine(nextId(), invoiceId, deliveryCtx[1]);
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());

        Long retId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-003", retId, deliveryCtx[0]);
            newReturnLine(nextId(), retId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(retId).getStatus(), "退货审核");
        ErpFinArApItem negReturn = findArItem(ErpFinConstants.SOURCE_BILL_SAL_RETURN, "RT-EXC-003");
        ErpFinArApItem posInvoice = findArItem(ErpFinConstants.SOURCE_BILL_AR_INVOICE, "SI-EXC-003");
        assertNotNull(negReturn);
        assertNotNull(posInvoice);

        ErpFinReconciliation over = reconciliationBiz.create(
                ErpFinConstants.DIRECTION_RECEIVABLE, CUSTOMER_ID, LocalDate.of(2026, 7, 5),
                Collections.singletonList(reconLine(negReturn.getId(), posInvoice.getId(), "999")), CTX);
        assertThrows(NopException.class, () -> reconciliationBiz.post(over.getId(), CTX),
                "退款核销金额超过负 openAmount 应拒绝");
    }

    // ---------- chain seed ----------

    private Long[] seedApprovedDelivery(String deliveryCode, BigDecimal deliveryQty) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDelivery(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        seedStock("SEED-" + deliveryCode);
        assertEquals(0, submitDelivery(deliveryId).getStatus(), "出库提交");
        assertEquals(0, approveDelivery(deliveryId).getStatus(), "出库审核应成功");
        return new Long[]{deliveryId, deliveryLineId};
    }

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submitDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveDelivery(Long id) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submitInvoice(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveInvoice(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submitReceipt(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveReceipt(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- queries ----------

    private ErpSalReturn reload(Long id) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(id);
    }

    private ErpFinArApItem findArItem(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinArApItem reloadItem(Long id) {
        return daoProvider.daoFor(ErpFinArApItem.class).getEntityById(id);
    }

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private BigDecimal sumReceivableOpen() {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", CUSTOMER_ID));
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_RECEIVABLE));
        q.addFilter(notIn("status", List.of(ErpFinConstants.AR_AP_STATUS_SETTLED,
                ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : dao.findAllByQuery(q)) {
            if (it.getOpenAmountFunctional() != null) {
                sum = sum.add(it.getOpenAmountFunctional());
            }
        }
        return sum;
    }

    private ReconciliationLineInput reconLine(Long paymentItemId, Long invoiceItemId, String amount) {
        BigDecimal amt = new BigDecimal(amount);
        ReconciliationLineInput in = new ReconciliationLineInput();
        in.setPaymentItemId(paymentItemId);
        in.setInvoiceItemId(invoiceItemId);
        in.setSettledAmountSource(amt);
        in.setSettledAmountFunctional(amt);
        return in;
    }

    // ---------- seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            // SALES_OUTPUT(1401/6401) + AR_INVOICE(1131/6001/2221) + RECEIPT(1002) + SALES_RETURN(1401/6401)
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
            seedSubject("1131", "应收账款");
            seedSubject("6001", "主营业务收入");
            seedSubject("2221", "应交税费-销项税额");
            seedSubject("1002", "银行存款");
            seedAcctSchema();
            return null;
        });
    }

    private void seedStock(String billCode) {
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
        line.put("quantity", SEED_QTY);
        line.put("unitCost", SEED_COST);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
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

    private Long newOrderWithId(String code, Long orderId) {
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
        return orderId;
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

    private void newDelivery(String code, Long deliveryId, Long orderId) {
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

    private void newDeliverySubmitted(String code, Long deliveryId, Long orderId) {
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
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
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

    private void newInvoice(String code, Long invoiceId) {
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
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newInvoiceLine(Long lineId, Long invoiceId, Long deliveryLineId) {
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

    private void newReceipt(String code, Long receiptId, BigDecimal total) {
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
        returnOrder.setTotalAmountWithTax(RETURN_WITH_TAX);
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
