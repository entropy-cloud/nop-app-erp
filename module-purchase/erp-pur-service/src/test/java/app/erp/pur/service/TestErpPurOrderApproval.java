package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
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

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 1 服务层集成测试：采购订单三轴审批状态机 + 供应商启用校验。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpPurOrder__submit/approve/reject/withdrawSubmit/reverseApprove/cancel}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。测试自建供应商/行明细后断言状态迁移。
 * 订单审核 = 纯状态推进（state-machine §2「采购订单｜仅状态推进」），不触发库存/凭证。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurOrderApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testOrderSubmitApproveRejectResubmit() {
        ErpPurOrder order = newOrder("PO-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        ApiResponse<?> resp = submit(order.getId());
        assertEquals(0, resp.getStatus(), "提交应成功");
        ErpPurOrder submitted = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "审核应成功");
        ErpPurOrder approved = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核通过 → APPROVED");
        assertEquals("PO-SUBMIT-001", approved.getCode(), "订单审核不触发库存/凭证，仅状态推进");
    }

    @Test
    public void testOrderRejectAndResubmit() {
        ErpPurOrder order = newOrder("PO-REJ-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, reject(order.getId()).getStatus());
        ErpPurOrder rejected = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        assertEquals(0, submit(order.getId()).getStatus());
        ErpPurOrder resubmitted = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testOrderIllegalTransitionRejected() {
        ErpPurOrder order = newOrder("PO-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus());
        ErpPurOrder approved = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        ApiResponse<?> bad = submit(order.getId());
        assertEquals(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可再提交，应返回非法迁移错误");
        bad = withdrawSubmit(order.getId());
        assertEquals(ErpPurErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可撤回提交，应返回非法迁移错误");

        assertEquals(0, reverseApprove(order.getId()).getStatus());
        ErpPurOrder reversed = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核目标态 = REJECTED 非 UNSUBMITTED");
    }

    @Test
    public void testOrderInactiveSupplierRejected() {
        ErpPurOrder order = newOrder("PO-INACTIVE-001");
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        ormTemplate.runInSession(() -> {
            seedSupplier(SUPPLIER_ID, "INACTIVE");
            saveOrderWithLine(order);
        });
        ApiResponse<?> bad = submit(order.getId());
        assertEquals(ErpPurErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "供应商停用 → submit 应返回 ERR_PARTNER_INACTIVE");

        ErpPurOrder submittedOrder = newOrder("PO-INACTIVE-002");
        submittedOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> saveOrderWithLine(submittedOrder));
        bad = approve(submittedOrder.getId());
        assertEquals(ErpPurErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "供应商停用 → approve 也应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testOrderCancelFromDraft() {
        ErpPurOrder order = newOrder("PO-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveOrderWithLine(order);
        });

        assertEquals(0, cancel(order.getId()).getStatus());
        ErpPurOrder cancelled = daoProvider.daoFor(ErpPurOrder.class).getEntityById(order.getId());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(order.getId());
        assertEquals(ErpPurErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废订单不可提交，应返回非法单据状态迁移错误");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__submit", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> withdrawSubmit(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__withdrawSubmit", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__approve", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> reject(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__reject", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> reverseApprove(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__reverseApprove", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> cancel(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__cancel", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurOrder newOrder(String code) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpPurOrder order) {
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);
        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(new BigDecimal("50"));
        lineDao.saveEntity(line);
    }

    private void seedActiveSupplier(Long id) {
        seedSupplier(id, ErpPurConstants.PARTNER_STATUS_ACTIVE);
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
}
