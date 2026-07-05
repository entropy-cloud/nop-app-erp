package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IActionAuthChecker;
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
import java.util.Set;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：销售订单三轴审批状态机（仅状态推进，不触发库存/凭证）+ 客户启用校验 +
 * 客户信用额度校验（SOFT_WARNING 放行 / HARD_BLOCK 拒绝 / outstanding 口径）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalOrder__submit/approve/reject/withdrawSubmit/reverseApprove}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。测试自建客户/行明细后断言状态迁移。
 *
 * <p>信用控制 Phase 2 扩展用例：AR 未核销余额纳入 outstanding（含多币种 AR + config-gate 关闭回退）、
 * SPECIAL_APPROVAL 级别权限门控（持专项权限放行 / 未持有抛 ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED）。
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
    static final Long ACCT_SCHEMA_ID = 7301L;

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
        assertTrue(bad.getStatus() != 0, "未提交不可直接审核，应被状态守卫拒绝");

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, withdrawSubmit(order.getId()).getStatus());
        bad = withdrawSubmit(order.getId());
        assertTrue(bad.getStatus() != 0, "UNSUBMITTED 不可撤回提交，应被状态守卫拒绝");
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
    public void testCreditLimitMultiCurrencyFunctionalComparison() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        // 外币订单：原币 50 × 汇率 2 = 本位币 100，恰好占满额度
        ErpSalOrder orderA = newOrderWithRate("SO-CREDIT-FX-A-001", "50", new BigDecimal("2"));
        // 外币订单：原币 10 × 汇率 2 = 本位币 20，叠加 A 后超额度
        ErpSalOrder orderB = newOrderWithRate("SO-CREDIT-FX-B-001", "10", new BigDecimal("2"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(orderA, "10");
            saveOrderWithLine(orderB, "10");
        });

        assertEquals(0, submit(orderA.getId()).getStatus());
        assertEquals(0, approve(orderA.getId()).getStatus(), "外币 A 折算本位币 100 不超额度");

        assertEquals(0, submit(orderB.getId()).getStatus());
        ApiResponse<?> bad = approve(orderB.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "外币 B 折算本位币 20 叠加 A 的 outstanding(100) 超额度(100) 应拒绝");
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

    // ---------- Phase 2：AR 未核销余额纳入 + SPECIAL_APPROVAL 权限门控 ----------

    @Test
    public void testOutstandingIncludesArOpenBalanceHardBlock() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);
        ErpSalOrder order = newOrder("SO-CREDIT-AR-001", "60");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
            // 已开票未收款 AR open 余额 50：纯订单口径占 60（未超 100），加 AR 余额后 110 超 100
            seedArOpenItem(CUSTOMER_ID, "SI-AR-001", new BigDecimal("50"), BigDecimal.ONE);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "AR 未核销余额纳入后 outstanding(订单60+AR50=110) 超额度(100) 应拒绝（纯订单口径会误放行）");
    }

    @Test
    public void testArInclusionConfigGatedOffFallsBackToOrdersOnly() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(false);
        ErpSalOrder order = newOrder("SO-CREDIT-AR-OFF-001", "60");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
            seedArOpenItem(CUSTOMER_ID, "SI-AR-002", new BigDecimal("50"), BigDecimal.ONE);
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus(), "AR 纳入关闭 → 纯订单口径(60)不超额度(100) 放行");
        ErpSalOrder approved = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "AR 纳入关闭 → 回退纯订单口径 → APPROVED");
    }

    @Test
    public void testArMultiCurrencyFunctionalInclusion() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);
        ErpSalOrder order = newOrder("SO-CREDIT-AR-FX-001", "60");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
            // 外币 AR：源币 20 × 汇率 2 = 本位币 40，经 openAmountFunctional 纳入（订单60+AR40=100 恰好占满）
            seedArOpenItem(CUSTOMER_ID, "SI-AR-FX-001", new BigDecimal("20"), new BigDecimal("2"));
        });

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus(), "外币 AR openAmountFunctional(40) 纳入后恰好占满额度 放行");

        enableArInclusion(false);
        ErpSalOrder order2 = newOrder("SO-CREDIT-AR-FX-002", "60");
        ormTemplate.runInSession(() -> {
            saveOrderWithLine(order2, "10");
        });
        // 再审一单 60：纯订单口径已有上一单 60，加本单 60 = 120 超额度
        assertEquals(0, submit(order2.getId()).getStatus());
        ApiResponse<?> bad = approve(order2.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "纯订单 outstanding 累计超额度应拒绝（验证 AR 纳入关闭路径同样生效）");
    }

    @Test
    public void testSpecialApprovalWithPermissionAllows() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL);
        ErpSalOrder order = newOrder("SO-CREDIT-SP-PERM-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        // 持有专项权限的审批人 → 放行
        ApiResponse<?> resp = approveWithPermission(order.getId(),
                ErpSalConstants.PERM_CREDIT_OVER_LIMIT_APPROVE);
        assertEquals(0, resp.getStatus(), "SPECIAL_APPROVAL + 持专项权限 → 超额度放行");
        ErpSalOrder approved = reload(order.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "SPECIAL_APPROVAL + 持专项权限 → APPROVED");
    }

    @Test
    public void testSpecialApprovalWithoutPermissionRejects() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL);
        ErpSalOrder order = newOrder("SO-CREDIT-SP-NOPERM-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        // 无专项权限的审批人 → 抛 ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED
        ApiResponse<?> bad = approveWithPermission(order.getId(), "erp-sal:nonExistentPermission");
        assertEquals(ErpSalErrors.ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED.getErrorCode(), bad.getCode(),
                "SPECIAL_APPROVAL + 无专项权限 → 抛 ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED");
    }

    @Test
    public void testSpecialApprovalNoCheckerRejects() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL);
        ErpSalOrder order = newOrder("SO-CREDIT-SP-NOCHK-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        // 无 actionAuthChecker（auth 系统未启用）→ 无法确认专项权限 → 拒绝
        ApiResponse<?> bad = approveWithAuthChecker(order.getId(), null);
        assertEquals(ErpSalErrors.ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED.getErrorCode(), bad.getCode(),
                "SPECIAL_APPROVAL + 无 auth checker → 拒绝（保守安全默认）");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> withdrawSubmit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__withdrawApproval", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> reject(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__reject", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> approveWithAuthChecker(Long orderId, IActionAuthChecker checker) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpSalOrder__approve",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
        ctx.setActionAuthChecker(checker);
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> approveWithPermission(Long orderId, String grantedPermission) {
        return approveWithAuthChecker(orderId, (permission, ctx) -> grantedPermission != null
                && grantedPermission.equals(permission));
    }

    // ---------- helpers ----------

    private ErpSalOrder reload(Long orderId) {
        return daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
    }

    private ErpSalOrder newOrder(String code, String totalAmountWithTax) {
        return newOrderWithRate(code, totalAmountWithTax, new BigDecimal("1"));
    }

    private ErpSalOrder newOrderWithRate(String code, String totalAmountWithTax, BigDecimal exchangeRate) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(exchangeRate);
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

    private void enableArInclusion(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_INCLUDE_AR, enabled);
    }

    private void seedArOpenItem(Long customerId, String sourceBillCode, BigDecimal openAmountSource,
                                BigDecimal exchangeRate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(ORG_ID);
        item.setAcctSchemaId(ACCT_SCHEMA_ID);
        item.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        item.setPartnerId(customerId);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(LocalDate.of(2026, 6, 15));
        item.setCurrencyId(CURRENCY_ID);
        item.setExchangeRate(exchangeRate);
        BigDecimal functional = openAmountSource.multiply(exchangeRate);
        item.setAmountSource(openAmountSource);
        item.setAmountFunctional(functional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openAmountSource);
        item.setOpenAmountFunctional(functional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
    }
}
