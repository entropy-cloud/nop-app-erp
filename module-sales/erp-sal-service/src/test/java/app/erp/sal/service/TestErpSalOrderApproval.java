package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
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
 * Phase 1 服务层集成测试：销售订单三轴审批状态机（仅状态推进，不触发库存/凭证）+ 客户启用校验 +
 * 客户信用额度校验（SOFT_WARNING 放行 / HARD_BLOCK 拒绝 / outstanding 口径）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalOrder__submit/approve/reject/withdrawSubmit/reverseApprove}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。测试自建客户/行明细后断言状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2301L;
    static final Long CUSTOMER_ID_2 = 2302L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testOrderSubmitApproveRejectResubmit() {
        ErpSalOrder order = newOrder("SO-SUBMIT-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus(), "提交应成功");
        ErpSalOrder submitted = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, reject(order.getId()).getStatus(), "驳回应成功");
        ErpSalOrder rejected = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        assertEquals(0, submit(order.getId()).getStatus());
        ErpSalOrder resubmitted = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");

        assertEquals(0, approve(order.getId()).getStatus(), "审核应成功");
        ErpSalOrder approved = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核 → APPROVED");
    }

    @Test
    public void testOrderIllegalTransitionRejected() {
        ErpSalOrder order = newOrder("SO-ILL-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "未提交不可直接审核，应返回非法迁移错误");

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, withdrawSubmit(order.getId()).getStatus());
        bad = withdrawSubmit(order.getId());
        assertEquals(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "UNSUBMITTED 不可撤回提交，应返回非法迁移错误");
    }

    @Test
    public void testOrderInactiveCustomerRejected() {
        ErpSalOrder order = newOrder("SO-INACTIVE-001", "100");
        ormTemplate.runInSession(() -> {
            seedCustomer(CUSTOMER_ID, "INACTIVE", null);
            saveOrderWithLine(order, "10");
        });

        ApiResponse<?> bad = submit(order.getId());
        assertEquals(ErpSalErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "客户停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCreditLimitSoftWarningAllows() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        ErpSalOrder order = newOrder("SO-CREDIT-SOFT-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus(), "SOFT_WARNING 超额度应放行");
        ErpSalOrder approved = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "SOFT_WARNING 超额度应放行 → APPROVED");
    }

    @Test
    public void testCreditLimitHardBlockRejects() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder order = newOrder("SO-CREDIT-HARD-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "HARD_BLOCK 超额度应返回 ERR_CREDIT_LIMIT_EXCEEDED");
    }

    @Test
    public void testCreditLimitNullNoCheck() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder order = newOrder("SO-CREDIT-NULL-001", "999999");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus(), "creditLimit=null 不控制应放行");
        ErpSalOrder approved = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "creditLimit=null 不控制 → APPROVED");
    }

    @Test
    public void testOutstandingIncludesApprovedUndeliveredOrders() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        ErpSalOrder orderA = newOrder("SO-OUT-A-001", "60");
        ErpSalOrder orderB = newOrder("SO-OUT-B-001", "50");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(orderA, "10");
            saveOrderWithLine(orderB, "10");
        });

        assertEquals(0, submit(orderA.getId()).getStatus());
        assertEquals(0, approve(orderA.getId()).getStatus(), "A 首单审核不超额度");
        ErpSalOrder approvedA = reload(orderA.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedA.getApproveStatus(),
                "A 首单审核不超额度 → APPROVED");

        assertEquals(0, submit(orderB.getId()).getStatus());
        ApiResponse<?> bad = approve(orderB.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "outstanding 含已审核未出库 A → B 超额度拒绝");

        ormTemplate.runInSession(() -> {
            ErpSalOrder a = daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderA.getId());
            a.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_DELIVERED);
            daoProvider.daoFor(ErpSalOrder.class).updateEntity(a);
        });
        assertEquals(0, approve(orderB.getId()).getStatus(), "A 已发货 → B 通过");
        ErpSalOrder approvedB = reload(orderB.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedB.getApproveStatus(),
                "A 已发货不计入 outstanding → B 通过");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submit", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> withdrawSubmit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__withdrawSubmit", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> reject(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__reject", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers ----------

    private ErpSalOrder reload(Long orderId) {
        return daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
    }

    private ErpSalOrder newOrder(String code, String totalAmountWithTax) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setTotalAmountWithTax(new BigDecimal(totalAmountWithTax));
        order.setTotalAmount(new BigDecimal(totalAmountWithTax));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpSalOrder order, String quantity) {
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal("10"));
        line.setAmount(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        seedCustomer(id, ErpSalConstants.PARTNER_STATUS_ACTIVE, creditLimit);
    }

    private void seedCustomer(Long id, String status, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(status);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }
}
