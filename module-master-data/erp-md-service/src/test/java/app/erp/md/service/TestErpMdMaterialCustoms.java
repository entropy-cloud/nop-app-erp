package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialCustoms;
import app.erp.md.dao.entity.ErpMdPartner;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C2 跨境贸易扩展：{@code ErpMdMaterialCustomsBizModel} 单元测试
 * （plan 2026-07-21-1206-1 Phase 2，docs/design/master-data/cross-border-trade.md §3）。
 *
 * <p>覆盖 4 场景（plan §Phase 2 TestErpMdMaterialCustoms 场景）：
 * <ol>
 *   <li>{@code testCrudLifecycle}：CRUD 基础生命周期（save → findPage → update → delete）。</li>
 *   <li>{@code testPartnerMustBeCustomsBroker}：partnerId 引用 Partner 类型非 CUSTOMS_BROKER 抛
 *       {@code ERR_PARTNER_NOT_CUSTOMS_BROKER}。</li>
 *   <li>{@code testSourceBillRequired}：sourceBillType/sourceBillCode 均空时抛
 *       {@code ERR_CUSTOMS_SOURCE_BILL_REQUIRED}。</li>
 *   <li>{@code testDeclarationNoUnique}：declarationNo 重复时抛
 *       {@code ERR_CUSTOMS_DECLARATION_NO_DUPLICATE}（DB UK 前置友好校验）。</li>
 * </ol>
 *
 * <p>测试方式：seed Material + seed Partner（CUSTOMS_BROKER）→ 经 GraphQL RPC 调用
 * {@code ErpMdMaterialCustoms__save} 等动作，断言成功/异常状态码。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdMaterialCustoms extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCrudLifecycle() {
        Long matId = seedMaterial("E2E-MC-CRUD-MAT");
        Long partnerId = seedCustomsBroker("E2E-MC-CRUD-BROKER");

        // 1. save：新建报关记录
        Map<String, Object> data = newCustomsPayload("E2E-MC-CRUD-1", matId, partnerId,
                "DECL-CRUD-001", "PURCHASE_RECEIVE", "PR-001");
        Map<?, ?> saved = saveAndExpectOk(data);
        String id = String.valueOf(saved.get("id"));
        assertNotNull(id, "save 应返回 id");

        // 2. findPage：查询应至少返回 1 条
        Map<?, ?> page = findPage(Map.of("limit", 10));
        int total = ((Number) page.get("total")).intValue();
        assertTrue(total >= 1, "findPage 应至少返回 1 条");

        // 3. update：修改 remark
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", String.valueOf(id));
        update.put("remark", "E2E-UPDATED");
        Map<?, ?> updated = updateAndExpectOk(update);
        assertEquals("E2E-UPDATED", updated.get("remark"), "update 后 remark 应一致");

        // 4. delete：逻辑删除后 findPage 不再包含此记录
        deleteAndExpectOk(String.valueOf(id));
        Map<?, ?> page2 = findPage(Map.of("filter_code", "E2E-MC-CRUD-1", "limit", 10));
        int total2 = ((Number) page2.get("total")).intValue();
        assertEquals(0, total2, "逻辑删除后按 code 过滤应返回 0 条");
    }

    @Test
    public void testPartnerMustBeCustomsBroker() {
        Long matId = seedMaterial("E2E-MC-PART-MAT");
        Long customerPartnerId = seedPartner("E2E-MC-PART-CUSTOMER", "CUSTOMER");

        // partnerType=CUSTOMER（非 CUSTOMS_BROKER）→ 抛 ERR_PARTNER_NOT_CUSTOMS_BROKER
        Map<String, Object> data = newCustomsPayload("E2E-MC-PART-1", matId, customerPartnerId,
                "DECL-PART-001", "PURCHASE_RECEIVE", "PR-002");
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__save",
                ApiRequest.build(Map.of("data", data)));
        assertNotEquals(0, resp.getStatus(),
                "partnerType=CUSTOMER 应被拒绝（status!=" + resp.getStatus() + "）");
    }

    @Test
    public void testSourceBillRequired() {
        Long matId = seedMaterial("E2E-MC-SRC-MAT");
        Long partnerId = seedCustomsBroker("E2E-MC-SRC-BROKER");

        // sourceBillType/sourceBillCode 均空 → 抛 ERR_CUSTOMS_SOURCE_BILL_REQUIRED
        Map<String, Object> data = newCustomsPayload("E2E-MC-SRC-1", matId, partnerId,
                "DECL-SRC-001", null, null);
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__save",
                ApiRequest.build(Map.of("data", data)));
        assertNotEquals(0, resp.getStatus(),
                "sourceBillType/Code 均空应被拒绝（status!=" + resp.getStatus() + "）");
    }

    @Test
    public void testDeclarationNoUnique() {
        Long matId = seedMaterial("E2E-MC-DUP-MAT");
        Long partnerId = seedCustomsBroker("E2E-MC-DUP-BROKER");

        // 第一次保存成功
        Map<String, Object> data1 = newCustomsPayload("E2E-MC-DUP-1", matId, partnerId,
                "DECL-DUP-001", "PURCHASE_RECEIVE", "PR-003");
        saveAndExpectOk(data1);

        // 第二次同 declarationNo → 抛 ERR_CUSTOMS_DECLARATION_NO_DUPLICATE
        Map<String, Object> data2 = newCustomsPayload("E2E-MC-DUP-2", matId, partnerId,
                "DECL-DUP-001", "SALES_SHIP", "SS-001");
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__save",
                ApiRequest.build(Map.of("data", data2)));
        assertNotEquals(0, resp.getStatus(),
                "declarationNo 重复应被拒绝（status!=" + resp.getStatus() + "）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Map<String, Object> newCustomsPayload(String code, Long materialId, Long partnerId,
                                                   String declarationNo,
                                                   String sourceBillType, String sourceBillCode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("materialId", String.valueOf(materialId));
        if (partnerId != null) {
            data.put("partnerId", String.valueOf(partnerId));
        }
        data.put("declarationNo", declarationNo);
        data.put("declarationDate", "2026-07-21");
        data.put("qtyDeclared", new BigDecimal("100"));
        data.put("uomDeclared", "千克");
        data.put("amountDeclared", new BigDecimal("10000.00"));
        if (sourceBillType != null) {
            data.put("sourceBillType", sourceBillType);
        }
        if (sourceBillCode != null) {
            data.put("sourceBillCode", sourceBillCode);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> saveAndExpectOk(Map<String, Object> data) {
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "save 应成功，实际 status=" + resp.getStatus()
                + " code=" + resp.getCode() + " body=" + resp.toString());
        return (Map<String, Object>) resp.getData();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> updateAndExpectOk(Map<String, Object> data) {
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__update",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "update 应成功，实际 status=" + resp.getStatus()
                + " code=" + resp.getCode() + " body=" + resp.toString());
        return (Map<String, Object>) resp.getData();
    }

    private void deleteAndExpectOk(String id) {
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdMaterialCustoms__delete",
                ApiRequest.build(Map.of("id", id)));
        assertEquals(0, resp.getStatus(), "delete 应成功，实际 status=" + resp.getStatus());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findPage(Map<String, Object> filter) {
        ApiResponse<?> resp = rpc(GraphQLOperationType.query, "ErpMdMaterialCustoms__findPage",
                ApiRequest.build(filter));
        assertEquals(0, resp.getStatus(), "findPage 应成功，实际 status=" + resp.getStatus());
        return (Map<String, Object>) resp.getData();
    }

    private Long seedMaterial(String code) {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode(code);
        material.setName("E2E-" + code);
        material.setMaterialType("GOODS");
        material.setUoMId(1L);
        material.setStatus("ACTIVE");
        ormTemplate.runInSession(() -> materialDao().saveEntity(material));
        return material.getId();
    }

    private Long seedCustomsBroker(String code) {
        return seedPartner(code, "CUSTOMS_BROKER");
    }

    private Long seedPartner(String code, String partnerType) {
        ErpMdPartner partner = new ErpMdPartner();
        partner.setCode(code);
        partner.setName("E2E-" + code);
        partner.setPartnerType(partnerType);
        partner.setStatus("ACTIVE");
        ormTemplate.runInSession(() -> partnerDao().saveEntity(partner));
        return partner.getId();
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }

    private IEntityDao<ErpMdPartner> partnerDao() {
        return daoProvider.daoFor(ErpMdPartner.class);
    }
}
