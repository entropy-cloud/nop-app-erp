package app.erp.pur.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
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
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：采购退货 PURCHASE_RETURN 过账端到端——红字冲减凭证（反向 PURCHASE_INPUT：
 * 借暂估应付 / 贷存货）+ DIRECTION_PAYABLE 负 openAmount 辅助账（credit memo）+ 应付余额回减（经
 * {@code sumOpen} 语义，对齐 0300-3 lazy refresh）+ 反审核红字冲销（辅助账 CANCELLED + 余额恢复）。
 *
 * <p>余额证明：供应商应付 = Σ PAYABLE 方向辅助账 openAmountFunctional（排除 SETTLED/CANCELLED），
 * 即 {@code PartnerBalanceUpdater.sumOpen} 的同口径计算。退货负项使该和自然减计退货 totalAmount。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 3401L;
    static final Long SUPPLIER_ID = 4401L;
    static final Long WAREHOUSE_ID = 5401L;
    static final Long MATERIAL_ID = 6401L;
    static final Long UOM_ID = 7401L;
    static final Long CURRENCY_ID = 8401L;
    static final Long ACCT_SCHEMA_ID = 9401L;
    static final BigDecimal RETURN_AMOUNT = new BigDecimal("20"); // 4 × 5

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(800000L);

    @Test
    public void testApproveGeneratesPurchaseReturnVoucherAndNegativeApItem() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-POST-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-POST-001", returnId, receiveCtx[0], RETURN_AMOUNT);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpPurReturn approved = reload(returnId);
        assertTrue(Boolean.TRUE.equals(approved.getPosted()), "审核过账 posted=true");

        // PURCHASE_RETURN 凭证（billHeadCode=return.code，businessType=140）
        ErpFinVoucherBillR link = findBillLink("RT-POST-001", ErpFinBusinessType.PURCHASE_RETURN.name());
        assertNotNull(link, "应生成 PURCHASE_RETURN 业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        // 借 2202 暂估应付 20 / 贷 1401 存货 20
        assertEquals(0, voucher.getTotalDebit().compareTo(RETURN_AMOUNT), "借方合计=20");
        assertEquals(0, voucher.getTotalCredit().compareTo(RETURN_AMOUNT), "贷方合计=20");
        assertEquals(2, countVoucherLines(voucher.getId()), "PURCHASE_RETURN 凭证 2 行");

        // DIRECTION_PAYABLE 负 openAmount 辅助账（credit memo）
        ErpFinArApItem item = findApItem("RT-POST-001");
        assertNotNull(item, "应生成退货辅助账项");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, item.getDirection(), "方向=应付");
        assertEquals(ErpFinConstants.SOURCE_BILL_PUR_RETURN, item.getSourceBillType(), "sourceBillType=PUR_RETURN");
        assertEquals(0, item.getOpenAmountFunctional().compareTo(RETURN_AMOUNT.negate()),
                "openAmountFunctional = 负 totalAmount（credit memo）");

        // 供应商应付余额（sumOpen 口径）= 负 totalAmount（退货回减）
        assertEquals(0, sumPayableOpen().compareTo(RETURN_AMOUNT.negate()),
                "应付余额 sumOpen = -20（退货回减 totalAmount）");

        // 库存余额同步减少（入库 10 - 退货 4 = 6），证明凭证贷存货与物理库存一致
        assertEquals(0, new BigDecimal("6").compareTo(findBalance().getTotalQuantity()), "库存余额=6");
    }

    @Test
    public void testReverseApproveCancelsApItemAndRestoresBalance() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-REV-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-REV-001", returnId, receiveCtx[0], RETURN_AMOUNT);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());
        assertTrue(Boolean.TRUE.equals(reload(returnId).getPosted()), "先过账 posted=true");
        assertEquals(0, sumPayableOpen().compareTo(RETURN_AMOUNT.negate()), "退货后应付=-20");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核应成功（先红字冲销）");
        ErpPurReturn reversed = reload(returnId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "反审核后 posted 反转为 false");

        // 退货辅助账项被取消（status=CANCELLED、openAmount=0）
        ErpFinArApItem item = findApItem("RT-REV-001");
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, item.getStatus(), "辅助账 CANCELLED");
        assertEquals(0, item.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "openAmount=0");

        // 应付余额恢复为 0（负项被取消）
        assertEquals(0, sumPayableOpen().compareTo(BigDecimal.ZERO), "反审核后应付余额恢复=0");
    }

    // ---------- end-to-end seed ----------

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
        assertEquals(0, executeRpc(mutation, "ErpPurReceive__approve",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId)))).getStatus(), "源入库单审核应成功");
        return new Long[]{receiveId, receiveLineId};
    }

    private ErpPurReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpPurReturn.class).getEntityById(returnId);
    }

    // ---------- sumOpen 口径（同 PartnerBalanceUpdater.sumOpen）----------

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

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- queries ----------

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinArApItem findApItem(String returnCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", ErpFinConstants.SOURCE_BILL_PUR_RETURN),
                eq("sourceBillCode", returnCode)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ---------- seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
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

    private void newReturn(String code, Long returnId, Long receiveId, BigDecimal totalAmount) {
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
        returnOrder.setTotalAmount(totalAmount);
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
