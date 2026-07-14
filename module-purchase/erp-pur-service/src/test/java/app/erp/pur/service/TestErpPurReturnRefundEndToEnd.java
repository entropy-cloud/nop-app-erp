package app.erp.pur.service;

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
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
 * 4.4 采购退货到退款连续链端到端（{@code flow-overview.md} 异常冲销 + {@code ar-ap-reconciliation.md}）。
 *
 * <p>本测试是首条将「退货审批 → 反向出库 → PURCHASE_RETURN 红字过账 → 应付辅助账回减 → 退款核销/归零」
 * 串成连续场景的用例（既有退货组件分散在 Posting/Inventory/Approval/Qty/Trace，无单一连续链）。
 *
 * <p>断言主线：
 * <ul>
 *   <li>已入库+已开票采购单 → 退货审核 → 反向出库（库存回减）</li>
 *   <li>PURCHASE_RETURN 红字凭证（取负 + posted=true + 业财回链）</li>
 *   <li>DIRECTION_PAYABLE 负 openAmount 辅助账（credit memo，经 sumOpen 回减应付余额）</li>
 *   <li>退货反审核 → 辅助账 openAmount 归零（cancelOnReverse：CANCELLED + open=0）+ 余额恢复</li>
 * </ul>
 *
 * <p>注：应付/应收正式核销单 {@link ErpFinReconciliation} 的对称结算器仅处理同号项（发票↔收付款）；
 * 退货负项 credit memo 的 openAmount 归零经 cancelOnReverse（红冲）实现，应付余额回减经 sumOpen（辅助账层）
 * 自然完成——这是 {@code ar-ap-reconciliation.md} + 0300-3 既定设计，本测试据实断言。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnRefundEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 3601L;
    static final Long SUPPLIER_ID = 4601L;
    static final Long WAREHOUSE_ID = 5601L;
    static final Long MATERIAL_ID = 6601L;
    static final Long UOM_ID = 7601L;
    static final Long CURRENCY_ID = 8601L;
    static final Long ACCT_SCHEMA_ID = 9601L;
    static final BigDecimal RETURN_AMOUNT = new BigDecimal("20"); // 4 × 5

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    private static final IServiceContext CTX = new ServiceContextImpl();
    private final AtomicLong idSeq = new AtomicLong(860000L);

    // WORKFLOW 模式下付款单 submit 会启动 wf 实例，wf 引擎校验 caller 需 resolved 用户。
    // 用 SYS（id=0）：submit 步骤 owner 解析为 SYS，caller=0 匹配跳过委托校验，避免 NopAuthUser 查询。
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    /**
     * 连续链：已入库+已开票+已付款 → 退货审核 → 反向出库 + PURCHASE_RETURN 红字凭证 + 负应付辅助账 →
     * 余额回减 → 退货反审核 → 辅助账归零（CANCELLED）+ 余额恢复。
     */
    @Test
    public void testPurchaseReturnRefundEndToEnd() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-E2E-001", new BigDecimal("10"), new BigDecimal("5"));

        // 已开票（AP_INVOICE +56.5 辅助账）+ 已付款（PAYMENT +56.5 辅助账）
        Long invoiceId = nextId();
        Long paymentId = nextId();
        ormTemplate.runInSession(session -> {
            newInvoice("PI-E2E-001", invoiceId, receiveCtx[1], new BigDecimal("10"), new BigDecimal("5"));
            newPayment("PY-E2E-001", paymentId, new BigDecimal("56.5"));
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus(), "发票提交");
        assertEquals(0, approveInvoice(invoiceId).getStatus(), "发票审核 → AP_INVOICE 过账");
        assertEquals(0, submitPayment(paymentId).getStatus(), "付款提交");
        assertEquals(0, approvePayment(paymentId).getStatus(), "付款审核 → PAYMENT 过账");

        ErpFinArApItem invoiceItem = findApItem(ErpFinConstants.SOURCE_BILL_AP_INVOICE, "PI-E2E-001");
        ErpFinArApItem paymentItem = findApItem(ErpFinConstants.SOURCE_BILL_PAYMENT, "PY-E2E-001");
        assertNotNull(invoiceItem, "AP_INVOICE 辅助账生成");
        assertNotNull(paymentItem, "PAYMENT 辅助账生成");
        assertEquals(0, new BigDecimal("56.5").compareTo(invoiceItem.getOpenAmountFunctional()));

        // 退款核销（财务正式核销单，发票↔付款，全额 56.5）→ 双方归零 SETTLED
        ErpFinReconciliation paid = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, SUPPLIER_ID, LocalDate.of(2026, 7, 5),
                Collections.singletonList(reconLine(paymentItem.getId(), invoiceItem.getId(), "56.5")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(paid.getId(), CTX));
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED,
                reloadItem(invoiceItem.getId()).getStatus(), "发票辅助账核销归零 SETTLED");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED,
                reloadItem(paymentItem.getId()).getStatus(), "付款辅助账核销归零 SETTLED");

        // ===== 退货连续链 =====
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-E2E-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        // 退货审核 → 反向出库 + PURCHASE_RETURN 红字过账 + 负应付辅助账
        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpPurReturn approved = reload(returnId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(approved.getPosted()), "退货 posted=true（PURCHASE_RETURN 红字过账）");

        // 反向出库：库存 10 - 4 = 6
        assertEquals(0, new BigDecimal("6").compareTo(findBalance().getTotalQuantity()), "库存回减=6");

        // PURCHASE_RETURN 红字凭证（取负 + posted + 业财回链）
        ErpFinVoucherBillR link = findBillLink("RT-E2E-001", ErpFinBusinessType.PURCHASE_RETURN.name());
        assertNotNull(link, "PURCHASE_RETURN 业财回链存在");
        ErpFinVoucher returnVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, returnVoucher.getDocStatus(), "凭证已过账");
        assertEquals(0, RETURN_AMOUNT.compareTo(returnVoucher.getTotalDebit()), "红字凭证借方=退货金额 20");
        assertEquals(0, RETURN_AMOUNT.compareTo(returnVoucher.getTotalCredit()), "红字凭证贷方=退货金额 20");

        // DIRECTION_PAYABLE 负 openAmount 辅助账（credit memo）
        ErpFinArApItem returnItem = findApItem(ErpFinConstants.SOURCE_BILL_PUR_RETURN, "RT-E2E-001");
        assertNotNull(returnItem, "退货辅助账项生成");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, returnItem.getDirection(), "方向=应付");
        assertEquals(ErpFinConstants.SOURCE_BILL_PUR_RETURN, returnItem.getSourceBillType());
        assertEquals(0, RETURN_AMOUNT.negate().compareTo(returnItem.getOpenAmountFunctional()),
                "openAmount = 负 totalAmount（credit memo）");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, returnItem.getStatus(), "退货辅助账 OPEN");

        // 应付余额回减：发票/付款已 SETTLED（排除），仅退货负项 → sumOpen = -20
        assertEquals(0, RETURN_AMOUNT.negate().compareTo(sumPayableOpen()),
                "应付余额 sumOpen = -20（退货回减 totalAmount）");

        // 退货反审核 → 辅助账 openAmount 归零（cancelOnReverse：CANCELLED + open=0）+ 余额恢复
        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "退货反审核（红字冲销）");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
        assertFalse(Boolean.TRUE.equals(reload(returnId).getPosted()), "反审核后 posted=false");

        ErpFinArApItem cancelledReturn = reloadItem(returnItem.getId());
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, cancelledReturn.getStatus(), "退货辅助账 CANCELLED");
        assertEquals(0, BigDecimal.ZERO.compareTo(cancelledReturn.getOpenAmountFunctional()),
                "退货辅助账 openAmount 归零");
        assertEquals(0, BigDecimal.ZERO.compareTo(sumPayableOpen()),
                "反审核后应付余额恢复=0（负项 CANCELLED 排除）");
    }

    /**
     * 异常路径：(a) 终态退货不可重复审批；(b) 无可用库存（源入库单未审核）退货审核拒绝；
     * (c) 退款核销金额超过负 openAmount 拒绝。
     */
    @Test
    public void testPurchaseReturnRefundExceptions() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-EXC-001", new BigDecimal("10"), new BigDecimal("5"));

        // (a) 终态退货不可重复审批：审核 → 反审核（REJECTED）→ 再次审核非法
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "首次审核成功");
        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
        assertNotEquals(0, approveReturn(returnId).getStatus(), "终态（REJECTED）重复审核应拒绝");

        // (b) 源入库单未审核（无库存）→ 退货审核拒绝
        Long unapprovedReceiveId = nextId();
        Long unapprovedReceiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            Long orderId = newOrder("PO-EXC-002");
            newOrderLine(orderId, nextId(), 1, new BigDecimal("10"));
            newReceiveSubmitted("PR-EXC-002", unapprovedReceiveId, orderId);
            newReceiveLineNoOrder(unapprovedReceiveLineId, unapprovedReceiveId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        Long noStockReturnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-002", noStockReturnId, unapprovedReceiveId);
            newReturnLine(nextId(), noStockReturnId, unapprovedReceiveLineId, new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(noStockReturnId).getStatus(), "源入库单未审核（无库存），退货审核应拒绝");

        // (c) 退款核销金额超过负 openAmount 拒绝：退货负项(-20) + 发票正项(+56.5 OPEN)，核销 999 → 超额拒绝
        Long invoiceId = nextId();
        ormTemplate.runInSession(session -> {
            newInvoice("PI-EXC-003", invoiceId, receiveCtx[1], new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());

        Long retId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-EXC-003", retId, receiveCtx[0]);
            newReturnLine(nextId(), retId, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(retId).getStatus(), "退货审核");
        ErpFinArApItem negReturn = findApItem(ErpFinConstants.SOURCE_BILL_PUR_RETURN, "RT-EXC-003");
        ErpFinArApItem posInvoice = findApItem(ErpFinConstants.SOURCE_BILL_AP_INVOICE, "PI-EXC-003");
        assertNotNull(negReturn);
        assertNotNull(posInvoice);

        ErpFinReconciliation over = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, SUPPLIER_ID, LocalDate.of(2026, 7, 5),
                Collections.singletonList(reconLine(negReturn.getId(), posInvoice.getId(), "999")), CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> reconciliationBiz.post(over.getId(), CTX)),
                "退款核销金额超过负 openAmount 应拒绝");
    }

    // ---------- chain seed ----------

    private Long[] seedApprovedReceive(String receiveCode, BigDecimal receiveQty, BigDecimal unitPrice) {
        Long orderLineId = nextId();
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-" + receiveCode);
            newOrderLine(orderId, orderLineId, 1, receiveQty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, receiveQty, unitPrice);
            return null;
        });
        assertEquals(0, executeRpc(mutation, "ErpPurReceive__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId)))).getStatus(), "源入库单提交");
        assertEquals(0, executeRpc(mutation, "ErpPurReceive__approve",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId)))).getStatus(), "源入库单审核应成功");
        return new Long[]{receiveId, receiveLineId};
    }

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submitInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submitPayment(Long id) {
        return executeRpc(mutation, "ErpPurPayment__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approvePayment(Long id) {
        return executeRpc(mutation, "ErpPurPayment__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- queries ----------

    private ErpPurReturn reload(Long id) {
        return daoProvider.daoFor(ErpPurReturn.class).getEntityById(id);
    }

    private ErpFinArApItem findApItem(String sourceBillType, String sourceBillCode) {
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

    private BigDecimal sumPayableOpen() {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", SUPPLIER_ID));
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_PAYABLE));
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
            // PURCHASE_INPUT(1401/2202) + AP_INVOICE(1403/2221/2202) + PAYMENT(2202/1002) + PURCHASE_RETURN(1401/2202)
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
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus("ACTIVE");
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

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
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
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveSubmitted(String code, Long receiveId, Long orderId) {
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
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        dao.saveEntity(line);
    }

    private void newReceiveLineNoOrder(Long lineId, Long receiveId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        dao.saveEntity(line);
    }

    private void newInvoice(String code, Long invoiceId, Long receiveLineId, BigDecimal qty, BigDecimal price) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setId(invoiceId);
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
        dao.saveEntity(invoice);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setInvoiceId(invoiceId);
        line.setReceiveLineId(receiveLineId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(price);
        line.setTaxRate(new BigDecimal("13"));
        daoProvider.daoFor(ErpPurInvoiceLine.class).saveEntity(line);
    }

    private void newPayment(String code, Long paymentId, BigDecimal total) {
        IEntityDao<ErpPurPayment> dao = daoProvider.daoFor(ErpPurPayment.class);
        ErpPurPayment payment = new ErpPurPayment();
        payment.setId(paymentId);
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
        dao.saveEntity(payment);
    }

    private void newReturn(String code, Long returnId, Long receiveId) {
        IEntityDao<ErpPurReturn> dao = daoProvider.daoFor(ErpPurReturn.class);
        ErpPurReturn returnOrder = new ErpPurReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setReceiveId(receiveId);
        returnOrder.setSupplierId(SUPPLIER_ID);
        returnOrder.setWarehouseId(WAREHOUSE_ID);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(CURRENCY_ID);
        returnOrder.setExchangeRate(new BigDecimal("1"));
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(RETURN_AMOUNT);
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long receiveLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpPurReturnLine> dao = daoProvider.daoFor(ErpPurReturnLine.class);
        ErpPurReturnLine line = new ErpPurReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setReceiveLineId(receiveLineId);
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
