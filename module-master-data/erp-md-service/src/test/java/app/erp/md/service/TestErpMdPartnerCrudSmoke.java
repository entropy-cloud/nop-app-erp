package app.erp.md.service;

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
 * Phase 4 CRUD 冒烟测试样板（master-data 域）。
 *
 * <p>抽样实体选取（Phase 1 Decision 实体抽样规则）：
 * <ul>
 *   <li>主实体 {@code ErpMdPartner}（往来单位）：独立实体，无强制外键，含可编辑文本字段
 *       {@code name}/{@code phone} 与逻辑删除字段 {@code delVersion}，覆盖
 *       新建 / 查询筛选 / 编辑保存 / 逻辑删除 4 类操作。</li>
 *   <li>头-行对 {@code ErpMdPartner}(头) → {@code ErpMdPartnerAddress}(行)：
 *       行实体 {@code partnerId} 外键引用头实体主键，覆盖关系导航操作。</li>
 * </ul>
 *
 * <p>落位约定（Phase 1 Decision）：测试置于 {@code -service} 模块，继承
 * {@link JunitAutoTestCase}，输入置于测试类相对 {@code _cases/}，快照经
 * RECORDING 录制并人工审查后切 CHECKING。
 *
 * <p>多步 ID 传递：ERP 实体主键使用 {@code tagSet="seq-default"}（不等同平台
 * 自动变量标记 {@code seq}），不会被快照框架自动收集为 {@code @var} 变量。
 * 故多步测试在 Java 内从响应中取出 id 后传入后续步骤。CHECKING 模式下每方法
 * 从 {@code input/tables/} 恢复完整库状态（含 {@code nop_sys_sequence}），
 * 故 id 在录制与校验间稳定一致。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdPartnerCrudSmoke extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreatePartner() {
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save",
                request("request.json5", Map.class));

        assertEquals(0, result.getStatus());
        assertNotNull(((Map<?, ?>) result.getData()).get("id"), "新建应返回非空 ID");

        output("response.json5", result);
    }

    @Test
    public void testQueryPartner() {
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save",
                request("1_save.json5", Map.class));
        output("1_save_response.json5", created);

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpMdPartner__findPage",
                request("2_findPage.json5", Map.class));

        assertEquals(0, page.getStatus());
        Map<?, ?> data = (Map<?, ?>) page.getData();
        assertTrue(((Number) data.get("total")).intValue() >= 1, "查询应至少返回刚创建的 1 条");

        output("response.json5", page);
    }

    @Test
    public void testUpdatePartner() {
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save",
                request("1_save.json5", Map.class));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("id", id);
        updateData.put("name", "冒烟-已修改");
        ApiResponse<?> updated = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__update",
                ApiRequest.build(Map.of("data", updateData)));

        assertEquals(0, updated.getStatus());
        assertEquals("冒烟-已修改", ((Map<?, ?>) updated.getData()).get("name"), "编辑保存后名称应与输入一致");

        output("2_update_response.json5", updated);
    }

    @Test
    public void testDeletePartner() {
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save",
                request("1_save.json5", Map.class));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        ApiResponse<?> deleted = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__delete",
                ApiRequest.build(Map.of("id", id)));
        assertEquals(0, deleted.getStatus());

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpMdPartner__findPage",
                request("3_findPage.json5", Map.class));

        assertEquals(0, page.getStatus());
        Map<?, ?> data = (Map<?, ?>) page.getData();
        assertEquals(0, ((Number) data.get("total")).intValue(), "逻辑删除后查询应返回 0 条");

        output("response.json5", page);
    }

    @Test
    public void testPartnerAddressRelation() {
        ApiResponse<?> partner = executeRpc(GraphQLOperationType.mutation, "ErpMdPartner__save",
                request("1_savePartner.json5", Map.class));
        output("1_savePartner_response.json5", partner);
        String partnerId = String.valueOf(((Map<?, ?>) partner.getData()).get("id"));

        Map<String, Object> addressData = new LinkedHashMap<>();
        addressData.put("partnerId", partnerId);
        addressData.put("addressType", "BILLING");
        addressData.put("address", "冒烟测试-关系导航地址行");
        ApiResponse<?> address = executeRpc(GraphQLOperationType.mutation, "ErpMdPartnerAddress__save",
                ApiRequest.build(Map.of("data", addressData)));
        assertEquals(0, address.getStatus());
        output("2_saveAddress_response.json5", address);

        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("filter_partnerId", partnerId);
        queryData.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpMdPartnerAddress__findPage",
                ApiRequest.build(queryData));

        assertEquals(0, page.getStatus());
        Map<?, ?> data = (Map<?, ?>) page.getData();
        assertTrue(((Number) data.get("total")).intValue() >= 1, "按外键查询应至少返回 1 行");

        output("response.json5", page);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
