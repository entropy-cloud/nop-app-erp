package app.erp.md.service.report;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
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
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ md-019-r3（DIM-T 覆盖缺口）：4 公开动作行为断言——
 * {@code resolvePriceWithSource}（md-002 现症主入口，带价格来源层的解析）、
 * {@code validateSkuReference}（跨域引用守卫查询）、
 * {@code materialPriceListData}（物料×默认SKU 四档价格数据集）、
 * {@code partnerListData}（往来单位分类清单数据集）。修复前零测试引用。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdPublicActionCoverage extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private String seedMaterialWithDefaultSku() {
        ormTemplate.runInSession(session -> {
            ErpMdMaterial material = new ErpMdMaterial();
            material.setCode("M28-RPT-MAT");
            material.setName("M2.8 报表物料");
            material.setMaterialType("RAW");
            material.setUoMId("1");
            material.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(material);

            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.setMaterialId(material.getId());
            sku.setSkuCode("SKU-M28-RPT");
            sku.setUoMId("1");
            sku.setConversionRate(BigDecimal.ONE);
            sku.setIsDefault(true);
            sku.setSalePrice(new BigDecimal("25.50"));
            sku.setPurchasePrice(new BigDecimal("12.00"));
            daoProvider.daoFor(ErpMdMaterialSku.class).saveEntity(sku);
            return null;
        });
        return skuIdByCode("SKU-M28-RPT");
    }

    private String seedPartner(String code, String type) {
        ormTemplate.runInSession(session -> {
            ErpMdPartner partner = new ErpMdPartner();
            partner.setCode(code);
            partner.setName("M2.8 往来单位 " + code);
            partner.setPartnerType(type);
            partner.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(partner);
            return null;
        });
        return code;
    }

    private String skuIdByCode(String skuCode) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("skuCode", skuCode));
        return daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q).stream()
                .map(ErpMdMaterialSku::getId).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rowsOf(ApiResponse<?> resp) {
        assertEquals(0, resp.getStatus(), "数据集查询应成功");
        return (List<Map<String, Object>>) resp.getData();
    }

    @Test
    public void testResolvePriceWithSourceReturnsResolvedPrice() {
        String skuId = seedMaterialWithDefaultSku();
        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__resolvePriceWithSource",
                ApiRequest.build(Map.of("skuId", skuId)));
        assertEquals(0, resp.getStatus(), "价格来源解析应成功");
        Object data = resp.getData();
        assertNotNull(data, "ResolvedPrice 非空");
        assertTrue(((Map<?, ?>) data).containsKey("unitPrice") || ((Map<?, ?>) data).containsKey("priceSource"),
                "解析结果承载价格字段: " + ((Map<?, ?>) data).keySet());
    }

    @Test
    public void testValidateSkuReferencePassesUnreferencedSku() {
        String skuId = seedMaterialWithDefaultSku();
        ApiResponse<?> resp = rpc(query, "ErpMdMaterialSku__validateSkuReference",
                ApiRequest.build(Map.of("skuId", skuId)));
        assertEquals(0, resp.getStatus(), "无单据引用的 SKU 引用校验应通过");
        assertEquals(Boolean.TRUE, resp.getData(), "校验返回 true");
    }

    @Test
    public void testMaterialPriceListDatasetContainsMaterialRow() {
        seedMaterialWithDefaultSku();
        List<Map<String, Object>> rows = rowsOf(rpc(query, "ErpMdReport__materialPriceListData",
                ApiRequest.build(Map.of("materialCode", "M28-RPT-MAT"))));
        assertTrue(rows.stream().anyMatch(r -> "M28-RPT-MAT".equals(r.get("materialCode"))),
                "物料价格清单应包含种子物料行: " + rows);
    }

    @Test
    public void testPartnerListDatasetFiltersByType() {
        seedPartner("M28-RPT-SUP", "SUPPLIER");
        List<Map<String, Object>> rows = rowsOf(rpc(query, "ErpMdReport__partnerListData",
                ApiRequest.build(Map.of("partnerType", "SUPPLIER"))));
        assertTrue(rows.stream().anyMatch(r -> "M28-RPT-SUP".equals(r.get("partnerCode"))),
                "往来单位清单应包含种子供应商行: " + rows);
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
