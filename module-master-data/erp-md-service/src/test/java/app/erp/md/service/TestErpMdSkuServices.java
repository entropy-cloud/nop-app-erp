package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdUoMConversion;
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

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主数据业务服务 Phase 1 集成测试（UC-MD-01 扫码 + UC-MD-05 默认 SKU + UC-MD-02 多单位换算）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpMdMaterialSku__findSkuByBarcode/findDefaultSku/resolveSku}
 * 与 {@code ErpMdUoMConversion__convertQty}，断言读解析、换算正确性与 barcode 应用层查重拒绝。
 * {@code @NopTestConfig} 启用本地 H2 + 建表，无快照录制（精确断言场景）。
 *
 * <p>对应计划 {@code docs/plans/2026-07-07-0024-1} Phase 1。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSkuServices extends JunitAutoTestCase {

    static final Long UOM_BOTTLE = 8001L;   // 瓶（基本单位）
    static final Long UOM_CASE = 8002L;     // 箱
    static final Long UOM_PALLET = 8003L;   // 托盘

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ============ UC-MD-01 扫码开单 ============

    @Test
    public void testFindSkuByBarcode() {
        Long materialId = seedMaterialWithSkus("MAT-BC-1", true);

        // 命中：默认 SKU 的 barcode
        ErpMdMaterialSku defaultSku = findDefaultSkuViaRpc(materialId);
        Long skuId = defaultSku.getId();
        ormTemplate.runInSession(() -> {
            ErpMdMaterialSku s = skuDao().getEntityById(skuId);
            s.setBarcode("BC-HIT");
            // MANAGED 实体修改后 session flush 自动持久化，无需 save
        });

        Map<?, ?> hit = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findSkuByBarcode",
                Map.of("barcode", "BC-HIT"));
        assertNotNull(hit, "条码命中应返回 SKU");
        assertEquals(skuId, toLong(hit.get("id")));

        // 未命中
        assertNull(rpcData(query, "ErpMdMaterialSku__findSkuByBarcode", Map.of("barcode", "NOT-EXIST")));
    }

    // ============ UC-MD-05 默认 SKU 兜底 ============

    @Test
    public void testFindDefaultSku() {
        // 有默认 SKU
        Long materialId = seedMaterialWithSkus("MAT-DEF-1", true);
        Map<?, ?> def = findDefaultSkuMap(materialId);
        assertNotNull(def, "应有默认 SKU");
        assertEquals(Boolean.TRUE, def.get("isDefault"));

        // 无默认 SKU
        Long materialId2 = seedMaterialWithSkus("MAT-DEF-2", false);
        Map<?, ?> none = findDefaultSkuMap(materialId2);
        assertNull(none, "无默认 SKU 应返回 null");
    }

    @Test
    public void testResolveSkuByUnit() {
        Long materialId = seedMaterialWithSkus("MAT-RSV-1", true);

        // 按单位匹配（瓶=基本单位 SKU）
        Map<String, Object> byUnitArgs = new java.util.HashMap<>();
        byUnitArgs.put("materialId", materialId);
        byUnitArgs.put("unitId", UOM_BOTTLE);
        Map<?, ?> byUnit = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku", byUnitArgs);
        assertNotNull(byUnit, "按单位匹配应返回 SKU");

        // unitId=null → 兜底默认 SKU
        Map<String, Object> fallbackArgs = new java.util.HashMap<>();
        fallbackArgs.put("materialId", materialId);
        fallbackArgs.put("unitId", null);
        Map<?, ?> fallback = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku", fallbackArgs);
        assertNotNull(fallback, "unitId 空应兜底默认 SKU");
        assertEquals(Boolean.TRUE, fallback.get("isDefault"));
    }

    @Test
    public void testResolveSkuNoDefaultRequired() {
        // 物料无默认 SKU + sku-default-required=true（默认） → 抛错
        Long materialId = seedMaterialWithSkus("MAT-REQ-1", false);

        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__resolveSku",
                ApiRequest.build(Map.of("materialId", materialId, "unitId", 99999L)));
        assertEquals(ErpMdErrors.ERR_SKU_DEFAULT_REQUIRED.getErrorCode(), resp.getCode(),
                "无默认 SKU 且 sku-default-required=true 应抛错");
    }

    // ============ UC-MD-02 多单位换算 ============

    @Test
    public void testConvertQtyMaterialLevel() {
        Long materialId = seedMaterialWithSkus("MAT-CV-1", true);
        // 物料级换算：1 箱 = 24 瓶
        seedUoMConversion(materialId, UOM_CASE, UOM_BOTTLE, new BigDecimal("24"));

        Object result = rpcData(query, "ErpMdUoMConversion__convertQty",
                Map.of("materialId", materialId, "qty", new BigDecimal("10"),
                        "fromUoMId", UOM_CASE, "toUoMId", UOM_BOTTLE));
        assertEquals(new BigDecimal("240.0000"), new BigDecimal(result.toString()),
                "10 箱 × 24 = 240 瓶（物料级系数）");
    }

    @Test
    public void testConvertQtyGenericFallback() {
        Long materialId = seedMaterialWithSkus("MAT-CV-2", true);
        // 通用换算（materialId=null）：1 托盘 = 576 瓶
        seedUoMConversion(null, UOM_PALLET, UOM_BOTTLE, new BigDecimal("576"));

        Object result = rpcData(query, "ErpMdUoMConversion__convertQty",
                Map.of("materialId", materialId, "qty", new BigDecimal("2"),
                        "fromUoMId", UOM_PALLET, "toUoMId", UOM_BOTTLE));
        assertEquals(new BigDecimal("1152.0000"), new BigDecimal(result.toString()),
                "2 托盘 × 576 = 1152 瓶（通用 fallback）");
    }

    @Test
    public void testConvertQtyStrictNotFound() {
        Long materialId = seedMaterialWithSkus("MAT-CV-3", true);
        // 不配置任何换算系数；strict 默认 true → 抛错

        ApiResponse<?> resp = rpc(query, "ErpMdUoMConversion__convertQty",
                ApiRequest.build(Map.of(
                        "materialId", materialId,
                        "qty", new BigDecimal("5"),
                        "fromUoMId", UOM_CASE,
                        "toUoMId", UOM_PALLET)));
        assertEquals(ErpMdErrors.ERR_UOM_CONVERSION_NOT_FOUND.getErrorCode(), resp.getCode(),
                "strict=true 未命中系数应抛错");
    }

    @Test
    public void testConvertQtySameUnit() {
        Long materialId = seedMaterialWithSkus("MAT-CV-4", true);
        Object result = rpcData(query, "ErpMdUoMConversion__convertQty",
                Map.of("materialId", materialId, "qty", new BigDecimal("7"),
                        "fromUoMId", UOM_BOTTLE, "toUoMId", UOM_BOTTLE));
        assertEquals(new BigDecimal("7.0000"), new BigDecimal(result.toString()),
                "同单位换算返回原值");
    }

    // ============ UC-MD-01 barcode 应用层查重 ============

    @Test
    public void testBarcodeDuplicateRejected() {
        // 准备：UoM（save 突变走 FK 校验，需先存在）
        ensureUoM(UOM_BOTTLE, "BOTTLE-DUP", "瓶");
        ensureUoM(UOM_CASE, "CASE-DUP", "箱");
        // SKU A 占用 barcode DUP-1
        Long materialId = seedMaterialWithSkus("MAT-DUP-1", true);
        ErpMdMaterialSku skuA = findDefaultSkuViaRpc(materialId);
        Long skuAId = skuA.getId();
        ormTemplate.runInSession(() -> {
            ErpMdMaterialSku s = skuDao().getEntityById(skuAId);
            s.setBarcode("DUP-1");
            // MANAGED 实体修改后 session flush 自动持久化，无需 save
        });

        // SKU B 试图用同 barcode → save（经 GraphQL save 走 defaultPrepareSave 钩子）拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpMdMaterialSku__save",
                ApiRequest.build(Map.of("data", Map.of(
                        "materialId", materialId,
                        "skuCode", "MAT-DUP-1-B",
                        "uoMId", UOM_CASE,
                        "barcode", "DUP-1",
                        "isDefault", false))));
        assertNotNull(resp, "save 应有响应");
        String code = resp.getCode();
        boolean isError = code != null && !code.isEmpty() && !"0".equals(code);
        assertTrue(isError || resp.getData() == null,
                "重复 barcode 应被拒绝（code 非 0 或 data 为 null）");
        if (isError) {
            assertEquals(ErpMdErrors.ERR_SKU_BARCODE_DUPLICATE.getErrorCode(), code,
                    "异常应为 ERR_SKU_BARCODE_DUPLICATE");
        }
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Object rpcData(GraphQLOperationType opType, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(opType, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功，实际 code=" + resp.getCode());
        return resp.getData();
    }

    private ErpMdMaterialSku findDefaultSkuViaRpc(Long materialId) {
        Map<?, ?> map = findDefaultSkuMap(materialId);
        if (map == null) {
            return null;
        }
        Long id = toLong(map.get("id"));
        return skuDao().getEntityById(id);
    }

    private Map<?, ?> findDefaultSkuMap(Long materialId) {
        return (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findDefaultSku", Map.of("materialId", materialId));
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.valueOf(v.toString());
    }

    /**
     * 创建物料 + 可选默认 SKU（瓶单位）。返回物料 ID。
     *
     * <p>UoM 实体不创建——H2 测试允许悬空外键（与 AVL 测试 seedApproval 不建 Partner 同模式），
     * 仅以 UOM_* 常量值作为 SKU.uoMId / UoMConversion.fromUoMId 的引用。
     *
     * @param withDefaultSku true=创建 isDefault=true 的 SKU；false=创建无默认标志的 SKU
     */
    private Long seedMaterialWithSkus(String codePrefix, boolean withDefaultSku) {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode("M-" + codePrefix);
        material.setName("物料-" + codePrefix);
        material.setMaterialType("GOODS");
        material.setUoMId(UOM_BOTTLE);
        material.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);

        ormTemplate.runInSession(() -> {
            materialDao().saveEntity(material);
            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.setMaterialId(material.getId());
            sku.setSkuCode("SKU-" + codePrefix);
            sku.setUoMId(UOM_BOTTLE);
            sku.setConversionRate(BigDecimal.ONE);
            sku.setIsDefault(withDefaultSku);
            sku.setPurchasePrice(new BigDecimal("10.00"));
            sku.setSalePrice(new BigDecimal("15.00"));
            sku.setWholesalePrice(new BigDecimal("12.00"));
            sku.setRetailPrice(new BigDecimal("18.00"));
            skuDao().saveEntity(sku);
        });
        return material.getId();
    }

    private void seedUoMConversion(Long materialId, Long fromUoMId, Long toUoMId, BigDecimal rate) {
        ErpMdUoMConversion conv = new ErpMdUoMConversion();
        conv.setMaterialId(materialId);
        conv.setFromUoMId(fromUoMId);
        conv.setToUoMId(toUoMId);
        conv.setConversionRate(rate);
        ormTemplate.runInSession(() -> conversionDao().saveEntity(conv));
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }

    private IEntityDao<ErpMdUoM> uomDao() {
        return daoProvider.daoFor(ErpMdUoM.class);
    }

    private IEntityDao<ErpMdUoMConversion> conversionDao() {
        return daoProvider.daoFor(ErpMdUoMConversion.class);
    }

    /** 幂等创建 UoM（存在则跳过）——barcode save 突变走 FK 校验需 UoM 实体存在。 */
    @SuppressWarnings("unchecked")
    private void ensureUoM(Long id, String code, String name) {
        ormTemplate.runInSession(() -> {
            if (uomDao().getEntityById(id) != null) {
                return;
            }
            ErpMdUoM uom = new ErpMdUoM();
            uom.setId(id);
            uom.setCode(code);
            uom.setName(name);
            uomDao().saveEntity(uom);
        });
    }
}
