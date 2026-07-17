package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmBundlePricing;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;
import app.erp.crm.dao.entity.ErpCrmConfigRule;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmPriceRule;
import app.erp.crm.dao.entity.ErpCrmProductConfigurator;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPQ 配置→报价生成集成测试（plan 2026-07-07-1430-2 §Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpCrmProductConfigurator__generateQuote}，覆盖：
 * <ol>
 *   <li>bundlePricing 路径：配置规则评估 + 捆绑定价（PERCENTAGE）+ 报价单创建 + lead 回写。</li>
 *   <li>priceRule 路径：价格规则命中 + 报价单创建。</li>
 *   <li>负路径：配置器失效 → ERR_CPQ_CONFIGURATOR_INACTIVE；无价格匹配 → ERR_CPQ_NO_PRICE_MATCHED。</li>
 *   <li>维护钩子：discountType-discountValue 不一致 → ERR_CPQ_DISCOUNT_INCONSISTENT。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmCpqGenerateQuote extends JunitAutoTestCase {

    @RegisterExtension
    static CrmFrozenClockExtension frozenClock = new CrmFrozenClockExtension();

    static final Long ORG_ID = 3301L;
    static final Long CURRENCY_ID = 6401L;
    static final Long CUSTOMER_ID = 3401L;
    static final Long PRODUCT_ID = 3501L;
    static final Long CONFIGURATOR_ID = 3001L;
    static final Long INACTIVE_CONFIGURATOR_ID = 3002L;
    static final Long BUNDLE_ID = 3601L;
    static final Long LEAD_ID = 3701L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testGenerateQuoteViaBundlePricing() {
        seedCommons();
        seedConfigurator(CONFIGURATOR_ID, "CFG-001", true);
        seedConfigRule(3011L, CONFIGURATOR_ID, "REQUIRED", "CPU_TYPE", "INTEL_XEON", "HEATSINK", "HEAVY_DUTY", 10);
        seedBundle(BUNDLE_ID, "PERCENTAGE", BigDecimal.valueOf(15), null);
        seedBundleLine(3611L, BUNDLE_ID, PRODUCT_ID, BigDecimal.valueOf(100000), BigDecimal.valueOf(1));
        seedLead(LEAD_ID, "OPP-CPQ-001", ORG_ID, CUSTOMER_ID);

        Map<String, Object> features = new HashMap<>();
        features.put("CPU_TYPE", "INTEL_XEON");
        // bundle path 也需要 priceRuleContext 提供 currencyId（report 强制非空）
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("currencyId", CURRENCY_ID);

        ApiResponse<?> result = generateQuote(CONFIGURATOR_ID, features, BUNDLE_ID, ctx, LEAD_ID);
        assertEquals(0, result.getStatus(), "bundlePricing 路径 generateQuote 应成功");

        Long quotationId = extractId(result.getData(), "id");
        ErpSalQuotation quotation = daoProvider.daoFor(ErpSalQuotation.class).getEntityById(quotationId);
        assertNotNull(quotation, "报价单已创建");
        assertEquals(CUSTOMER_ID, quotation.getCustomerId(), "报价单 customerId 取自 lead.partnerId");
        assertEquals(0, quotation.getTotalAmount().compareTo(BigDecimal.valueOf(85000.0)),
                "100000 × (1 - 15/100) = 85000");

        ErpCrmLead lead = daoProvider.daoFor(ErpCrmLead.class).getEntityById(LEAD_ID);
        assertEquals(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION, lead.getRelatedBillType(),
                "lead 弱指针 relatedBillType=SALES_QUOTATION");
        assertEquals(quotation.getCode(), lead.getRelatedBillCode(), "lead 弱指针 relatedBillCode=报价单号");
    }

    @Test
    public void testGenerateQuoteViaPriceRule() {
        seedCommons();
        seedConfigurator(CONFIGURATOR_ID, "CFG-002", true);
        seedPriceRule(3801L, "PROMOTIONAL", 1, PRODUCT_ID, null, null, null,
                null, 10.0, null, CURRENCY_ID);
        seedLead(LEAD_ID, "OPP-CPQ-002", ORG_ID, CUSTOMER_ID);

        Map<String, Object> features = new HashMap<>();
        Map<String, Object> priceCtx = new HashMap<>();
        priceCtx.put("productId", PRODUCT_ID);
        priceCtx.put("quantity", BigDecimal.valueOf(5));
        priceCtx.put("basePrice", BigDecimal.valueOf(1000));
        priceCtx.put("currencyId", CURRENCY_ID);

        ApiResponse<?> result = generateQuote(CONFIGURATOR_ID, features, null, priceCtx, LEAD_ID);
        assertEquals(0, result.getStatus(), "priceRule 路径 generateQuote 应成功");

        Long quotationId = extractId(result.getData(), "id");
        ErpSalQuotation quotation = daoProvider.daoFor(ErpSalQuotation.class).getEntityById(quotationId);
        assertNotNull(quotation);
        assertEquals(0, quotation.getTotalAmount().compareTo(BigDecimal.valueOf(900.0)),
                "1000 × (1 - 10/100) = 900");
    }

    @Test
    public void testInactiveConfiguratorRejected() {
        seedCommons();
        seedConfigurator(INACTIVE_CONFIGURATOR_ID, "CFG-INACTIVE", false);
        Map<String, Object> features = new HashMap<>();
        Map<String, Object> priceCtx = new HashMap<>();
        priceCtx.put("productId", PRODUCT_ID);
        priceCtx.put("basePrice", BigDecimal.valueOf(1000));
        priceCtx.put("currencyId", CURRENCY_ID);

        ApiResponse<?> result = generateQuote(INACTIVE_CONFIGURATOR_ID, features, null, priceCtx, null);
        assertEquals(ErpCrmErrors.ERR_CPQ_CONFIGURATOR_INACTIVE.getErrorCode(), result.getCode(),
                "未启用配置器 → ERR_CPQ_CONFIGURATOR_INACTIVE");
    }

    @Test
    public void testNoPriceMatchedRejected() {
        seedCommons();
        seedConfigurator(CONFIGURATOR_ID, "CFG-003", true);
        // 提供 priceRuleContext 但无 basePrice 且无匹配规则
        Map<String, Object> features = new HashMap<>();
        Map<String, Object> priceCtx = new HashMap<>();
        priceCtx.put("productId", PRODUCT_ID);
        priceCtx.put("quantity", BigDecimal.valueOf(5));
        priceCtx.put("currencyId", CURRENCY_ID);

        ApiResponse<?> result = generateQuote(CONFIGURATOR_ID, features, null, priceCtx, null);
        assertEquals(ErpCrmErrors.ERR_CPQ_NO_PRICE_MATCHED.getErrorCode(), result.getCode(),
                "无价格匹配且无 basePrice → ERR_CPQ_NO_PRICE_MATCHED");
    }

    @Test
    public void testNoPriceContextRejected() {
        seedCommons();
        seedConfigurator(CONFIGURATOR_ID, "CFG-004", true);
        Map<String, Object> features = new HashMap<>();

        // 既无 bundlePricingId 也无 priceRuleContext → ERR_CPQ_NO_PRICE_MATCHED
        ApiResponse<?> result = generateQuote(CONFIGURATOR_ID, features, null, null, null);
        assertEquals(ErpCrmErrors.ERR_CPQ_NO_PRICE_MATCHED.getErrorCode(), result.getCode(),
                "无定价上下文 → ERR_CPQ_NO_PRICE_MATCHED");
    }

    @Test
    public void testMaintenanceHookDiscountInconsistent() {
        seedCommons();
        // 经 ErpCrmBundlePricing__save 保存 PERCENTAGE=150 → 应被钩子拒绝
        Map<String, Object> data = new HashMap<>();
        data.put("code", "BAD-BUNDLE");
        data.put("name", "非法折扣");
        data.put("bundleName", "百分比超限");
        data.put("discountType", "PERCENTAGE");
        data.put("discountValue", BigDecimal.valueOf(150));

        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                mutation, "ErpCrmBundlePricing__save", ApiRequest.build(Map.of("data", data)));
        ApiResponse<?> result = graphQLEngine.executeRpc(ctx);
        assertEquals(ErpCrmErrors.ERR_CPQ_DISCOUNT_INCONSISTENT.getErrorCode(), result.getCode(),
                "PERCENTAGE=150 → ERR_CPQ_DISCOUNT_INCONSISTENT");
    }

    @Test
    public void testMaintenanceHookQtyRangeInvalid() {
        seedCommons();
        Map<String, Object> data = new HashMap<>();
        data.put("code", "BAD-PRICE-RULE");
        data.put("name", "数量区间非法");
        data.put("ruleType", "VOLUME");
        data.put("priority", 1);
        data.put("minQuantity", BigDecimal.valueOf(100));
        data.put("maxQuantity", BigDecimal.valueOf(10));

        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                mutation, "ErpCrmPriceRule__save", ApiRequest.build(Map.of("data", data)));
        ApiResponse<?> result = graphQLEngine.executeRpc(ctx);
        assertEquals(ErpCrmErrors.ERR_CPQ_QTY_RANGE_INVALID.getErrorCode(), result.getCode(),
                "min>max → ERR_CPQ_QTY_RANGE_INVALID");
    }

    @Test
    public void testMaintenanceHookEffectiveDateInvalid() {
        seedCommons();
        Map<String, Object> data = new HashMap<>();
        data.put("code", "BAD-DATE");
        data.put("name", "日期非法");
        data.put("discountType", "PERCENTAGE");
        data.put("discountValue", BigDecimal.valueOf(10));
        data.put("effectiveFrom", LocalDate.of(2026, 12, 31));
        data.put("effectiveTo", LocalDate.of(2026, 1, 1));

        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                mutation, "ErpCrmBundlePricing__save", ApiRequest.build(Map.of("data", data)));
        ApiResponse<?> result = graphQLEngine.executeRpc(ctx);
        assertEquals(ErpCrmErrors.ERR_CPQ_EFFECTIVE_DATE_INVALID.getErrorCode(), result.getCode(),
                "effectiveFrom>effectiveTo → ERR_CPQ_EFFECTIVE_DATE_INVALID");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> generateQuote(Long configuratorId, Map<String, Object> features,
                                         Long bundlePricingId, Map<String, Object> priceRuleContext,
                                         Long leadId) {
        Map<String, Object> data = new HashMap<>();
        data.put("configuratorId", configuratorId);
        data.put("selectedFeatures", features);
        if (bundlePricingId != null) {
            data.put("bundlePricingId", bundlePricingId);
        }
        if (priceRuleContext != null) {
            data.put("priceRuleContext", priceRuleContext);
        }
        if (leadId != null) {
            data.put("leadId", leadId);
        }
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                mutation, "ErpCrmProductConfigurator__generateQuote", ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private Long extractId(Object data, String key) {
        Object idVal = ((Map<?, ?>) data).get(key);
        if (idVal instanceof Number) {
            return ((Number) idVal).longValue();
        }
        return Long.valueOf(String.valueOf(idVal));
    }

    // ---------- seed helpers ----------

    private void seedCommons() {
        ormTemplate.runInSession(() -> {
            seedCurrency();
            seedOrganization();
            seedCustomer();
        });
    }

    private void seedCurrency() {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.setId(CURRENCY_ID);
        currency.setCode("CNY");
        currency.setName("人民币");
        dao.saveEntity(currency);
    }

    private void seedOrganization() {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization org = new ErpMdOrganization();
        org.setId(ORG_ID);
        org.setCode("ORG-" + ORG_ID);
        org.setName("CPQ 测试组织");
        org.setOrgType("COMPANY");
        org.setStatus("ACTIVE");
        dao.saveEntity(org);
    }

    private void seedCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-CPQ-001");
        partner.setName("CPQ 测试客户");
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        dao.saveEntity(partner);
    }

    private void seedConfigurator(Long id, String code, boolean active) {
        IEntityDao<ErpCrmProductConfigurator> dao = daoProvider.daoFor(ErpCrmProductConfigurator.class);
        ErpCrmProductConfigurator cfg = new ErpCrmProductConfigurator();
        cfg.setId(id);
        cfg.setCode(code);
        cfg.setName("配置器 " + code);
        cfg.setOrgId(ORG_ID);
        cfg.setProductType("SERVER");
        cfg.setConfigName(code);
        cfg.setIsActive(active);
        cfg.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        cfg.setEffectiveTo(LocalDate.of(2026, 12, 31));
        dao.saveEntity(cfg);
    }

    private void seedConfigRule(Long id, Long configuratorId, String ruleType,
                                String sourceCode, String sourceValue,
                                String targetCode, String targetValue, int sequence) {
        IEntityDao<ErpCrmConfigRule> dao = daoProvider.daoFor(ErpCrmConfigRule.class);
        ErpCrmConfigRule rule = new ErpCrmConfigRule();
        rule.setId(id);
        rule.setConfiguratorId(configuratorId);
        rule.setOrgId(ORG_ID);
        rule.setRuleType(ruleType);
        rule.setSourceFeatureCode(sourceCode);
        rule.setSourceFeatureValue(sourceValue);
        rule.setTargetFeatureCode(targetCode);
        rule.setTargetFeatureValue(targetValue);
        rule.setSequence(sequence);
        dao.saveEntity(rule);
    }

    private void seedBundle(Long id, String discountType, BigDecimal discountValue, BigDecimal bundleAmount) {
        IEntityDao<ErpCrmBundlePricing> dao = daoProvider.daoFor(ErpCrmBundlePricing.class);
        ErpCrmBundlePricing bundle = new ErpCrmBundlePricing();
        bundle.setId(id);
        bundle.setCode("BND-" + id);
        bundle.setName("捆绑 " + id);
        bundle.setOrgId(ORG_ID);
        bundle.setBundleName("CPQ 捆绑");
        bundle.setDiscountType(discountType);
        bundle.setDiscountValue(discountValue);
        bundle.setBundleAmount(bundleAmount);
        bundle.setIsActive(Boolean.TRUE);
        bundle.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        bundle.setEffectiveTo(LocalDate.of(2026, 12, 31));
        dao.saveEntity(bundle);
    }

    private void seedBundleLine(Long id, Long bundleId, Long productId,
                                BigDecimal unitPrice, BigDecimal quantity) {
        IEntityDao<ErpCrmBundlePricingLine> dao = daoProvider.daoFor(ErpCrmBundlePricingLine.class);
        ErpCrmBundlePricingLine line = new ErpCrmBundlePricingLine();
        line.setId(id);
        line.setBundleId(bundleId);
        line.setOrgId(ORG_ID);
        line.setProductId(productId);
        line.setUnitPrice(unitPrice);
        line.setQuantity(quantity);
        line.setSequence(10);
        dao.saveEntity(line);
    }

    private void seedPriceRule(Long id, String ruleType, int priority, Long productId, Long customerId,
                               BigDecimal minQty, BigDecimal maxQty,
                               BigDecimal priceOverride, Double discountPercent,
                               BigDecimal discountAmount, Long currencyId) {
        IEntityDao<ErpCrmPriceRule> dao = daoProvider.daoFor(ErpCrmPriceRule.class);
        ErpCrmPriceRule rule = new ErpCrmPriceRule();
        rule.setId(id);
        rule.setCode("PR-" + id);
        rule.setName("价格规则 " + id);
        rule.setOrgId(ORG_ID);
        rule.setRuleType(ruleType);
        rule.setPriority(priority);
        rule.setProductId(productId);
        rule.setCustomerId(customerId);
        rule.setMinQuantity(minQty);
        rule.setMaxQuantity(maxQty);
        rule.setPriceOverride(priceOverride);
        rule.setDiscountPercent(discountPercent);
        rule.setDiscountAmount(discountAmount);
        rule.setCurrencyId(currencyId);
        rule.setIsActive(Boolean.TRUE);
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setEffectiveTo(LocalDate.of(2026, 12, 31));
        dao.saveEntity(rule);
    }

    private void seedLead(Long id, String code, Long orgId, Long partnerId) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(orgId);
        lead.setPartnerId(partnerId);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_QUALIFIED);
        dao.saveEntity(lead);
    }
}
