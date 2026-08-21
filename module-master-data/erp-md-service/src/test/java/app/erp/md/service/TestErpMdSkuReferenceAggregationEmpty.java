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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.72 Phase 2 D4 Proof：空收集器放行——无 testBeansFile（容器内零
 * {@code IErpMdSkuReferenceChecker} bean）时 {@code ErpMdSkuReferenceCheckerRegistry}
 * 收集空集合，{@code validateSkuDeactivation} 守卫 2 空转放行（单域测试零回归语义）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSkuReferenceAggregationEmpty extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testEmptyCollectorPasses() {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode("M-AGG-EMPTY");
        material.setName("物料-AGG-EMPTY");
        material.setMaterialType("GOODS");
        material.setUoMId("1");
        material.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        ormTemplate.runInSession(() -> {
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(material);
            ErpMdMaterialSku def = new ErpMdMaterialSku();
            def.setMaterialId(material.getId());
            def.setSkuCode("SKU-AGG-EMPTY-DEF");
            def.setUoMId("1");
            def.setConversionRate(BigDecimal.ONE);
            def.setIsDefault(true);
            daoProvider.daoFor(ErpMdMaterialSku.class).saveEntity(def);
            ErpMdMaterialSku extra = new ErpMdMaterialSku();
            extra.setMaterialId(material.getId());
            extra.setSkuCode("SKU-AGG-EMPTY-EXTRA");
            extra.setUoMId("1");
            extra.setConversionRate(BigDecimal.ONE);
            extra.setIsDefault(false);
            daoProvider.daoFor(ErpMdMaterialSku.class).saveEntity(extra);
        });

        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("skuCode", "SKU-AGG-EMPTY-EXTRA"));
        String extraSkuId = daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);

        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(query,
                "ErpMdMaterialSku__validateSkuDeactivation",
                ApiRequest.build(Map.of("skuId", extraSkuId)));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), "空收集器下守卫 2 应空转放行");
        assertTrue(Boolean.TRUE.equals(resp.getData()), "validateSkuDeactivation 应返回 true");
    }
}
