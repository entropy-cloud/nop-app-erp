package app.erp.pur.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.inject.Inject;

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
import org.junit.jupiter.api.Test;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.service.ErpMdConstants;
import app.erp.pur.dao.entity.ErpPurSupplierPriceList;
import app.erp.pur.service.support.ErpPurSupplierPriceResolver;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 供应商价格清单解析器测试（P1-RC-063 生产实现，plan 2026-08-15-0320-2 Phase 3）。
 *
 * <p>①-⑥ 为 {@link ErpPurSupplierPriceResolver} SPI 单测（经真实 purchase beans.xml 注册注入，
 * H2 落库 seed ErpPurSupplierPriceList）；集成用例经 IGraphQLEngine 调
 * {@code ErpMdMaterialSku__resolvePrice}，断言 supplier 价格表层运行时命中（非默认档）——
 * 证实 {@code supplierPriceResolver} 经类型注入非 null 且三级链 supplier 分支成立。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurSupplierPriceResolver extends JunitAutoTestCase {

    @Inject
    ErpPurSupplierPriceResolver resolver;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ============ ① 命中：supplier + material + active + 效期内 → unitPrice ============

    @Test
    public void testResolveHitActiveWithinPeriod() {
        Long materialId = 9001L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"),
                null, true, null, null);

        BigDecimal price = resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L);
        assertNotNull(price, "supplier+material+active+效期开放应命中");
        assertEquals(0, new BigDecimal("7.77").compareTo(price), "命中应返回协议单价 unitPrice");
    }

    // ============ ② 无命中返回 null：supplier 不符 / material 不符 / inactive / 效期外 ============

    @Test
    public void testResolveNoHitSupplierMismatch() {
        Long materialId = 9011L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"),
                null, true, null, null);
        assertNull(resolver.resolveSupplierPrice(newSku(materialId, 1L), 999L),
                "supplierId 不符 → null");
    }

    @Test
    public void testResolveNoHitMaterialMismatch() {
        seedPriceList(100L, 9012L, 1L, new BigDecimal("7.77"), null, true, null, null);
        assertNull(resolver.resolveSupplierPrice(newSku(9099L, 1L), 100L),
                "materialId 不符 → null");
    }

    @Test
    public void testResolveNoHitInactive() {
        Long materialId = 9013L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"),
                null, false, null, null);
        assertNull(resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L),
                "isActive=false → null");
    }

    @Test
    public void testResolveNoHitOutsidePeriod() {
        Long materialId = 9014L;
        LocalDate today = LocalDate.now();
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"),
                null, true, today.plusDays(1), null);
        assertNull(resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L),
                "validFrom 在未来 → null");
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"),
                null, true, null, today.minusDays(1));
        assertNull(resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L),
                "validTo 已过期 → null");
    }

    // ============ ③ 多条命中 priority 裁决：数字小优先 + 同 priority unitPrice 低者 ============

    @Test
    public void testResolvePrioritySmallWins() {
        Long materialId = 9021L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("9.00"), 100, true, null, null);
        seedPriceList(100L, materialId, 1L, new BigDecimal("8.00"), 10, true, null, null);

        BigDecimal price = resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L);
        assertEquals(0, new BigDecimal("8.00").compareTo(price),
                "priority 数字小优先（10 < 100 → 8.00）");
    }

    @Test
    public void testResolveSamePriorityLowerUnitPriceWins() {
        Long materialId = 9022L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("9.00"), 10, true, null, null);
        seedPriceList(100L, materialId, 1L, new BigDecimal("8.50"), 10, true, null, null);

        BigDecimal price = resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L);
        assertEquals(0, new BigDecimal("8.50").compareTo(price),
                "同 priority 时 unitPrice 低者优先（采购保守语义）");
    }

    // ============ ④ 效期边界：validFrom/validTo null 开放 + 当日命中 + 末日命中 ============

    @Test
    public void testResolvePeriodBoundaries() {
        Long materialId = 9031L;
        LocalDate today = LocalDate.now();
        // 两端开放（null/null）→ 命中
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.00"), null, true, null, null);
        // validFrom=today（当日生效）→ 命中
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.10"), null, true, today, null);
        // validTo=today（末日命中）→ 命中
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.20"), null, true, null, today);

        assertEquals(0, new BigDecimal("7.00").compareTo(
                        resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L)),
                "同 priority 低者优先裁决——三行均命中时取 unitPrice 低者 7.00");
    }

    @Test
    public void testResolveNullPeriodOpenHit() {
        Long materialId = 9032L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("6.60"), null, true, null, null);
        BigDecimal price = resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L);
        assertNotNull(price, "validFrom/validTo 均 null = 开放边界应命中");
        assertEquals(0, new BigDecimal("6.60").compareTo(price));
    }

    @Test
    public void testResolveLastDayHit() {
        Long materialId = 9033L;
        LocalDate today = LocalDate.now();
        seedPriceList(100L, materialId, 1L, new BigDecimal("6.70"),
                null, true, today.minusDays(1), today);
        assertEquals(0, new BigDecimal("6.70").compareTo(
                        resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L)),
                "validTo=today 末日命中");
    }

    // ============ ⑤ 防御：sku null / partnerId null → null ============

    @Test
    public void testResolveDefensiveNulls() {
        Long materialId = 9041L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.77"), null, true, null, null);
        assertNull(resolver.resolveSupplierPrice(null, 100L), "sku null → null");
        assertNull(resolver.resolveSupplierPrice(newSku(materialId, 1L), null), "partnerId null → null");
        assertNull(resolver.resolveSupplierPrice(newSku(null, 1L), 100L), "sku.materialId null → null");
    }

    // ============ ⑥ 单位匹配（U1）：同 material 不同 uoMId 行，按 sku.uoMId 精确命中 ============

    @Test
    public void testResolveUomExactMatch() {
        Long materialId = 9051L;
        seedPriceList(100L, materialId, 1L, new BigDecimal("7.00"), null, true, null, null);
        seedPriceList(100L, materialId, 2L, new BigDecimal("6.00"), null, true, null, null);

        assertEquals(0, new BigDecimal("7.00").compareTo(
                        resolver.resolveSupplierPrice(newSku(materialId, 1L), 100L)),
                "uoMId=1 精确命中 7.00（而非 6.00）");
        assertEquals(0, new BigDecimal("6.00").compareTo(
                        resolver.resolveSupplierPrice(newSku(materialId, 2L), 100L)),
                "uoMId=2 精确命中 6.00（而非 7.00）");
        assertEquals(0, new BigDecimal("6.00").compareTo(
                        resolver.resolveSupplierPrice(newSku(materialId, null), 100L)),
                "sku.uoMId null 宽放（仅 materialId 匹配）→ 双行候选按 unitPrice 低者 6.00");
    }

    // ============ master-data 侧集成：resolvePrice 经注入 supplierPriceResolver 返回价格表层价 ============

    @Test
    public void testResolvePriceIntegrationSupplierTierWins() {
        Long materialId = seedMaterialAndSku("SPL-INT", new BigDecimal("10.00"));
        Long skuId = skuIdFor(materialId);
        // supplier 价格表层命中 7.77（低于默认档采购价 10.00）
        seedPriceList(300L, materialId, 1L, new BigDecimal("7.77"), null, true, null, null);

        Object data = rpcData(query, "ErpMdMaterialSku__resolvePrice",
                resolvePriceArgs(skuId, 300L, ErpMdConstants.BILL_TYPE_PURCHASE, null));
        BigDecimal result = new BigDecimal(data.toString());
        assertEquals(0, new BigDecimal("7.77").compareTo(result),
                "supplier 价格表层命中应返回价格表层价（非默认档 purchasePrice 10.0000）");
    }

    @Test
    public void testResolvePriceIntegrationNoSupplierTierFallsBack() {
        Long materialId = seedMaterialAndSku("SPL-DEF", new BigDecimal("10.00"));
        Long skuId = skuIdFor(materialId);

        Object data = rpcData(query, "ErpMdMaterialSku__resolvePrice",
                resolvePriceArgs(skuId, 300L, ErpMdConstants.BILL_TYPE_PURCHASE, null));
        assertEquals(0, new BigDecimal("10.0000").compareTo(new BigDecimal(data.toString())),
                "无 supplier 价格表层命中 → 回退 SKU 默认档 purchasePrice");
    }

    // ---------- helpers ----------

    private Map<String, Object> resolvePriceArgs(Long skuId, Long partnerId, String billType,
                                                 BigDecimal manualPrice) {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("skuId", skuId);
        args.put("partnerId", partnerId);
        args.put("billType", billType);
        args.put("manualPrice", manualPrice);
        return args;
    }

    private ErpMdMaterialSku newSku(Long materialId, Long uoMId) {
        ErpMdMaterialSku sku = new ErpMdMaterialSku();
        sku.setMaterialId(materialId);
        sku.setUoMId(uoMId);
        return sku;
    }

    private Long seedPriceList(Long supplierId, Long materialId, Long uoMId, BigDecimal unitPrice,
                               Integer priority, Boolean isActive, LocalDate validFrom, LocalDate validTo) {
        ErpPurSupplierPriceList pl = new ErpPurSupplierPriceList();
        pl.setSupplierId(supplierId);
        pl.setMaterialId(materialId);
        pl.setUoMId(uoMId);
        pl.setCurrencyId(1L);
        pl.setUnitPrice(unitPrice);
        pl.setPriority(priority);
        pl.setIsActive(isActive);
        pl.setValidFrom(validFrom);
        pl.setValidTo(validTo);
        ormTemplate.runInSession(() -> priceListDao().saveEntity(pl));
        return pl.getId();
    }

    private Long seedMaterialAndSku(String codePrefix, BigDecimal purchasePrice) {
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
            sku.setPurchasePrice(purchasePrice);
            skuDao().saveEntity(sku);
        });
        return material.getId();
    }

    private Long skuIdFor(Long materialId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("materialId", materialId));
        return skuDao().findAllByQuery(q).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    private IEntityDao<ErpPurSupplierPriceList> priceListDao() {
        return daoProvider.daoFor(ErpPurSupplierPriceList.class);
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }

    private Object rpcData(GraphQLOperationType opType, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 应成功，实际 code=" + resp.getCode());
        return resp.getData();
    }
}
