package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 1 服务层集成测试：销售退货审核触发库存反向入库移动（{@code generateMove}，{@code relatedBillType=SAL_RETURN}）
 * + 库存余额增加 + 幂等 + 反审核内部冲销（反向出库移动冲减库存）。
 *
 * <p>前置：预置库存（INCOMING generateMove）+ 源出库单置 APPROVED。覆盖 sales→inventory 端到端。
 * 退货入库方向 = INCOMING（库存增加），与采购退货出库（OUTGOING，库存减少）镜像对称。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnInventory extends JunitAutoTestCase {

    static final Long ORG_ID = 3703L;
    static final Long CUSTOMER_ID = 4703L;
    static final Long WAREHOUSE_ID = 5703L;
    static final Long MATERIAL_ID = 6703L;
    static final Long UOM_ID = 7703L;
    static final Long CURRENCY_ID = 8703L;
    static final Long ACCT_SCHEMA_ID = 9703L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(720000L);

    @Test
    public void testApproveGeneratesIncomingMoveAndStockIncrease() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-INV-001", new BigDecimal("10"));
        seedStock("SEED-INV-001", new BigDecimal("20"), new BigDecimal("5"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-INV-001", returnId, deliveryCtx[0]);
            newReturnLine(returnLineId, returnId, deliveryCtx[1], new BigDecimal("4"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpSalReturn approved = daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(), "审核 → APPROVED");

        ErpInvStockMove move = findReturnMove("RT-INV-001");
        assertNotNull(move, "应生成入库移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, move.getMoveType(), "入库类型");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");

        ErpInvStockBalance balance = findBalance();
        assertNotNull(balance, "应存在库存余额");
        // 预置 20 + 退货入库 4 = 24
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("24")),
                "退货后余额 = 预置 20 + 退货入库 4 = 24");
    }

    @Test
    public void testApproveIdempotent() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-IDM-001", new BigDecimal("10"));
        seedStock("SEED-IDM-001", new BigDecimal("20"), new BigDecimal("5"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-IDM-001", returnId, deliveryCtx[0]);
            newReturnLine(returnLineId, returnId, deliveryCtx[1], new BigDecimal("3"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, approveReturn(returnId).getStatus(), "二次审核幂等空操作");
        assertEquals(1, countReturnMoves("RT-IDM-001"), "幂等：不应产生第二张入库移动单");
    }

    @Test
    public void testReverseApproveRestoresStock() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-REV-001", new BigDecimal("10"));
        seedStock("SEED-REV-001", new BigDecimal("20"), new BigDecimal("5"));

        Long returnId = nextId();
        Long returnLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-REV-001", returnId, deliveryCtx[0]);
            newReturnLine(returnLineId, returnId, deliveryCtx[1], new BigDecimal("4"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, new BigDecimal("24").compareTo(findBalance().getTotalQuantity()), "退货后余额=24");

        ErpInvStockMove original = findReturnMove("RT-REV-001");
        assertEquals(0, countReversals(original.getCode()), "反审核前无冲销单");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核应成功");
        ErpSalReturn reversed = daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(), "反审核 → REJECTED");
        assertEquals(1, countReversals(original.getCode()), "应生成 1 张反向冲销移动单（冲减库存）");

        // 冲销后余额恢复到预置量 20（24 - 4 冲销）
        assertEquals(0, new BigDecimal("20").compareTo(findBalance().getTotalQuantity()),
                "反审核冲销后余额恢复 = 预置 20");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "二次反审核幂等");
        assertEquals(1, countReversals(original.getCode()), "幂等：不产生第二张冲销单");
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

    /**
     * 预置库存：经 GraphQL 调 {@code ErpInvStockMove__generateMove}(INCOMING) 业务联动建余额，
     * 供退货入库后余额断言有基准。
     */
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

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long returnId) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("returnId", returnId)));
    }

    private ApiResponse<?> reverseApproveReturn(Long returnId) {
        return executeRpc(mutation, "ErpSalReturn__reverseApprove", ApiRequest.build(Map.of("returnId", returnId)));
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
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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

    // ---------- query helpers ----------

    private ErpInvStockMove findReturnMove(String returnCode) {
        return findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnCode);
    }

    private long countReturnMoves(String returnCode) {
        return countMoves(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnCode);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countMoves(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return dao.findAllByQuery(q).size();
    }

    private long countReversals(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).size();
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
