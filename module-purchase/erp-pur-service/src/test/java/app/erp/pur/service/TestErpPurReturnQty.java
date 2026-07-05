package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Phase 1 服务层集成测试：退货数量上限校验（按入库行聚合已审核退货量，{@code returns.md §退货数量限制}）。
 *
 * <p>覆盖超额退货拒绝、部分退货放行、跨退货单累计上限、退货原因必填拒绝。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnQty extends JunitAutoTestCase {

    static final Long ORG_ID = 3301L;
    static final Long SUPPLIER_ID = 4301L;
    static final Long WAREHOUSE_ID = 5301L;
    static final Long MATERIAL_ID = 6301L;
    static final Long UOM_ID = 7301L;
    static final Long CURRENCY_ID = 8301L;
    static final Long ACCT_SCHEMA_ID = 9301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(700000L);

    @Test
    public void testQtyExceedRejected() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-Q1-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q1-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("12"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "退货 12 > 入库 10 应被拒");
    }

    @Test
    public void testPartialReturnAllowed() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-Q2-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q2-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("7"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "部分退货 7 ≤ 10 应放行");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testAccumulatedReturnLimit() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-Q3-001", new BigDecimal("10"), new BigDecimal("5"));

        Long return1 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q3-001", return1, receiveCtx[0]);
            newReturnLine(nextId(), return1, receiveCtx[1], new BigDecimal("6"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(return1).getStatus(), "第一单退货 6 应放行");

        Long return2 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q3-002", return2, receiveCtx[0]);
            // 已审核退货累计 6，剩余可退 4；再退 6 应被拒
            newReturnLine(nextId(), return2, receiveCtx[1], new BigDecimal("6"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(return2).getStatus(), "累计已退 6，再退 6 > 可退 4 应被拒");

        Long return3 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q3-003", return3, receiveCtx[0]);
            newReturnLine(nextId(), return3, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(return3).getStatus(), "再退 4 = 剩余可退 4 应放行");
    }

    @Test
    public void testReasonRequiredRejected() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-Q4-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-Q4-001", returnId, receiveCtx[0]);
            newReturnLineNoReason(nextId(), returnId, receiveCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "退货原因缺失（默认必填）应被拒");
    }

    // ---------- seed ----------

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

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

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
        returnOrder.setTotalAmount(new BigDecimal("15"));
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long receiveLineId, BigDecimal qty, BigDecimal unitPrice) {
        newReturnLine(lineId, returnId, receiveLineId, qty, unitPrice, "质量不合格");
    }

    private void newReturnLineNoReason(Long lineId, Long returnId, Long receiveLineId, BigDecimal qty,
                                       BigDecimal unitPrice) {
        newReturnLine(lineId, returnId, receiveLineId, qty, unitPrice, null);
    }

    private void newReturnLine(Long lineId, Long returnId, Long receiveLineId, BigDecimal qty, BigDecimal unitPrice,
                               String reason) {
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
        line.setReason(reason);
        dao.saveEntity(line);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
