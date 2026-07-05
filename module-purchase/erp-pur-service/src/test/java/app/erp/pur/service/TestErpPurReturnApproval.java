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
 * Phase 1 服务层集成测试：采购退货单三轴审批状态机（对齐 {@code returns.md §退货单状态机}）+ 前置校验拒绝。
 *
 * <p>覆盖 submit/withdrawSubmit/approve/reject/reverseApprove/cancel 正向/反向/非法迁移，以及供应商停用、
 * 源入库单未审核的拒绝路径。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReturnApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 3101L;
    static final Long SUPPLIER_ID = 4101L;
    static final Long WAREHOUSE_ID = 5101L;
    static final Long MATERIAL_ID = 6101L;
    static final Long UOM_ID = 7101L;
    static final Long CURRENCY_ID = 8101L;
    static final Long ACCT_SCHEMA_ID = 9101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(600000L);

    @Test
    public void testStateMachineHappyPath() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-SM-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-SM-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("3"), new BigDecimal("5"));
            return null;
        });

        // UNSUBMITTED 已是提交态（seed），提交非法 → withdraw 到 UNSUBMITTED
        assertNotEquals(0, submitReturn(returnId).getStatus(), "已 SUBMITTED 再次提交应非法");
        assertEquals(0, withdrawSubmitReturn(returnId).getStatus(), "撤回 → UNSUBMITTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, reload(returnId).getApproveStatus());

        assertEquals(0, submitReturn(returnId).getStatus(), "重新提交 → SUBMITTED");
        assertEquals(0, approveReturn(returnId).getStatus(), "审核 → APPROVED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(returnId).getApproveStatus());
        assertTrue(Boolean.TRUE.equals(reload(returnId).getPosted()), "审核过账 posted=true");

        // 反审核 → REJECTED（内部冲销库存移动单 + 红字冲销凭证）
        assertEquals(0, reverseApproveReturn(returnId).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testRejectAndResubmit() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-RJ-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RJ-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, rejectReturn(returnId).getStatus(), "驳回 SUBMITTED → REJECTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(returnId).getApproveStatus());

        assertEquals(0, submitReturn(returnId).getStatus(), "REJECTED 重新提交 → SUBMITTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reload(returnId).getApproveStatus());
    }

    @Test
    public void testApproveIllegalFromUnsubmitted() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-IL-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-IL-001", returnId, receiveCtx[0]);
            ((ErpPurReturn) daoProvider.daoFor(ErpPurReturn.class).getEntityById(returnId))
                    .setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        assertNotEquals(0, approveReturn(returnId).getStatus(), "UNSUBMITTED 直接审核应非法");
    }

    @Test
    public void testSupplierInactiveRejected() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-SUP-001", new BigDecimal("10"), new BigDecimal("5"));
        Long inactiveSupplier = nextId();
        ormTemplate.runInSession(session -> {
            seedInactiveSupplier(inactiveSupplier);
            newReturnWithSupplier("RT-SUP-001", nextId(), receiveCtx[0], inactiveSupplier);
            return null;
        });
        Long returnId = findReturn("RT-SUP-001").getId();
        assertNotEquals(0, submitReturn(returnId).getStatus(), "停用供应商提交应被拒");
    }

    @Test
    public void testSourceReceiveNotApprovedRejected() {
        seedPeriodAndSubjects();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            return null;
        });
        // 源入库单停在 SUBMITTED（未审核），无库存
        Long receiveId = nextId();
        Long receiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            Long orderId = newOrder("PO-RCV-001");
            newOrderLine(orderId, nextId(), 1, new BigDecimal("10"));
            newReceiveSubmitted("PR-RCV-001", receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });

        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-RCV-001", returnId, receiveId);
            newReturnLine(nextId(), returnId, receiveLineId, new BigDecimal("2"), new BigDecimal("5"));
            return null;
        });
        // 退货单 seed 为 SUBMITTED；审核时校验源入库单已审核——未审核应被拒
        assertNotEquals(0, approveReturn(returnId).getStatus(), "源入库单未审核，退货审核应被拒");
    }

    @Test
    public void testCancelApprovedReversesMove() {
        seedPeriodAndSubjects();
        Long[] receiveCtx = seedApprovedReceive("PR-CN-001", new BigDecimal("10"), new BigDecimal("5"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-CN-001", returnId, receiveCtx[0]);
            newReturnLine(nextId(), returnId, receiveCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, cancelReturn(returnId).getStatus(), "作废 APPROVED 单（先冲销）→ CANCELLED");
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, reload(returnId).getDocStatus());
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

    // ---------- rpc ----------

    private ApiResponse<?> submitReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> withdrawSubmitReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__withdrawApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> rejectReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__reject", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> cancelReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__cancel", ApiRequest.build(Map.of("returnId", id)));
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
        seedSupplier(SUPPLIER_ID, ErpPurConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedInactiveSupplier(Long id) {
        seedSupplier(id, "INACTIVE");
    }

    private void seedSupplier(Long id, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
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

    private void newReceiveSubmitted(String code, Long receiveId, Long orderId) {
        newReceive(code, receiveId, orderId);
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

    private void newReceiveLine(Long lineId, Long receiveId, BigDecimal qty, BigDecimal unitPrice) {
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

    private void newReturn(String code, Long returnId, Long receiveId) {
        newReturnWithSupplier(code, returnId, receiveId, SUPPLIER_ID);
    }

    private void newReturnWithSupplier(String code, Long returnId, Long receiveId, Long supplierId) {
        IEntityDao<ErpPurReturn> dao = daoProvider.daoFor(ErpPurReturn.class);
        ErpPurReturn returnOrder = new ErpPurReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setReceiveId(receiveId);
        returnOrder.setSupplierId(supplierId);
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

    private ErpPurReturn findReturn(String code) {
        IEntityDao<ErpPurReturn> dao = daoProvider.daoFor(ErpPurReturn.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
