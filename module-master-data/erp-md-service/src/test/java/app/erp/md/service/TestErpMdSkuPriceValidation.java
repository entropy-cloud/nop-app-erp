package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialCategory;
import app.erp.md.dao.entity.ErpMdMaterialSku;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主数据业务服务 Phase 2 集成测试（UC-MD-03 价格优先级解析 + UC-MD-04 最低价校验）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpMdMaterialSku__resolvePrice/validatePrice}，
 * 断言三级优先级（手工价 > SPI 价格表 > SKU 默认档）与 OFF/WARN/HARD 校验分派。
 * 价格表层经 {@link TestStubSupplierPriceResolver} 桩模拟（master-data 不反向依赖 purchase）。
 *
 * <p>对应计划 {@code docs/plans/2026-07-07-0024-1} Phase 2。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-supplier-price-resolver.beans.xml")
public class TestErpMdSkuPriceValidation extends JunitAutoTestCase {

    static final String BC_PURCHASE = ErpMdConstants.BILL_TYPE_PURCHASE;
    static final String BC_WHOLESALE = ErpMdConstants.BILL_TYPE_WHOLESALE;
    static final String BC_RETAIL = ErpMdConstants.BILL_TYPE_RETAIL;
    static final String BC_DEFAULT = ErpMdConstants.BILL_TYPE_DEFAULT;
    static final Long PARTNER_1 = 9001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubSupplierPriceResolver stubResolver;

    // ============ UC-MD-03 价格优先级解析 ============

