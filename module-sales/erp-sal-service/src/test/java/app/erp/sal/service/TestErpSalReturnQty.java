package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
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
 * Phase 1 服务层集成测试：退货数量上限校验（{@code ReturnQtyValidator}，按出库行聚合已审核退货量）。
 *
 * <p>覆盖：超 出库量 被拒（提示最大可退量）；部分退货放行；同一出库行第二张退货单累计上限。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnQty extends JunitAutoTestCase {

    static final Long ORG_ID = 3602L;
    static final Long CUSTOMER_ID = 4602L;
    static final Long WAREHOUSE_ID = 5602L;
    static final Long MATERIAL_ID = 6602L;
    static final Long UOM_ID = 7602L;
    static final Long CURRENCY_ID = 8602L;
    static final Long ACCT_SCHEMA_ID = 9602L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(710000L);

    @Test
    public void testReturnQtyOverDeliveredRejected() {
        seedPeriodAndSubjects();
        // 出库 10
        Long[] deliveryCtx = seedApprovedDelivery("SD-QTY-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-001", returnId, deliveryCtx[0]);
            // 退货 12 > 出库 10
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("12"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "退货 12 > 出库 10 应被拒");
    }

    @Test
    public void testPartialReturnAllowed() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-QTY-002", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-002", returnId, deliveryCtx[0]);
            // 部分退货 4 ≤ 10 放行
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "部分退货 4 ≤ 出库 10 应放行");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testSecondReturnCumulativeLimit() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-QTY-003", new BigDecimal("10"));
        // 第一张退货单 7（已审核）
        Long return1 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-003A", return1, deliveryCtx[0]);
            newReturnLine(nextId(), return1, deliveryCtx[1], new BigDecimal("7"));
            return null;
        });
        assertEquals(0, approveReturn(return1).getStatus(), "第一张退货 7 ≤ 10 放行");

        // 第二张退货单 5 > 剩余可退 (10 - 7 = 3) → 拒
        Long return2 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-003B", return2, deliveryCtx[0]);
            newReturnLine(nextId(), return2, deliveryCtx[1], new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(return2).getStatus(), "第二张退货 5 > 剩余可退 3 应被拒");
    }

    @Test
    public void testSecondReturnWithinCumulativeLimit() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-QTY-004", new BigDecimal("10"));
        Long return1 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-004A", return1, deliveryCtx[0]);
            newReturnLine(nextId(), return1, deliveryCtx[1], new BigDecimal("7"));
            return null;
        });
        assertEquals(0, approveReturn(return1).getStatus());

        // 第二张退货 3 ≤ 剩余可退 3 → 放行
        Long return2 = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-QTY-004B", return2, deliveryCtx[0]);
            newReturnLine(nextId(), return2, deliveryCtx[1], new BigDecimal("3"));
            return null;
        });
        assertEquals(0, approveReturn(return2).getStatus(), "第二张退货 3 ≤ 剩余可退 3 应放行");
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

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    private ApiResponse<?> approveReturn(Long id) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpSalReturn__approve",
                ApiRequest.build(Map.of("returnId", id)));
        return graphQLEngine.executeRpc(ctx);
    }

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
        returnOrder.setTotalAmount(new BigDecimal("15"));
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
}
