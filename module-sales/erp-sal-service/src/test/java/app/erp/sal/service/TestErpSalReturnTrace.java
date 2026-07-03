package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
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
 * 销售退货追溯挂链测试：退货审核后退货移动单 {@code originReturnedMoveId} 指向原出库移动单，
 * 且 {@code returnTrace(原出库移动单)} 双向贯通返回该退货移动单（解除 0456-2 Deferred「追溯」语义）。
 *
 * <p>前置：预置库存 → 审核源出库单（生成出库移动）→ 审核退货单（生成反向入库移动并挂退货链上链）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnTrace extends JunitAutoTestCase {

    static final Long ORG_ID = 3801L;
    static final Long CUSTOMER_ID = 4801L;
    static final Long WAREHOUSE_ID = 5801L;
    static final Long MATERIAL_ID = 6801L;
    static final Long UOM_ID = 7801L;
    static final Long CURRENCY_ID = 8801L;
    static final Long ACCT_SCHEMA_ID = 9801L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(780000L);

    @Test
    public void testReturnMoveLinkedToSourceDeliveryMove() {
        seedPeriodAndSubjects();
        seedStock("SEED-TRC-001", new BigDecimal("20"), new BigDecimal("5"));
        String deliveryCode = "SD-TRC-001";
        Long[] deliveryCtx = seedApprovedDelivery(deliveryCode, new BigDecimal("10"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        String returnCode = "RT-TRC-001";
        ormTemplate.runInSession(session -> {
            newReturn(returnCode, returnId, deliveryCtx[0]);
            newReturnLine(returnLineId, returnId, deliveryCtx[1], new BigDecimal("4"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpInvStockMove deliveryMove = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, deliveryCode);
        assertNotNull(deliveryMove, "源出库单应已生成出库移动单");
        ErpInvStockMove returnMove = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnCode);
        assertNotNull(returnMove, "应生成退货入库移动单");

        assertEquals(deliveryMove.getId(), returnMove.getOriginReturnedMoveId(),
                "退货移动单 originReturnedMoveId 应指向源出库移动单");

        assertTrue(returnTraceContains(deliveryMove.getId(), returnMove.getId()),
                "returnTrace(源出库移动单) 应返回该退货移动单");
    }

    @Test
    public void testReturnTraceFromReturnMoveReachesOriginal() {
        seedPeriodAndSubjects();
        seedStock("SEED-TRC-002", new BigDecimal("20"), new BigDecimal("5"));
        String deliveryCode = "SD-TRC-002";
        Long[] deliveryCtx = seedApprovedDelivery(deliveryCode, new BigDecimal("10"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        String returnCode = "RT-TRC-002";
        ormTemplate.runInSession(session -> {
            newReturn(returnCode, returnId, deliveryCtx[0]);
            newReturnLine(returnLineId, returnId, deliveryCtx[1], new BigDecimal("3"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");

        ErpInvStockMove deliveryMove = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, deliveryCode);
        ErpInvStockMove returnMove = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnCode);

        // 给定退货移动单 → 其原出库移动单
        assertTrue(returnTraceContains(returnMove.getId(), deliveryMove.getId()),
                "returnTrace(退货移动单) 应返回源出库移动单");
    }

    // ---------- seed ----------

    /**
     * 预置库存 + 审核源出库单（经 {@code ErpSalDelivery__approve} 生成 OUTGOING 移动）。
     * 返回 {deliveryId, deliveryLineId}。
     */
    private Long[] seedApprovedDelivery(String deliveryCode, BigDecimal deliveryQty) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDeliverySubmitted(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        assertEquals(0, approveDelivery(deliveryId).getStatus(), "源出库单审核应成功");
        return new Long[]{deliveryId, deliveryLineId};
    }

    private void seedStock(String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", "INCOMING");
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

    // ---------- rpc ----------

    private ApiResponse<?> approveDelivery(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> approveReturn(Long returnId) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("returnId", returnId)));
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
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType("CUSTOMER");
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
        subject.setStatus("ACTIVE");
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
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long deliveryLineId, BigDecimal qty) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        ErpSalReturnLine line = new ErpSalReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setDeliveryLineId(deliveryLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(qty.multiply(new BigDecimal("5")));
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
