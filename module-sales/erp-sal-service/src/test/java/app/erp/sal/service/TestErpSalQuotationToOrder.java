package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：报价单审核/客户确认状态机 + 报价→订单转化（幂等/回链/字段映射）+
 * 转化产物与 Phase 1 订单审核（信用额度）衔接。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalQuotation__submit/approve/confirmCustomerAccepted/convertToOrder}
 * 与 {@code ErpSalOrder__submit/approve}，引擎负责建 session/事务/管道。测试自建客户/报价/行后断言状态迁移与转化产物。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalQuotationToOrder extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2401L;
    static final Long CUSTOMER_ID_2 = 2402L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testQuotationSubmitApproveConfirmConvert() {
        ErpSalQuotation quotation = newQuotation("SQ-CONV-001", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveQuotationWithLine(quotation, "10", "10");
        });

        assertEquals(0, submit(quotation.getId()).getStatus(), "报价提交应成功");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED,
                reloadQuotation(quotation.getId()).getApproveStatus(), "报价 → SUBMITTED");

        assertEquals(0, approve(quotation.getId()).getStatus(), "报价审核应成功");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED,
                reloadQuotation(quotation.getId()).getApproveStatus(), "报价 → APPROVED");

        assertEquals(0, confirmCustomerAccepted(quotation.getId()).getStatus(), "客户确认应成功");
        assertTrue(Boolean.TRUE.equals(reloadQuotation(quotation.getId()).getIsAccepted()),
                "客户确认 → isAccepted=true");

        ApiResponse<?> converted = convertToOrder(quotation.getId());
        assertEquals(0, converted.getStatus(), "转化应成功");
        Long orderId = extractOrderId(converted);

        ErpSalOrder order = reloadOrder(orderId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, order.getApproveStatus(),
                "转化产物订单审核状态 = UNSUBMITTED");
        assertEquals(ErpSalConstants.DOC_STATUS_DRAFT, order.getDocStatus(),
                "转化产物订单单据状态 = DRAFT");
        assertEquals(quotation.getId(), order.getQuotationId(), "订单回链 quotationId");
        assertEquals(CUSTOMER_ID, order.getCustomerId(), "订单复制 customerId");
        assertEquals(CURRENCY_ID, order.getCurrencyId(), "订单复制 currencyId");
        assertEquals(0, new BigDecimal("1").compareTo(order.getExchangeRate()), "订单复制 exchangeRate");
        assertEquals(0, new BigDecimal("100").compareTo(order.getTotalAmountWithTax()),
                "订单复制 totalAmountWithTax");
        assertEquals(CoreMetrics.currentDate(), order.getBusinessDate(),
                "转化产物订单 businessDate = today");

        List<ErpSalOrderLine> orderLines = loadOrderLines(orderId);
        assertEquals(1, orderLines.size(), "转化产物订单行数 = 报价行数");
        ErpSalOrderLine ol = orderLines.get(0);
        assertEquals(MATERIAL_ID, ol.getMaterialId(), "订单行复制 materialId");
        assertEquals(UOM_ID, ol.getUoMId(), "订单行复制 uoMId");
        assertEquals(0, new BigDecimal("10").compareTo(ol.getQuantity()), "订单行复制 quantity");
        assertEquals(0, new BigDecimal("10").compareTo(ol.getUnitPrice()), "订单行复制 unitPrice");
        assertEquals(0, new BigDecimal("100").compareTo(ol.getAmount()), "订单行复制 amount");
        assertNull(ol.getSkuId(), "ErpSalQuotationLine 无 skuId 列 → 订单行 skuId 留空（不复制）");

        assertTrue(Boolean.TRUE.equals(reloadQuotation(quotation.getId()).getIsAccepted()),
                "转化成功后 isAccepted 保持 true");
    }

    @Test
    public void testConvertNotReadyRejected() {
        ErpSalQuotation notApproved = newQuotation("SQ-NOT-READY-001", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveQuotationWithLine(notApproved, "10", "10");
        });

        assertEquals(0, submit(notApproved.getId()).getStatus());
        ApiResponse<?> bad = convertToOrder(notApproved.getId());
        assertEquals(ErpSalErrors.ERR_QUOTATION_NOT_READY.getErrorCode(), bad.getCode(),
                "未 APPROVED 不可转化 → ERR_QUOTATION_NOT_READY");

        ErpSalQuotation approvedNotAccepted = newQuotation("SQ-NOT-READY-002", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> saveQuotationWithLine(approvedNotAccepted, "10", "10"));
        assertEquals(0, submit(approvedNotAccepted.getId()).getStatus());
        assertEquals(0, approve(approvedNotAccepted.getId()).getStatus());
        ApiResponse<?> bad2 = convertToOrder(approvedNotAccepted.getId());
        assertEquals(ErpSalErrors.ERR_QUOTATION_NOT_READY.getErrorCode(), bad2.getCode(),
                "APPROVED 但未客户确认(isAccepted=false) 不可转化 → ERR_QUOTATION_NOT_READY");
    }

    @Test
    public void testConvertExpiredRejected() {
        ErpSalQuotation expired = newQuotation("SQ-EXPIRED-001", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), CoreMetrics.currentDate().minusDays(1));
        expired.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        expired.setIsAccepted(true);
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveQuotationWithLine(expired, "10", "10");
        });

        ApiResponse<?> bad = convertToOrder(expired.getId());
        assertEquals(ErpSalErrors.ERR_QUOTATION_EXPIRED.getErrorCode(), bad.getCode(),
                "validTo < today → ERR_QUOTATION_EXPIRED");
    }

    @Test
    public void testConvertIdempotentRejected() {
        ErpSalQuotation quotation = newQuotation("SQ-IDEMP-001", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveQuotationWithLine(quotation, "10", "10");
        });
        fullApproveAndConfirm(quotation.getId());

        ApiResponse<?> first = convertToOrder(quotation.getId());
        assertEquals(0, first.getStatus(), "首次转化应成功");
        Long orderId = extractOrderId(first);

        ApiResponse<?> second = convertToOrder(quotation.getId());
        assertEquals(ErpSalErrors.ERR_QUOTATION_ALREADY_CONVERTED.getErrorCode(), second.getCode(),
                "重复转化 → ERR_QUOTATION_ALREADY_CONVERTED");

        ormTemplate.runInSession(() -> {
            ErpSalOrder o = daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
            o.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
            daoProvider.daoFor(ErpSalOrder.class).updateEntity(o);
        });

        ApiResponse<?> reconversion = convertToOrder(quotation.getId());
        assertEquals(0, reconversion.getStatus(), "原订单作废后可重新转化");
    }

    @Test
    public void testConvertedOrderThenCreditCheckAndApprove() {
        ErpSalQuotation quotationSoft = newQuotation("SQ-SOFT-001", CUSTOMER_ID, "150",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveQuotationWithLine(quotationSoft, "10", "10");
        });
        fullApproveAndConfirm(quotationSoft.getId());

        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        Long softOrderId = convertAndExtractId(quotationSoft.getId());
        assertEquals(0, orderSubmit(softOrderId).getStatus());
        assertEquals(0, orderApprove(softOrderId).getStatus(), "SOFT_WARNING 超额度应放行转化产物订单");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED,
                reloadOrder(softOrderId).getApproveStatus(), "转化产物订单 → APPROVED (SOFT_WARNING)");

        ErpSalQuotation quotationHard = newQuotation("SQ-HARD-001", CUSTOMER_ID_2, "150",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID_2, new BigDecimal("100"));
            saveQuotationWithLine(quotationHard, "10", "10");
        });
        fullApproveAndConfirm(quotationHard.getId());

        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        Long hardOrderId = convertAndExtractId(quotationHard.getId());
        assertEquals(0, orderSubmit(hardOrderId).getStatus());
        ApiResponse<?> bad = orderApprove(hardOrderId);
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "HARD_BLOCK 超额度拒绝转化产物订单 → ERR_CREDIT_LIMIT_EXCEEDED");
    }

    @Test
    public void testQuotationToOrderToEnd() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        ErpSalQuotation quotation = newQuotation("SQ-E2E-001", CUSTOMER_ID, "100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("1000"));
            saveQuotationWithLine(quotation, "10", "10");
        });

        fullApproveAndConfirm(quotation.getId());
        assertTrue(Boolean.TRUE.equals(reloadQuotation(quotation.getId()).getIsAccepted()),
                "客户确认 → isAccepted=true");

        Long orderId = convertAndExtractId(quotation.getId());
        ErpSalOrder order = reloadOrder(orderId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, order.getApproveStatus(),
                "转化产物订单 = UNSUBMITTED");
        assertEquals(ErpSalConstants.DOC_STATUS_DRAFT, order.getDocStatus(), "转化产物订单 = DRAFT");

        assertEquals(0, orderSubmit(orderId).getStatus(), "订单提交应成功");
        assertEquals(0, orderApprove(orderId).getStatus(), "订单审核应成功（信用额度校验通过）");
        ErpSalOrder approved = reloadOrder(orderId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "订单 → APPROVED");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED, approved.getDeliveryStatus(),
                "订单审核 = 纯状态推进，不触发出库（deliveryStatus 保持 UNDELIVERED）");
        assertEquals(false, approved.getPosted(), "订单审核不触发过账（posted 保持 false）");

        assertEquals(0, orderCancel(orderId).getStatus(), "订单作废应成功");
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, reloadOrder(orderId).getDocStatus(),
                "订单 → CANCELLED");

        ApiResponse<?> reconversion = convertToOrder(quotation.getId());
        assertEquals(0, reconversion.getStatus(), "原订单作废后可重新转化");
    }

    // ---------- flow helpers ----------

    private void fullApproveAndConfirm(Long quotationId) {
        assertEquals(0, submit(quotationId).getStatus());
        assertEquals(0, approve(quotationId).getStatus());
        assertEquals(0, confirmCustomerAccepted(quotationId).getStatus());
    }

    private Long convertAndExtractId(Long quotationId) {
        ApiResponse<?> r = convertToOrder(quotationId);
        assertEquals(0, r.getStatus(), "转化应成功");
        return extractOrderId(r);
    }

    /**
     * 从转化响应提取订单 ID。GraphQL 会将 Long ID 序列化为 String（避免 JS 精度丢失），需兼容 Number/String。
     */
    private Long extractOrderId(ApiResponse<?> response) {
        Object idVal = ((Map<?, ?>) response.getData()).get("id");
        if (idVal instanceof Number) {
            return ((Number) idVal).longValue();
        }
        return Long.valueOf(String.valueOf(idVal));
    }

    // ---------- rpc helpers (quotation) ----------

    private ApiResponse<?> submit(Long quotationId) {
        return executeRpc(mutation, "ErpSalQuotation__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(quotationId))));
    }

    private ApiResponse<?> approve(Long quotationId) {
        return executeRpc(mutation, "ErpSalQuotation__approve", ApiRequest.build(Map.of("id", String.valueOf(quotationId))));
    }

    private ApiResponse<?> confirmCustomerAccepted(Long quotationId) {
        return executeRpc(mutation, "ErpSalQuotation__confirmCustomerAccepted",
                ApiRequest.build(Map.of("quotationId", quotationId)));
    }

    private ApiResponse<?> convertToOrder(Long quotationId) {
        return executeRpc(mutation, "ErpSalQuotation__convertToOrder",
                ApiRequest.build(Map.of("quotationId", quotationId)));
    }

    // ---------- rpc helpers (order) ----------

    private ApiResponse<?> orderSubmit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> orderApprove(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> orderCancel(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__cancel", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private ErpSalQuotation newQuotation(String code, Long customerId, String totalAmountWithTax,
                                         LocalDate businessDate, LocalDate validTo) {
        ErpSalQuotation quotation = new ErpSalQuotation();
        quotation.setCode(code);
        quotation.setOrgId(ORG_ID);
        quotation.setCustomerId(customerId);
        quotation.setBusinessDate(businessDate);
        quotation.setValidTo(validTo);
        quotation.setCurrencyId(CURRENCY_ID);
        quotation.setExchangeRate(new BigDecimal("1"));
        quotation.setTotalAmount(new BigDecimal(totalAmountWithTax));
        quotation.setTotalTaxAmount(BigDecimal.ZERO);
        quotation.setTotalAmountWithTax(new BigDecimal(totalAmountWithTax));
        quotation.setIsAccepted(false);
        quotation.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        return quotation;
    }

    private void saveQuotationWithLine(ErpSalQuotation quotation, String quantity, String unitPrice) {
        daoProvider.daoFor(ErpSalQuotation.class).saveEntity(quotation);
        IEntityDao<ErpSalQuotationLine> lineDao = daoProvider.daoFor(ErpSalQuotationLine.class);
        ErpSalQuotationLine line = new ErpSalQuotationLine();
        line.setQuotationId(quotation.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setAmount(new BigDecimal("100"));
        line.setAmountWithTax(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

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

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }

    // ---------- reload helpers ----------

    private ErpSalQuotation reloadQuotation(Long quotationId) {
        return daoProvider.daoFor(ErpSalQuotation.class).getEntityById(quotationId);
    }

    private ErpSalOrder reloadOrder(Long orderId) {
        return daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
    }

    private List<ErpSalOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return dao.findAllByQuery(q);
    }
}
