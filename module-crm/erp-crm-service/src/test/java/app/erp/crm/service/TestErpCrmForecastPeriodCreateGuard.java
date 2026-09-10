package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-crm-022-r3 ForecastPeriod 初始态守卫回归：status 列无 ORM defaultValue，创建可携带
 * 任意 status（含 FROZEN 直建绕过 requireOpen）——defaultPrepareSave 强制初始态 OPEN。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmForecastPeriodCreateGuard extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testCreateForcesOpenStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "FP-GUARD-" + System.nanoTime());
        data.put("label", "初始态守卫期间");
        data.put("periodType", "MONTHLY");
        data.put("periodStart", "2026-07-01");
        data.put("periodEnd", "2026-07-31");
        // 携带 FROZEN 直建：应被创建路径守卫覆写为 OPEN（不再绕过 requireOpen 语义）
        data.put("status", "FROZEN");

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpCrmForecastPeriod__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "创建应成功: " + resp);
        String id = String.valueOf(((Map<?, ?>) resp.getData()).get("id"));

        ErpCrmForecastPeriod period = ormTemplate.runInSession(s ->
                daoProvider.daoFor(ErpCrmForecastPeriod.class).getEntityById(id));
        assertEquals(ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN, period.getStatus(),
                "创建路径应强制初始态 OPEN（P3-CK-crm-022-r3）");
    }

    @Test
    public void testFreezeStillReachableFromForcedOpen() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "FP-FRZ-" + System.nanoTime());
        data.put("label", "冻结链路期间");
        data.put("periodType", "MONTHLY");
        data.put("periodStart", "2026-08-01");
        data.put("periodEnd", "2026-08-31");
        data.put("status", "CLOSED");

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpCrmForecastPeriod__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "创建应成功: " + resp);
        String id = String.valueOf(((Map<?, ?>) resp.getData()).get("id"));

        // 创建被强制 OPEN → freeze（requireOpen）可达
        ApiResponse<?> freeze = rpc(GraphQLOperationType.mutation, "ErpCrmForecastPeriod__freeze",
                ApiRequest.build(Map.of("periodId", id)));
        assertTrue(freeze.getStatus() == 0, "强制 OPEN 后 freeze 应可达: " + freeze);

        ErpCrmForecastPeriod period = ormTemplate.runInSession(s ->
                daoProvider.daoFor(ErpCrmForecastPeriod.class).getEntityById(id));
        assertEquals(ErpCrmConstants.FORECAST_PERIOD_STATUS_FROZEN, period.getStatus(), "freeze 后 FROZEN");
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
