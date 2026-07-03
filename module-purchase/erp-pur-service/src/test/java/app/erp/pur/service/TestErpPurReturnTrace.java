package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockMove;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 采购退货追溯挂链测试：退货审核后退货移动单 {@code originReturnedMoveId} 指向原入库移动单，
 * 且 {@code returnTrace(原入库移动单)} 双向贯通返回该退货移动单（解除 0456-1 Deferred「追溯」语义）。
 *
 * <p>前置：审核源入库单（生成入库移动）→ 审核退货单（生成反向出库移动并挂退货链上链）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnTrace extends JunitAutoTestCase {

    static final Long ORG_ID = 3601L;
    static final Long SUPPLIER_ID = 4601L;
    static final Long WAREHOUSE_ID = 5601L;
    static final Long MATERIAL_ID = 6601L;
    static final Long UOM_ID = 7601L;
    static final Long CURRENCY_ID = 8601L;
    static final Long ACCT_SCHEMA_ID = 9601L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(560000L);

    @Test
    public void testReturnMoveLinkedToSourceReceiveMove() {
        seedPeriodAndSubjects();
        String receiveCode = "PR-TRC-001";
        Long[] receiveCtx = seedApprovedReceive(receiveCode, new BigDecimal("10"), new BigDecimal("5"));
        Long receiveId = receiveCtx[0];
        Long receiveLineId = receiveCtx[1];

        Long returnId = nextId();
        Long returnLineId = nextId();
        String returnCode = "RT-TRC-001";
        ormTemplate.runInSession(session -> {
            newReturn(returnCode, returnId, receiveId);
            newReturnLine(returnLineId, returnId, receiveLineId, new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpInvStockMove receiveMove = findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receiveCode);
        assertNotNull(receiveMove, "源入库单应已生成入库移动单");
        ErpInvStockMove returnMove = findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnCode);
        assertNotNull(returnMove, "应生成退货出库移动单");

        assertEquals(receiveMove.getId(), returnMove.getOriginReturnedMoveId(),
                "退货移动单 originReturnedMoveId 应指向源入库移动单");

        // returnTrace(源入库移动单) 双向贯通返回退货移动单
        assertTrue(returnTraceContains(receiveMove.getId(), returnMove.getId()),
                "returnTrace(源入库移动单) 应返回该退货移动单");
    }

    @Test
    public void testReturnTraceFromReturnMoveReachesOriginal() {
        seedPeriodAndSubjects();
        String receiveCode = "PR-TRC-002";
        Long[] receiveCtx = seedApprovedReceive(receiveCode, new BigDecimal("10"), new BigDecimal("5"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        String returnCode = "RT-TRC-002";
        ormTemplate.runInSession(session -> {
            newReturn(returnCode, returnId, receiveCtx[0]);
            newReturnLine(returnLineId, returnId, receiveCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpInvStockMove receiveMove = findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receiveCode);
        ErpInvStockMove returnMove = findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnCode);

        // 给定退货移动单 → 其原入库移动单
        assertTrue(returnTraceContains(returnMove.getId(), receiveMove.getId()),
                "returnTrace(退货移动单) 应返回源入库移动单");
    }

    // ---------- end-to-end seed: 审核源入库单（生成入库移动 + 库存）----------

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
        assertEquals(0, approveReceive(receiveId).getStatus(), "源入库单审核应成功");
        return new Long[]{receiveId, receiveLineId};
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approveReceive(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> approveReturn(Long returnId) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("returnId", returnId)));
    }

    private boolean returnTraceContains(Long rootMoveId, Long expectedNodeId) {
        ApiResponse<?> resp = executeRpc(query, "ErpInvStockMove__returnTrace",
                ApiRequest.build(Map.of("moveId", rootMoveId)));
        assertEquals(0, resp.getStatus(), "returnTrace 应成功");
        Object nodes = ((Map<?, ?>) resp.getData()).get("nodes");
        if (!(nodes instanceof List)) {
            return false;
        }
        for (Object node : (List<?>) nodes) {
            Object id = ((Map<?, ?>) node).get("id");
            Long lid = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
            if (expectedNodeId.equals(lid)) {
                return true;
            }
        }
        return false;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

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
        returnOrder.setTotalAmount(new BigDecimal("20"));
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

    private ErpInvStockMove findMove(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