    @Test
    public void testResolvePriceManualWins() {
        Long materialId = seedMaterialAndSku("MAT-PM-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        // 也注入一个 SPI 价格，但手工价应优先
        stubResolver.putPrice(skuId, PARTNER_1, new BigDecimal("8.88"));

        BigDecimal result = resolvePrice(skuId, PARTNER_1, BC_PURCHASE, new BigDecimal("99.99"));
        assertEquals(new BigDecimal("99.99"), result, "手工价 > SPI 价格表");
    }

    @Test
    public void testResolvePriceFromSupplierList() {
        Long materialId = seedMaterialAndSku("MAT-PS-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        // SPI 价格表命中 7.77（低于默认档采购价 10.00）
        stubResolver.putPrice(skuId, PARTNER_1, new BigDecimal("7.77"));

        BigDecimal result = resolvePrice(skuId, PARTNER_1, BC_PURCHASE, null);
        assertEquals(new BigDecimal("7.77"), result, "无手工价时 SPI 价格表层命中应优先于 SKU 默认档");
    }

    @Test
    public void testResolvePriceDefaultTier() {
        Long materialId = seedMaterialAndSku("MAT-PD-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);

        // 无 SPI + 无手工价 → 按 billType 选默认档（SKU 价格 DECIMAL scale=4，存储为 10.0000）
        assertEquals(new BigDecimal("10.0000"), resolvePrice(skuId, null, BC_PURCHASE, null),
                "PURCHASE → purchasePrice");
        assertEquals(new BigDecimal("12.0000"), resolvePrice(skuId, null, BC_WHOLESALE, null),
                "WHOLESALE → wholesalePrice");
        assertEquals(new BigDecimal("18.0000"), resolvePrice(skuId, null, BC_RETAIL, null),
                "RETAIL → retailPrice");
        assertEquals(new BigDecimal("15.0000"), resolvePrice(skuId, null, BC_DEFAULT, null),
                "DEFAULT → salePrice");
    }

    // ============ UC-MD-04 最低价校验 ============

    @Test
    public void testValidatePriceHardReject() {
        // SKU 四档价：purchase=10/wholesale=12/retail=18/sale=15 → 派生底线=10
        Long materialId = seedMaterialAndSku("MAT-VH-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        Long categoryId = seedCategory("CAT-VH-1", ErpMdConstants.PRICE_VALIDATION_HARD);

        // 最终价 9 < 底线 10 + HARD → 抛错
        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validatePrice",
                ApiRequest.build(Map.of(
                        "skuId", skuId,
                        "finalPrice", new BigDecimal("9.00"),
                        "materialCategoryId", categoryId)));
        assertEquals(ErpMdErrors.ERR_PRICE_BELOW_MIN.getErrorCode(), resp.getCode(),
                "HARD 级别低于底线应拒绝");
    }

    @Test
    public void testValidatePriceWarnAllows() {
        Long materialId = seedMaterialAndSku("MAT-VW-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        Long categoryId = seedCategory("CAT-VW-1", ErpMdConstants.PRICE_VALIDATION_WARN);

        // 最终价 9 < 底线 10 + WARN → 放行带警告
        Map<?, ?> result = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__validatePrice",
                Map.of("skuId", skuId, "finalPrice", new BigDecimal("9.00"),
                        "materialCategoryId", categoryId));
        assertEquals(Boolean.TRUE, result.get("passed"), "WARN 应放行");
        assertEquals(Boolean.TRUE, result.get("warning"), "WARN 低于底线应带警告");
        assertEquals(ErpMdConstants.PRICE_VALIDATION_WARN, result.get("level"));
    }

    @Test
    public void testValidatePriceOff() {
        Long materialId = seedMaterialAndSku("MAT-VO-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        Long categoryId = seedCategory("CAT-VO-1", ErpMdConstants.PRICE_VALIDATION_OFF);

        // OFF → 不校验（即使低于底线也直接通过，无警告）
        Map<?, ?> result = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__validatePrice",
                Map.of("skuId", skuId, "finalPrice", new BigDecimal("1.00"),
                        "materialCategoryId", categoryId));
        assertEquals(Boolean.TRUE, result.get("passed"), "OFF 应直接通过");
        assertFalse(Boolean.TRUE.equals(result.get("warning")), "OFF 不应带警告");
        assertEquals(ErpMdConstants.PRICE_VALIDATION_OFF, result.get("level"));
    }

    @Test
    public void testValidatePriceAboveMinNoWarning() {
        // 价格高于底线 → 即使 WARN/HARD 也不警告
        Long materialId = seedMaterialAndSku("MAT-VA-1", new BigDecimal("10.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), new BigDecimal("18.00"));
        Long skuId = skuIdFor(materialId);
        Long categoryId = seedCategory("CAT-VA-1", ErpMdConstants.PRICE_VALIDATION_HARD);

        Map<?, ?> result = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__validatePrice",
                Map.of("skuId", skuId, "finalPrice", new BigDecimal("20.00"),
                        "materialCategoryId", categoryId));
        assertEquals(Boolean.TRUE, result.get("passed"), "高于底线应通过");
        assertFalse(Boolean.TRUE.equals(result.get("warning")), "高于底线不应警告");
    }

    // ---------- helpers ----------

    private BigDecimal resolvePrice(Long skuId, Long partnerId, String billType, BigDecimal manual) {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("skuId", skuId);
        args.put("partnerId", partnerId);
        args.put("billType", billType);
        args.put("manualPrice", manual);
        Object data = rpcData(query, "ErpMdMaterialSku__resolvePrice", args);
        return new BigDecimal(data.toString());
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Object rpcData(GraphQLOperationType opType, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(opType, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功，实际 code=" + resp.getCode());
        return resp.getData();
    }

    private Long skuIdFor(Long materialId) {
        return skuDao().findAllByQuery(buildByMaterial(materialId)).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    private io.nop.api.core.beans.query.QueryBean buildByMaterial(Long materialId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("materialId", materialId));
        return q;
    }

    private Long seedMaterialAndSku(String codePrefix, BigDecimal purchase, BigDecimal sale,
                                    BigDecimal wholesale, BigDecimal retail) {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode("M-" + codePrefix);
        material.setName("物料-" + codePrefix);
        material.setMaterialType("GOODS");
        material.setUoMId(1L);
        material.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);

        ormTemplate.runInSession(() -> {
            materialDao().saveEntity(material);
            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.setMaterialId(material.getId());
            sku.setSkuCode("SKU-" + codePrefix);
            sku.setUoMId(1L);
            sku.setConversionRate(BigDecimal.ONE);
            sku.setIsDefault(true);
            sku.setPurchasePrice(purchase);
            sku.setSalePrice(sale);
            sku.setWholesalePrice(wholesale);
            sku.setRetailPrice(retail);
            skuDao().saveEntity(sku);
        });
        return material.getId();
    }

    private Long seedCategory(String code, String priceValidationLevel) {
        ErpMdMaterialCategory category = new ErpMdMaterialCategory();
        category.setCode(code);
        category.setName("分类-" + code);
        category.setPriceValidationLevel(priceValidationLevel);
        ormTemplate.runInSession(() -> categoryDao().saveEntity(category));
        return category.getId();
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }

    private IEntityDao<ErpMdMaterialCategory> categoryDao() {
        return daoProvider.daoFor(ErpMdMaterialCategory.class);
    }
}
