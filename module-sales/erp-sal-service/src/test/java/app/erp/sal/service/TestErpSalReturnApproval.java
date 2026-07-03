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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：销售退货单三轴审批状态机（对齐 {@code returns.md §退货单状态机}）+ 前置校验拒绝。
 *
 * <p>覆盖 submit/withdrawSubmit/approve/reject/reverseApprove/cancel 正向/反向/非法迁移，以及客户停用、
 * 源出库单未审核的拒绝路径。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 3501L;
    static final Long CUSTOMER_ID = 4501L;
    static final Long WAREHOUSE_ID = 5501L;
    static final Long MATERIAL_ID = 6501L;
    static final Long UOM_ID = 7501L;
    static final Long CURRENCY_ID = 8501L;
    static final Long ACCT_SCHEMA_ID = 9501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(700000L);

    @Test
    public void testStateMachineHappyPath() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-SM-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-SM-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("3"));
            return null;
        });

        // UNSUBMITTED 已是提交态（seed），提交非法 → withdraw 到 UNSUBMITTED
        assertNotEquals(0, submitReturn(returnId).getStatus(), "已 SUBMITTED 再次提交应非法");
        assertEquals(0, withdrawSubmitReturn(returnId).getStatus(), "撤回 → UNSUBMITTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, reload(returnId).getApproveStatus());

        assertEquals(0, submitReturn(returnId).getStatus(), "重新提交 → SUBMITTED");
        assertEquals(0, approveReturn(returnId).getStatus(), "审核 → APPROVED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(returnId).getApproveStatus());

        // 反审核 → REJECTED（内部冲销库存入库移动单）
        assertEquals(0, reverseApproveReturn(returnId).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testRejectAndResubmit() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-RJ-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RJ-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("2"));
            return null;
        });

        assertEquals(0, rejectReturn(returnId).getStatus(), "驳回 SUBMITTED → REJECTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());

        assertEquals(0, submitReturn(returnId).getStatus(), "REJECTED 重新提交 → SUBMITTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testApproveIllegalFromUnsubmitted() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-IL-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-IL-001", returnId, deliveryCtx[0]);
            ((ErpSalReturn) daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId))
                    .setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("2"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "UNSUBMITTED 直接审核应非法");
    }

    @Test
    public void testCustomerInactiveRejected() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-CUS-001", new BigDecimal("10"));
        Long inactiveCustomer = nextId();
        ormTemplate.runInSession(session -> {
            seedInactiveCustomer(inactiveCustomer);
            newReturnWithCustomer("RT-CUS-001", nextId(), deliveryCtx[0], inactiveCustomer);
            return null;
        });
        Long returnId = findReturn("RT-CUS-001").getId();
        assertNotEquals(0, submitReturn(returnId).getStatus(), "停用客户提交应被拒");
    }

    @Test
    public void testSourceDeliveryNotApprovedRejected() {
        seedPeriodAndSubjects();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            return null;
        });
        // 源出库单停在 SUBMITTED（未审核）
        Long deliveryId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            Long orderId = newOrder("SO-DLV-001");
            newOrderLine(orderId, nextId(), 1, new BigDecimal("10"));
            newDeliverySubmitted("SD-DLV-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, new BigDecimal("10"));
            return null;
        });

        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-DLV-001", returnId, deliveryId);
            newReturnLine(nextId(), returnId, deliveryLineId, new BigDecimal("2"));
            return null;
        });
        // 退货单 seed 为 SUBMITTED；审核时校验源出库单已审核——未审核应被拒
        assertNotEquals(0, approveReturn(returnId).getStatus(), "源出库单未审核，退货审核应被拒");
    }

    @Test
    public void testCancelApprovedReversesMove() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-CN-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-CN-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, cancelReturn(returnId).getStatus(), "作废 APPROVED 单（先冲销）→ CANCELLED");
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, reload(returnId).getDocStatus());
    }

    @Test
    public void testReasonRequiredRejected() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-RSN-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RSN-001", returnId, deliveryCtx[0]);
            // 退货行不填 reason（默认配置 erp-sal.return-reason-required=true）
            newReturnLineNoReason(nextId(), returnId, deliveryCtx[1], new BigDecimal("2"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "退货原因必填缺失应被拒");
    }

    // ---------- end-to-end seed ----------

    /**
     * 源出库单直接置 APPROVED（不经过出库移动，简化状态机/数量测试的库存依赖）。
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
            newDeliveryApproved(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId};
    }

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    // ---------- rpc ----------

    private ApiResponse<?> submitReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__submit", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> withdrawSubmitReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__withdrawSubmit", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> rejectReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__reject", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__reverseApprove", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> cancelReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__cancel", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed ----------

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
        seedCustomer(CUSTOMER_ID, ErpSalConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedInactiveCustomer(Long id) {
        seedCustomer(id, "INACTIVE");
    }

    private void seedCustomer(Long id, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(status);
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

    private Long newOrder(String code) {
        Long orderId = nextId();
        newOrderWithId(code, orderId);
        return orderId;
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

    private void newDeliveryLine(Long lineId, Long deliveryId, BigDecimal qty) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setId(lineId);
        line.setDeliveryId(deliveryId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        dao.saveEntity(line);
    }

    private void newReturn(String code, Long returnId, Long deliveryId) {
        newReturnWithCustomer(code, returnId, deliveryId, CUSTOMER_ID);
    }

    private void newReturnWithCustomer(String code, Long returnId, Long deliveryId, Long customerId) {
        IEntityDao<ErpSalReturn> dao = daoProvider.daoFor(ErpSalReturn.class);
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setDeliveryId(deliveryId);
        returnOrder.setCustomerId(customerId);
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

    private void newReturnLineNoReason(Long lineId, Long returnId, Long deliveryLineId, BigDecimal qty) {
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
        dao.saveEntity(line);
    }

    private ErpSalReturn findReturn(String code) {
        IEntityDao<ErpSalReturn> dao = daoProvider.daoFor(ErpSalReturn.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
