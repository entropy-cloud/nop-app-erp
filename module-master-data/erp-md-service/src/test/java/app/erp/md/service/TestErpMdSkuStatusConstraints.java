package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * <p>对应计划 {@code docs/plans/2026-07-07-0024-1} Phase 3；
 * RC-R1.72（plan 2026-08-19-0445-1 Phase 1 Proof）扩展 status 列独立停用语义七项。
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

    // ============ RC-R1.72 Phase 1 Proof：status 列独立停用语义 ============

    /** Proof ①：status=INACTIVE 迁移唯一默认 SKU（update 路径）→ 守卫 1 拒绝。 */
    @Test
    public void testUpdateOnlyDefaultSkuToInactiveRejected() {
        Long materialId = seedMaterialAndSku("MAT-UPD-DEF-ONLY", true);
        Long skuId = skuIdFor(materialId);

        ApiResponse<?> resp = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(skuId, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(ErpMdErrors.ERR_CANNOT_DEACTIVATE_DEFAULT_SKU.getErrorCode(), resp.getCode(),
                "update 迁移唯一默认 SKU 至 INACTIVE 应拒绝");
    }

    /** Proof ②：非默认 SKU（默认兄弟在场）update → INACTIVE 成功且落库。 */
    @Test
    public void testUpdateNonDefaultSkuToInactiveOk() {
        Long materialId = seedMaterialAndSku("MAT-UPD-NON-DEF", true);
        Long nonDefaultSkuId = seedExtraSku(materialId, "SKU-UPD-EXTRA", false);

        ApiResponse<?> resp = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(0, resp.getStatus(), "非默认 SKU 停用应放行，实际 " + resp.getCode() + " " + resp.getMsg());

        ormTemplate.runInSession(() -> {
            ErpMdMaterialSku sku = skuDao().getEntityById(nonDefaultSkuId);
            assertEquals(ErpMdConstants.ACTIVE_STATUS_INACTIVE, sku.getStatus(), "停用应落库 status=INACTIVE");
        });
    }

    /** Proof ③：默认 SKU 停用后 findDefaultSku/resolveSku/findSkuByBarcode 跳过 INACTIVE。 */
    @Test
    public void testInactiveSkuSkippedByReadApis() {
        Long materialId = seedMaterialAndSku("MAT-READ-SKIP", true);
        Long defaultSkuId = skuIdFor(materialId);
        // 兄弟 SKU 同单位（可用接替者），默认 SKU 带条码
        setBarcode(defaultSkuId, "BC-READ-SKIP");
        seedExtraSku(materialId, "SKU-READ-SKIP-SIB", false);

        // 停用默认 SKU（兄弟可用 → 守卫 1 放行）
        ApiResponse<?> deact = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(defaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(0, deact.getStatus(), "停用默认 SKU（存在可用兄弟）应放行");

        // findDefaultSku：INACTIVE 默认 SKU 不可被新单引用 → null
        Map<?, ?> def = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findDefaultSku",
                Map.of("materialId", materialId));
        assertNull(def, "停用后的默认 SKU 应被 findDefaultSku 跳过");

        // resolveSku(unitId)：单位匹配命中 INACTIVE 不构成可用，返回活跃兄弟
        Map<String, Object> args = new HashMap<>();
        args.put("materialId", materialId);
        args.put("unitId", 1L);
        Map<?, ?> resolved = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku", args);
        assertNotNull(resolved, "resolveSku 应返回活跃兄弟 SKU");
        assertEquals("SKU-READ-SKIP-SIB", resolved.get("skuCode"), "应跳过 INACTIVE 命中活跃兄弟");

        // findSkuByBarcode：INACTIVE SKU 条码反查 → null
        Map<?, ?> byBarcode = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findSkuByBarcode",
                Map.of("barcode", "BC-READ-SKIP"));
        assertNull(byBarcode, "停用后的 SKU 应被 findSkuByBarcode 跳过");
    }

    /** Proof ④：null status 派生 ACTIVE 兼容（存量行零迁移）。 */
    @Test
    public void testNullStatusDerivedActive() {
        // 单默认 SKU（status=null）经读侧解析可达
        Long materialId = seedMaterialAndSku("MAT-NULL-ACTIVE", true);
        Map<?, ?> def = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__findDefaultSku",
                Map.of("materialId", materialId));
        assertNotNull(def, "null status 应派生 ACTIVE，findDefaultSku 正常返回");
        Map<String, Object> args = new HashMap<>();
        args.put("materialId", materialId);
        Map<?, ?> resolved = (Map<?, ?>) rpcData(query, "ErpMdMaterialSku__resolveSku", args);
        assertNotNull(resolved, "null status 应派生 ACTIVE，resolveSku 正常返回");

        // null 兄弟 SKU 计为可用接替者：默认 A(null) 停用放行（B(null) 接替在场）
        Long materialId2 = seedMaterialAndSku("MAT-NULL-SIBLING", true);
        seedExtraSku(materialId2, "SKU-NULL-SIB", false);
        Long defaultSkuId2 = skuIdFor(materialId2);
        ApiResponse<?> deact = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(defaultSkuId2, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(0, deact.getStatus(), "null status 兄弟应计为可用接替者，停用默认 SKU 放行");
    }

    /**
     * Proof ⑤：status→INACTIVE 迁移触发守卫（守卫 2 经桩），非停用迁移不触发
     * （改码改名放行 / INACTIVE 行再编辑放行 / 停用→启用恢复放行 / null→ACTIVE 显式化放行）。
     */
    @Test
    public void testUpdateGuardTriggersOnlyOnDeactivationTransition() {
        Long materialId = seedMaterialAndSku("MAT-TRANSITION", true);
        Long nonDefaultSkuId = seedExtraSku(materialId, "SKU-TRANSITION-EXTRA", false);

        // 非停用迁移：改码（status 未触碰）不触发守卫
        ApiResponse<?> rename = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "skuCode", "SKU-TRANSITION-RENAMED"))));
        assertEquals(0, rename.getStatus(), "改码不应触发停用守卫");

        // 停用迁移 + 被引用（守卫 2 经桩）→ 拒绝
        refChecker.markReferenced(nonDefaultSkuId);
        ApiResponse<?> blocked = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL.getErrorCode(), blocked.getCode(),
                "status→INACTIVE 迁移应触发引用守卫");

        // 解除引用后停用迁移放行
        refChecker.clear();
        ApiResponse<?> deact = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_INACTIVE))));
        assertEquals(0, deact.getStatus(), "未被引用的停用迁移应放行");

        // 已停用行再编辑（old=INACTIVE → new=INACTIVE，非迁移）不重复触发守卫
        ApiResponse<?> editInactive = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "skuCode", "SKU-TRANSITION-EDITED"))));
        assertEquals(0, editInactive.getStatus(), "已停用行改名不应重复触发守卫");

        // 停用 → 启用恢复（无前置条件）
        ApiResponse<?> reactivate = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(nonDefaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_ACTIVE))));
        assertEquals(0, reactivate.getStatus(), "停用→启用恢复应放行");

        // null→ACTIVE 显式化（非停用迁移）不触发
        Long defaultSkuId = skuIdFor(materialId);
        ApiResponse<?> explicitActive = rpc(mutation, "ErpMdMaterialSku__update",
                ApiRequest.build(Map.of("data", updatePayload(defaultSkuId, "status", ErpMdConstants.ACTIVE_STATUS_ACTIVE))));
        assertEquals(0, explicitActive.getStatus(), "null→ACTIVE 显式化不应触发守卫");
    }

    /** Proof ⑥：删除含被引用 SKU 的物料被拒绝（级联旁路闭合，错误信息携带阻断 SKU）。 */
    @Test
    public void testDeleteMaterialWithReferencedSkuRejected() {
        Long materialId = seedMaterialAndSku("MAT-CASC-REF", true);
        Long referencedSkuId = seedExtraSku(materialId, "SKU-CASC-REF-EXTRA", false);
        refChecker.markReferenced(referencedSkuId);

        ApiResponse<?> resp = rpc(mutation, "ErpMdMaterial__delete",
                ApiRequest.build(Map.of("id", String.valueOf(materialId))));
        assertEquals(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL.getErrorCode(), resp.getCode(),
                "删除含被引用 SKU 的物料应拒绝（级联旁路闭合）");
        assertTrue(resp.getMsg().contains(String.valueOf(referencedSkuId)),
                "错误信息应携带阻断 SKU 标识，实际: " + resp.getMsg());
    }

    /** Proof ⑦（负控）：无引用的单默认 SKU 物料删除成功（守卫 1 不误伤整体删除）。 */
    @Test
    public void testDeleteMaterialWithOnlyDefaultSkuOk() {
        Long materialId = seedMaterialAndSku("MAT-CASC-SOLO", true);

        ApiResponse<?> resp = rpc(mutation, "ErpMdMaterial__delete",
                ApiRequest.build(Map.of("id", String.valueOf(materialId))));
        assertEquals(0, resp.getStatus(), "无引用的单默认 SKU 物料删除应放行，实际 " + resp.getCode() + " " + resp.getMsg());
    }

    // ---------- helpers ----------

    private Map<String, Object> updatePayload(Long skuId, String field, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(skuId));
        data.put(field, value);
        return data;
    }

    private void setBarcode(Long skuId, String barcode) {
        ormTemplate.runInSession(() -> {
            ErpMdMaterialSku sku = skuDao().getEntityById(skuId);
            sku.setBarcode(barcode);
        });
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
