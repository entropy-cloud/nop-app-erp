package io.nop.app.all.it;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.graphql.GraphQLRequestBean;
import io.nop.api.core.beans.graphql.GraphQLResponseBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.6 最小落地集 item 1（`ai-native-interface.md` 前置调研结论）：introspection config-gate 验证——
 * 开启 {@code nop.graphql.schema-introspection.enabled} 后 IntrospectionQuery 可用且含 description
 * （AI 工具发现的 schema 面范本）。默认关闭的反向断言见 {@link TestErpAiIntrospectionDisabledByDefault}。
 *
 * <p>交叉验证 item 2：Query 字段 {@code ErpFinDashboard__getDashboardKpi} 经 {@code @Description}
 * 补全后 description 非空（AI 只读消费面范本）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.graphql.schema-introspection.enabled", value = "true")
public class TestErpAiIntrospectionEnabled extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testIntrospectionQueryReturnsSchemaWithDescriptions() {
        // 最小形状：queryType + 类型清单（introspection 元字段含带默认值参数的深层形状触发平台
        // AST 单父约束限制，见 ai-native-interface.md 实现注记）
        GraphQLResponseBean response = execute("{__schema{queryType{name} types{name kind description}}}");
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertNotNull(data, "开启 introspection 后 __schema 应可用: " + response.getErrors());

        Map<String, Object> schema = (Map<String, Object>) data.get("__schema");
        assertNotNull(schema);
        assertEquals("Query", ((Map<String, Object>) schema.get("queryType")).get("name"));

        // 实体类型可发现（orm/xmeta 富覆盖的发现面）
        List<Map<String, Object>> types = (List<Map<String, Object>>) schema.get("types");
        assertTrue(types.stream().anyMatch(t -> "ErpMdPartner".equals(t.get("name"))),
                "ErpMdPartner 类型应可发现");

        // item 2 交叉验证：action description 可发现（getDashboardKpi @Description 补全范本）。
        // 经引擎 schema 定义 API 断言（introspection 元字段 __Type.fields 携带默认值参数，当前平台
        // 版本经查询语句解析会触发 AST 单父约束限制——登记于 ai-native-interface.md 实现注记；
        // schema 定义 API 与 introspection 查询共享同一元数据源，GraphQLToolProvider 同源）
        io.nop.graphql.core.ast.GraphQLFieldDefinition kpiAction =
                graphQLEngine.getOperationDefinition(io.nop.graphql.core.ast.GraphQLOperationType.query,
                        "ErpFinDashboard__getDashboardKpi");
        assertNotNull(kpiAction, "getDashboardKpi 应在 Query 操作中可发现");
        assertNotNull(kpiAction.getDescription(), "action description 应非空（@Description 补全）");
        assertTrue(kpiAction.getDescription().contains("财务看板"));
    }

    private GraphQLResponseBean execute(String query) {
        GraphQLRequestBean request = new GraphQLRequestBean();
        request.setQuery(query);
        return graphQLEngine.executeGraphQL(graphQLEngine.newGraphQLContext(request));
    }
}
