package app.erp.log.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import app.erp.log.dao.entity.ErpLogShipment;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发运单 trackingNo 唯一性测试（plan 2026-07-30-0841-2 R1.28 P1-MA2-092）。
 *
 * <p>验证 {@code ErpLogShipmentBizModel.defaultPrepareSave} 的应用层前置校验 +
 * DB {@code UK_LOG_SHIPMENT_TRACKING_CARRIER} 兜底：同 trackingNo+carrierId 的重复创建被拒绝，
 * 抛 {@link ErpLogErrors#ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE}（友好错误码，非 ERR_ORM_DATA_EXCEPTION），
 * 且仅 1 条发运单（无重复实体行）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogShipmentTrackingNoUk extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testDuplicateTrackingNoRejected() {
        Long carrierId = seedCarrier();

        // 首次创建 trackingNo=TRK-092 成功
        Map<String, Object> data1 = shipmentData("SHP-092-1", "TRK-092", carrierId);
        ApiResponse<?> first = save(data1);
        assertEquals(0, first.getStatus(), "首次创建应成功: " + first);

        // 同 trackingNo+carrierId 再次创建 → 前置校验抛 ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE
        Map<String, Object> data2 = shipmentData("SHP-092-2", "TRK-092", carrierId);
        ApiResponse<?> second = save(data2);
        assertTrue(second.getStatus() != 0, "重复 trackingNo 应被拒绝");
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE.getErrorCode(), second.getCode(),
                "应抛友好错误码（非 ERR_ORM_DATA_EXCEPTION）");

        // 仅 1 条发运单（无重复实体行）
        QueryBean q = new QueryBean();
        q.addFilter(eq("trackingNo", "TRK-092"));
        long count = daoProvider.daoFor(ErpLogShipment.class).findAllByQuery(q).size();
        assertEquals(1L, count, "重复 trackingNo 仅 1 条发运单（无重复）");
    }

    @Test
    public void testNullTrackingNoNotConstrained() {
        Long carrierId = seedCarrier();
        // trackingNo 为 null 的发运单不受 UK 约束（H2 NULLS DISTINCT），可创建多条
        Map<String, Object> d1 = shipmentData("SHP-092-NULL-1", null, carrierId);
        Map<String, Object> d2 = shipmentData("SHP-092-NULL-2", null, carrierId);
        assertEquals(0, save(d1).getStatus(), "null trackingNo 首次创建应成功");
        assertEquals(0, save(d2).getStatus(), "null trackingNo 再次创建应成功（UK 不约束 null）");
    }

    private Long seedCarrier() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", "CAR-092-" + System.nanoTime());
        d.put("carrierName", "测试承运商");
        d.put("carrierType", "EXPRESS");
        d.put("gatewayId", "1");
        d.put("isActive", 1);
        ApiResponse<?> resp = executeMutation("ErpLogCarrier__save", d);
        return Long.valueOf(String.valueOf(((Map<?, ?>) resp.getData()).get("id")));
    }

    private Map<String, Object> shipmentData(String code, String trackingNo, Long carrierId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("carrierId", carrierId);
        d.put("status", "DRAFT");
        if (trackingNo != null) {
            d.put("trackingNo", trackingNo);
        }
        return d;
    }

    private ApiResponse<?> save(Map<String, Object> data) {
        return executeMutation("ErpLogShipment__save", data);
    }

    private ApiResponse<?> executeMutation(String action, Map<String, Object> data) {
        java.util.Map<String, Object> args = new LinkedHashMap<>();
        args.put("data", data);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(GraphQLOperationType.mutation, action,
                ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
