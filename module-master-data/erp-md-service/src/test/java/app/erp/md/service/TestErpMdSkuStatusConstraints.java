package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
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
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 主数据业务服务 Phase 3 集成测试（UC-MD-06 SKU 状态约束）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@code testCannotDeactivateOnlyDefaultSku}：停用/删除唯一默认 SKU → 拒绝。</li>
 *   <li>{@code testMaterialDeactivateCascadeGuard}：物料停用后其 SKU 不可被新单引用（resolveSku 返回 null）。</li>
 *   <li>{@code testDeleteReferencedSkuRejected}：SKU 被引用（经 SPI 桩）→ delete 拒绝。</li>
 *   <li>{@code testDeleteUnreferencedSkuOk}：SKU 未被引用 → validateSkuDeactivation 放行。</li>
 * </ul>
 *
 * <p>跨域引用检查经 {@link TestStubSkuReferenceChecker} 桩模拟（master-data 不反向依赖下游域）。
 *
 * <p>对应计划 {@code docs/plans/2026-07-07-0024-1} Phase 3。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-sku-reference-checker.beans.xml")
public class TestErpMdSkuStatusConstraints extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubSkuReferenceChecker refChecker;

    // ============ UC-MD-06 默认 SKU 唯一性守卫 ============

    @Test
    public void testCannotDeactivateOnlyDefaultSku() {
        // 物料只有一个默认 SKU → 停用/删除该 SKU 应拒绝
        Long materialId = seedMaterialAndSku("MAT-DEF-ONLY", true);
        Long skuId = skuIdFor(materialId);

        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", skuId)));
        assertEquals(ErpMdErrors.ERR_CANNOT_DEACTIVATE_DEFAULT_SKU.getErrorCode(), resp.getCode(),
                "停用唯一默认 SKU 应拒绝");
    }

    @Test
    public void testCanDeactivateNonDefaultSku() {
        // 物料有默认 SKU + 另一个非默认 SKU → 停用非默认 SKU 应放行
        Long materialId = seedMaterialAndSku("MAT-NON-DEF", true);
        Long nonDefaultSkuId = seedExtraSku(materialId, "SKU-EXTRA", false);

        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(0, resp.getStatus(), "停用非默认 SKU 应放行");
        assertEquals(Boolean.TRUE, resp.getData(), "validateSkuDeactivation 应返回 true");
    }

    // ============ UC-MD-06 物料停用联动 ============

    @Test
    public void testMaterialDeactivateCascadeGuard() {
        Long materialId = seedMaterialAndSku("MAT-CASC", true);
        Long skuId = skuIdFor(materialId);

        // 物料 ACTIVE 时，resolveSku 正常返回
        Map<?, ?> active = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku",
                Map.of("materialId", materialId));
        assertNotNull(active, "物料 ACTIVE 时 resolveSku 应返回 SKU");

        // 停用物料（status → INACTIVE）
        Long mid = materialId;
        ormTemplate.runInSession(() -> {
            ErpMdMaterial m = materialDao().getEntityById(mid);
            m.setStatus(ErpMdConstants.ACTIVE_STATUS_INACTIVE);
        });

        // 物料 INACTIVE 后，resolveSku 返回 null（联动 SKU 不可被新单引用）
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("materialId", materialId);
        Map<?, ?> inactive = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku", args);
        assertNull(inactive, "物料 INACTIVE 时 resolveSku 应返回 null（联动过滤）");

        // findDefaultSku 同样受联动过滤
        Map<?, ?> defInactive = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findDefaultSku",
                Map.of("materialId", materialId));
        assertNull(defInactive, "物料 INACTIVE 时 findDefaultSku 应返回 null");
    }

    // ============ UC-MD-06 删除引用校验 ============

    @Test
    public void testDeleteReferencedSkuRejected() {
        // 物料有两个 SKU（默认 + 非默认），删除非默认 SKU 但它被引用 → 拒绝
        Long materialId = seedMaterialAndSku("MAT-REF", true);
        Long nonDefaultSkuId = seedExtraSku(materialId, "SKU-REF-EXTRA", false);
        refChecker.markReferenced(nonDefaultSkuId);

        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL.getErrorCode(), resp.getCode(),
                "被引用 SKU 停用/删除应拒绝");
    }

    @Test
    public void testDeleteUnreferencedSkuOk() {
        // 物料有两个 SKU（默认 + 非默认），删除非默认 SKU 且未被引用 → 放行
        Long materialId = seedMaterialAndSku("MAT-UNREF", true);
        Long nonDefaultSkuId = seedExtraSku(materialId, "SKU-UNREF-EXTRA", false);
        // 不标记引用

        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(0, resp.getStatus(), "未被引用 SKU 应放行");
        assertEquals(Boolean.TRUE, resp.getData(), "validateSkuDeactivation 应返回 true");
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

    private Long skuIdFor(Long materialId) {
        return skuDao().findAllByQuery(byMaterial(materialId)).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    private io.nop.api.core.beans.query.QueryBean byMaterial(Long materialId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("materialId", materialId));
        return q;
    }

    private Long seedMaterialAndSku(String codePrefix, boolean withDefaultSku) {
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
            sku.setIsDefault(withDefaultSku);
            sku.setPurchasePrice(new BigDecimal("10.00"));
            skuDao().saveEntity(sku);
        });
        return material.getId();
    }

    private Long seedExtraSku(Long materialId, String skuCode, boolean isDefault) {
        ErpMdMaterialSku sku = new ErpMdMaterialSku();
        sku.setMaterialId(materialId);
        sku.setSkuCode(skuCode);
        sku.setUoMId(1L);
        sku.setConversionRate(BigDecimal.ONE);
        sku.setIsDefault(isDefault);
        ormTemplate.runInSession(() -> skuDao().saveEntity(sku));
        // 重新查询获取生成的 ID
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("skuCode", skuCode));
        return skuDao().findAllByQuery(q).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
