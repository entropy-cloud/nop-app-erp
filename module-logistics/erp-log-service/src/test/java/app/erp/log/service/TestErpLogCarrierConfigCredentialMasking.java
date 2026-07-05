package app.erp.log.service;

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
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * C-4 安全回归测试：承运商配置的敏感凭证字段（apiKey/apiSecret/credentials）经保留层 XMeta
 * published=false + queryable=false + sortable=false 屏蔽后，GraphQL findPage 不得返回其明文。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogCarrierConfigCredentialMasking extends JunitAutoTestCase {

    private static final String SECRET_API_KEY = "SECRET-API-KEY-12345";
    private static final String SECRET_API_SECRET = "SECRET-API-SECRET-67890";
    private static final String SECRET_CREDENTIALS = "{\"token\":\"SECRET-CREDS\"}";

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCredentialsNotExposedViaFindPage() {
        String carrierId = createCarrier();

        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("carrierId", carrierId);
        cfg.put("configCode", "SEC-MASK");
        cfg.put("apiKey", SECRET_API_KEY);
        cfg.put("apiSecret", SECRET_API_SECRET);
        cfg.put("credentials", SECRET_CREDENTIALS);
        cfg.put("isActive", 1);
        ApiResponse<?> saved = executeRpc(GraphQLOperationType.mutation, "ErpLogCarrierConfig__save",
                ApiRequest.build(Map.of("data", cfg)));
        assertEquals(0, saved.getStatus(), "保存承运商配置应成功");

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpLogCarrierConfig__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        output("response.json5", page);

        String body = String.valueOf(page);
        assertFalse(body.contains(SECRET_API_KEY), "apiKey 明文不得出现在 findPage 响应中");
        assertFalse(body.contains(SECRET_API_SECRET), "apiSecret 明文不得出现在 findPage 响应中");
        assertFalse(body.contains("SECRET-CREDS"), "credentials 明文不得出现在 findPage 响应中");
    }

    private String createCarrier() {
        Map<String, Object> carrier = new LinkedHashMap<>();
        carrier.put("code", "MASK-CAR");
        carrier.put("carrierName", "凭证屏蔽测试承运商");
        carrier.put("carrierType", "EXPRESS");
        carrier.put("gatewayId", "1");
        carrier.put("isActive", 1);
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpLogCarrier__save",
                ApiRequest.build(Map.of("data", carrier)));
        assertEquals(0, resp.getStatus());
        return String.valueOf(((Map<?, ?>) resp.getData()).get("id"));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
