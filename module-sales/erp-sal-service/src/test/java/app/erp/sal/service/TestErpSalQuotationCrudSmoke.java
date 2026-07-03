package app.erp.sal.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 CRUD 冒烟测试（sales 域）。
 *
 * <p>抽样实体（Phase 1 实体抽样规则）：主实体 ErpSalQuotation（覆盖新建/查询筛选/编辑保存/逻辑删除），
 * 头-行对 ErpSalQuotation(头) → ErpSalQuotationLine(行)，行实体 quotationId 外键引用头实体主键，覆盖关系导航。
 *
 * <p>沿用 Phase 1 样板（-service 模块、JunitAutoTestCase、@NopTestConfig schema bootstrap、
 * _cases/ 快照）。多步 ID 在 Java 内传递；删除用例 DEL_VERSION 列以通配符 * 屏蔽时钟型非确定性。
 * 本域测试模块 test-scope 依赖 master-data-service 以注册跨域业务对象（ErpMd*），
 * 使 save 的跨域引用校验(validateRefValue)可在 H2 内存库通过；前置主数据由 createPrereqs() 自建。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalQuotationCrudSmoke extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-SAL");
        headData.put("customerId", pre.get("partner"));
        headData.put("businessDate", "2026-07-01");
        headData.put("currencyId", pre.get("currency"));
        headData.put("docStatus", "DRAFT");
        headData.put("approveStatus", "UNSUBMITTED");
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__save",
                ApiRequest.build(Map.of("data", headData)));

        assertEquals(0, result.getStatus());
        assertNotNull(((Map<?, ?>) result.getData()).get("id"), "新建应返回非空 ID");
        output("response.json5", result);
    }

    @Test
    public void testQueryHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-SAL");
        headData.put("customerId", pre.get("partner"));
        headData.put("businessDate", "2026-07-01");
        headData.put("currencyId", pre.get("currency"));
        headData.put("docStatus", "DRAFT");
        headData.put("approveStatus", "UNSUBMITTED");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalQuotation__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "查询应至少返回 1 条");
        output("response.json5", page);
    }

    @Test
    public void testUpdateHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-SAL");
        headData.put("customerId", pre.get("partner"));
        headData.put("businessDate", "2026-07-01");
        headData.put("currencyId", pre.get("currency"));
        headData.put("docStatus", "DRAFT");
        headData.put("approveStatus", "UNSUBMITTED");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id);
        upd.put("remark", "冒烟-已修改");
        ApiResponse<?> updated = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__update",
                ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, updated.getStatus());
        assertEquals("冒烟-已修改", ((Map<?, ?>) updated.getData()).get("remark"), "编辑保存后字段应与输入一致");
        output("2_update_response.json5", updated);
    }

    @Test
    public void testDeleteHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-SAL");
        headData.put("customerId", pre.get("partner"));
        headData.put("businessDate", "2026-07-01");
        headData.put("currencyId", pre.get("currency"));
        headData.put("docStatus", "DRAFT");
        headData.put("approveStatus", "UNSUBMITTED");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        ApiResponse<?> deleted = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__delete",
                ApiRequest.build(Map.of("id", id)));
        assertEquals(0, deleted.getStatus());

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalQuotation__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertEquals(0, ((Number) ((Map<?, ?>) page.getData()).get("total")).intValue(), "逻辑删除后查询应返回 0 条");
        output("response.json5", page);
    }

    @Test
    public void testLineRelation() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-SAL");
        headData.put("customerId", pre.get("partner"));
        headData.put("businessDate", "2026-07-01");
        headData.put("currencyId", pre.get("currency"));
        headData.put("docStatus", "DRAFT");
        headData.put("approveStatus", "UNSUBMITTED");
        ApiResponse<?> head = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotation__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_saveHead_response.json5", head);
        String headId = String.valueOf(((Map<?, ?>) head.getData()).get("id"));

        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("lineNo", 1);
        lineData.put("materialId", pre.get("material"));
        lineData.put("uoMId", pre.get("uom"));
        lineData.put("unitPrice", 100);
        lineData.put("quotationId", headId);
        ApiResponse<?> line = executeRpc(GraphQLOperationType.mutation, "ErpSalQuotationLine__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, line.getStatus());
        output("2_saveLine_response.json5", line);

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("filter_quotationId", headId);
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpSalQuotationLine__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "按外键查询应至少返回 1 行");
        output("response.json5", page);
    }

    private Map<String, String> createPrereqs() {
        Map<String, String> ids = new LinkedHashMap<>();
Map<String, Object> d_currency = new LinkedHashMap<>();
        d_currency.put("code", "CNY");
        d_currency.put("name", "元");
        ids.put("currency", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdCurrency__save", ApiRequest.build(Map.of("data", d_currency))).getData()).get("id")));
        Map<String, Object> d_partner = new LinkedHashMap<>();
        d_partner.put("code", "SMOKE-PARTNER");
        d_partner.put("name", "冒烟客户");
        d_partner.put("partnerType", "CUSTOMER");
        d_partner.put("status", "ACTIVE");
        ids.put("partner", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save", ApiRequest.build(Map.of("data", d_partner))).getData()).get("id")));
        Map<String, Object> d_uom = new LinkedHashMap<>();
        d_uom.put("code", "PCS");
        d_uom.put("name", "个");
        ids.put("uom", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdUoM__save", ApiRequest.build(Map.of("data", d_uom))).getData()).get("id")));
        Map<String, Object> d_material = new LinkedHashMap<>();
        d_material.put("code", "SMOKE-MAT");
        d_material.put("name", "冒烟物料");
        d_material.put("materialType", "GOODS");
        d_material.put("uoMId", ids.get("uom"));
        d_material.put("status", "ACTIVE");
        ids.put("material", String.valueOf(((Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpMdMaterial__save", ApiRequest.build(Map.of("data", d_material))).getData()).get("id")));
        return ids;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
