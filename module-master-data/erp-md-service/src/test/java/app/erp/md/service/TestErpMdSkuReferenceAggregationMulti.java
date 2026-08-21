package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
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

/**
 * RC-R1.72 Phase 2 D4 Proof：List 收集器聚合「任一命中即拒绝」（OR）语义——
 * 双桩 bean（{@link TestStubSkuReferenceChecker} + {@link TestStubSkuReferenceCheckerSecondary}）
 * 经 {@code test-sku-reference-checker-multi.beans.xml} 注册，由
 * {@code ErpMdSkuReferenceCheckerRegistry}（ioc:collect-beans）收集。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-sku-reference-checker-multi.beans.xml")
public class TestErpMdSkuReferenceAggregationMulti extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubSkuReferenceChecker primaryChecker;
    @Inject
    TestStubSkuReferenceCheckerSecondary secondaryChecker;

    @Test
    public void testAnyCheckerHitRejects() {
        String materialId = seedMaterialWithDefaultAndExtra();
        String nonDefaultSkuId = extraSkuId("SKU-AGG-EXTRA");

        // 两实例均未标记 → 放行
        ApiResponse<?> pass = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(0, pass.getStatus(), "两 checker 均未命中应放行");

        // 仅第一实例命中 → 拒绝（OR 语义）
        primaryChecker.markReferenced(nonDefaultSkuId);
        ApiResponse<?> hit1 = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL.getErrorCode(), hit1.getCode(),
                "第一 checker 命中应拒绝");

        // 仅第二实例命中 → 拒绝（OR 语义对称）
        primaryChecker.clear();
        secondaryChecker.markReferenced(nonDefaultSkuId);
        ApiResponse<?> hit2 = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL.getErrorCode(), hit2.getCode(),
                "第二 checker 命中应拒绝");

        // 解除后恢复放行
        secondaryChecker.clear();
        ApiResponse<?> pass2 = rpc(query, "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", nonDefaultSkuId)));
        assertEquals(0, pass2.getStatus(), "解除引用后应恢复放行");
    }

    // ---------- seeds ----------

    private String seedMaterialWithDefaultAndExtra() {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode("M-AGG");
        material.setName("物料-AGG");
        material.setMaterialType("GOODS");
        material.setUoMId("1");
        material.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        ormTemplate.runInSession(() -> {
            materialDao().saveEntity(material);
            ErpMdMaterialSku def = new ErpMdMaterialSku();
            def.setMaterialId(material.getId());
            def.setSkuCode("SKU-AGG-DEF");
            def.setUoMId("1");
            def.setConversionRate(BigDecimal.ONE);
            def.setIsDefault(true);
            skuDao().saveEntity(def);
            ErpMdMaterialSku extra = new ErpMdMaterialSku();
            extra.setMaterialId(material.getId());
            extra.setSkuCode("SKU-AGG-EXTRA");
            extra.setUoMId("1");
            extra.setConversionRate(BigDecimal.ONE);
            extra.setIsDefault(false);
            skuDao().saveEntity(extra);
        });
        return material.getId();
    }

    private String extraSkuId(String skuCode) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("skuCode", skuCode));
        return skuDao().findAllByQuery(q).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private io.nop.dao.api.IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private io.nop.dao.api.IEntityDao<ErpMdMaterialSku> skuDao() {
        return daoProvider.daoFor(ErpMdMaterialSku.class);
    }
}
