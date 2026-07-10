package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 信用冻结（credit hold）出库审核环节集成测试（plan 2026-07-10-1100-2 Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalDelivery__approve}，验证 {@code erp-sal.credit-check-on-delivery}
 * 门控下 {@link app.erp.sal.service.entity.CreditLimitChecker#checkCreditHold} 的三级策略与向后兼容。
 * 拦截场景在 validateBusinessRulesForApprove 抛错（无需库存预置）；放行场景需走完 doApprove 出库过账（预置库存）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalCreditHoldOnDelivery extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long CUSTOMER_ID = 2601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long MATERIAL_ID = 4601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // 场景 1（HARD_BLOCK 拦截）：creditLimit=10000，已审核未发货订单 8000 + AR open 3000 → available=-1000<0 → 出库审核被拦截
    @Test
    public void testHardBlockHoldsDelivery() {
        setCreditCheckOnDelivery(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);

        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-HOLD-HB-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-HOLD-HB-001", new BigDecimal("3000"), BigDecimal.ONE);
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-HB-001", "SO-HOLD-HB-001");

        ApiResponse<?> bad = approve(deliveryId);
        assertEquals(ErpSalErrors.ERR_CREDIT_HOLD_DELIVERY.getErrorCode(), bad.getCode(),
                "HARD_BLOCK 客户当前已超额 → 出库审核被信用冻结");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reloadDelivery(deliveryId).getApproveStatus(),
                "拦截后状态保持 SUBMITTED");
    }

    // 场景 2（SOFT_WARNING 放行）：同上超额但 level=SOFT_WARNING → 出库审核通过
    @Test
    public void testSoftWarningAllowsDelivery() {
        setCreditCheckOnDelivery(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        enableArInclusion(true);
        seedPostingPrereqs();
        seedStock("SEED-HOLD-SW-001", new BigDecimal("20"));

        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-HOLD-SW-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-HOLD-SW-001", new BigDecimal("3000"), BigDecimal.ONE);
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-SW-001", "SO-HOLD-SW-001");

        assertEquals(0, approve(deliveryId).getStatus(), "SOFT_WARNING 超额度放行（出库审核通过）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloadDelivery(deliveryId).getApproveStatus(),
                "SOFT_WARNING 放行 → APPROVED");
    }

    // 场景 3（SPECIAL_APPROVAL 权限放行）：同上超额但 level=SPECIAL_APPROVAL + 持专项权限 → 出库审核通过
    @Test
    public void testSpecialApprovalWithPermissionAllowsDelivery() {
        setCreditCheckOnDelivery(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL);
        enableArInclusion(true);
        seedPostingPrereqs();
        seedStock("SEED-HOLD-SP-001", new BigDecimal("20"));

        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-HOLD-SP-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-HOLD-SP-001", new BigDecimal("3000"), BigDecimal.ONE);
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-SP-001", "SO-HOLD-SP-001");

        ApiResponse<?> resp = approveWithPermission(deliveryId, ErpSalConstants.PERM_CREDIT_OVER_LIMIT_APPROVE);
        assertEquals(0, resp.getStatus(), "SPECIAL_APPROVAL + 持专项权限 → 超额度放行");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloadDelivery(deliveryId).getApproveStatus(),
                "SPECIAL_APPROVAL + 持专项权限 → APPROVED");
    }

    // 场景 4（config 关闭 → 向后兼容）：超额但 credit-check-on-delivery=false → 即使 HARD_BLOCK 也通过
    @Test
    public void testConfigOffBackwardCompat() {
        setCreditCheckOnDelivery(false);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);
        seedPostingPrereqs();
        seedStock("SEED-HOLD-OFF-001", new BigDecimal("20"));

        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-HOLD-OFF-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-HOLD-OFF-001", new BigDecimal("3000"), BigDecimal.ONE);
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-OFF-001", "SO-HOLD-OFF-001");

        assertEquals(0, approve(deliveryId).getStatus(), "config 关闭 → 即使超额也放行（向后兼容）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloadDelivery(deliveryId).getApproveStatus(),
                "config 关闭 → APPROVED");
    }

    // 场景 5（信用正常）：客户未超额 → 出库审核通过
    @Test
    public void testCreditNormalAllowsDelivery() {
        setCreditCheckOnDelivery(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);
        seedPostingPrereqs();
        seedStock("SEED-HOLD-OK-001", new BigDecimal("20"));

        ormTemplate.runInSession(session -> {
            // creditLimit=100000，outstanding 仅 8000 → available=92000 远未超额
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100000"));
            seedApprovedOrder("SO-HOLD-OK-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-OK-001", "SO-HOLD-OK-001");

        assertEquals(0, approve(deliveryId).getStatus(), "信用正常 → 出库审核通过");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloadDelivery(deliveryId).getApproveStatus(),
                "信用正常 → APPROVED");
    }

    // 场景 6（多币种）：creditLimit 本位币，订单/AR 含外币 → 汇率折算后超额 → 拦截
    @Test
    public void testMultiCurrencyFunctionalHolds() {
        setCreditCheckOnDelivery(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);

        ormTemplate.runInSession(session -> {
            // creditLimit=10000（本位币）；外币订单 4000×2=8000 + 外币 AR 1500×2=3000 → outstanding=11000>10000
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-HOLD-FX-001", CUSTOMER_ID, new BigDecimal("4000"), new BigDecimal("2"));
            seedArOpenItem(CUSTOMER_ID, "SI-HOLD-FX-001", new BigDecimal("1500"), new BigDecimal("2"));
            return null;
        });
        long deliveryId = newSubmittedDelivery("SD-HOLD-FX-001", "SO-HOLD-FX-001");

        ApiResponse<?> bad = approve(deliveryId);
        assertEquals(ErpSalErrors.ERR_CREDIT_HOLD_DELIVERY.getErrorCode(), bad.getCode(),
                "多币种折算本位币后超额 → 出库审核被信用冻结");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reloadDelivery(deliveryId).getApproveStatus(),
                "拦截后状态保持 SUBMITTED");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(deliveryId))));
    }

    private ApiResponse<?> approve(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__approve",
                ApiRequest.build(Map.of("id", String.valueOf(deliveryId))));
    }

    private ApiResponse<?> approveWithPermission(Long deliveryId, String grantedPermission) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpSalDelivery__approve",
                ApiRequest.build(Map.of("id", String.valueOf(deliveryId))));
        ctx.setActionAuthChecker((permission, c) -> grantedPermission != null && grantedPermission.equals(permission));
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- delivery/order builders ----------

    /**
     * 建出库单（含一行，引用 orderCode 对应订单）并提交至 SUBMITTED，返回出库单 ID。
     * 订单需已存在（seedApprovedOrder）。出库单行 quantity=10，需预置库存≥10（仅放行场景）。
     */
    private long newSubmittedDelivery(String deliveryCode, String orderCode) {
        Long orderId = findOrderId(orderCode);
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
            ErpSalDelivery delivery = new ErpSalDelivery();
            delivery.setCode(deliveryCode);
            delivery.setOrgId(ORG_ID);
            delivery.setOrderId(orderId);
            delivery.setCustomerId(CUSTOMER_ID);
            delivery.setWarehouseId(WAREHOUSE_ID);
            delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
            delivery.setCurrencyId(CURRENCY_ID);
            delivery.setExchangeRate(new BigDecimal("1"));
            delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
            delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
            delivery.setPosted(false);
            dao.saveEntity(delivery);

            IEntityDao<ErpSalDeliveryLine> lineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
            ErpSalDeliveryLine line = new ErpSalDeliveryLine();
            line.setDeliveryId(delivery.getId());
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("10"));
            line.setUnitPrice(new BigDecimal("5"));
            lineDao.saveEntity(line);
            return null;
        });
        Long deliveryId = findDeliveryId(deliveryCode);
        // 提交 → SUBMITTED（submit 校验客户启用 + 行非空）
        ApiResponse<?> submitResp = submit(deliveryId);
        assertTrue(submitResp.getStatus() == 0, "出库单提交应成功：" + submitResp.getCode());
        return deliveryId;
    }

    // ---------- seed helpers ----------

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void seedApprovedOrder(String code, Long customerId, BigDecimal totalAmountWithTax, BigDecimal exchangeRate) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(customerId);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(exchangeRate);
        order.setTotalAmountWithTax(totalAmountWithTax);
        order.setTotalAmount(totalAmountWithTax);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        dao.saveEntity(order);
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

    /** 出库过账前置：账套 + 开账期间 + SALES_OUTPUT 科目（借主营业务成本/贷库存商品）。仅放行场景需要。 */
    private void seedPostingPrereqs() {
        ormTemplate.runInSession(session -> {
            IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> pDao =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
            app.erp.fin.dao.entity.ErpFinAccountingPeriod period = new app.erp.fin.dao.entity.ErpFinAccountingPeriod();
            period.setCode("2026-07");
            period.setName("2026-07");
            period.setOrgId(ORG_ID);
            period.setYear(2026);
            period.setMonth(7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.setStatus("OPEN");
            pDao.saveEntity(period);

            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");

            IEntityDao<ErpMdAcctSchema> schemaDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema schema = new ErpMdAcctSchema();
            schema.setId(ACCT_SCHEMA_ID);
            schema.setCode("AS-" + ORG_ID);
            schema.setName("账套" + ORG_ID);
            schema.setOrgId(ORG_ID);
            schema.setNature("FINANCIAL");
            schema.setFunctionalCurrencyId(CURRENCY_ID);
            schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
            schemaDao.saveEntity(schema);
            return null;
        });
    }

    private void seedSubject(String code, String name) {
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        app.erp.md.dao.entity.ErpMdSubject subject = new app.erp.md.dao.entity.ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(subject);
    }

    /** 预置库存：经 GraphQL 调 ErpInvStockMove__generateMove(INCOMING) 建余额（avgCost 就位）。 */
    private void seedStock(String billCode, BigDecimal qty) {
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
        line.put("unitCost", new BigDecimal("5"));
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertTrue(resp.getStatus() == 0, "seedStock generateMove 应成功： " + resp.getCode());
    }

    // ---------- config helpers ----------

    private void setCreditCheckOnDelivery(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_ON_DELIVERY, enabled);
    }

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }

    private void enableArInclusion(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_INCLUDE_AR, enabled);
    }

    // ---------- query helpers ----------

    private ErpSalDelivery reloadDelivery(Long deliveryId) {
        return daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
    }

    private Long findDeliveryId(String code) {
        return daoProvider.daoFor(ErpSalDelivery.class).findAllByQuery(new io.nop.api.core.beans.query.QueryBean())
                .stream().filter(d -> code.equals(d.getCode())).map(ErpSalDelivery::getId).findFirst().orElse(null);
    }

    private Long findOrderId(String code) {
        return daoProvider.daoFor(ErpSalOrder.class).findAllByQuery(new io.nop.api.core.beans.query.QueryBean())
                .stream().filter(o -> code.equals(o.getCode())).map(ErpSalOrder::getId).findFirst().orElse(null);
    }
}
