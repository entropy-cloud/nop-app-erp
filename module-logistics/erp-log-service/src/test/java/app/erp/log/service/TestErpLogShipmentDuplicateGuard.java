package app.erp.log.service;

import app.erp.log.dao.entity.ErpLogShipment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 重复发运防护守卫测试（RC-R1.83，P1-RC-083，UC-LOG-01 正常②/异常「重复发运」）。
 *
 * <p>验证 {@code ErpLogShipmentBizModel.defaultPrepareSave} 的 relatedBillType+relatedBillCode
 * 非 CANCELLED 重复守卫：同出库单二次保存拒绝（错误码 + 出库单标识）、CANCELLED 后再建放行、
 * 无 relatedBill（手工发运）放行、仅 1 条非 CANCELLED 发运单。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogShipmentDuplicateGuard extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;

    /** 组 1：同出库单二次保存拒绝（错误码 + 出库单号断言 + 仅 1 条发运单）。 */
    @Test
    public void testDuplicateRelatedBillRejected() {
        Long carrierId = seedCarrier();

        Map<String, Object> d1 = shipmentData("SHP-083-1", "TRK-083-1", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-083-001");
        ApiResponse<?> first = save(d1);
        assertEquals(0, first.getStatus(), "首次创建应成功: " + first);

        Map<String, Object> d2 = shipmentData("SHP-083-2", "TRK-083-2", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-083-001");
        ApiResponse<?> second = save(d2);
        assertTrue(second.getStatus() != 0, "同出库单重复发运应被拒绝");
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_RELATED_BILL_DUPLICATE.getErrorCode(), second.getCode(),
                "应抛重复发运防护专用错误码");
        assertTrue(String.valueOf(second.getMsg()).contains("REL-BILL-083-001"),
                "错误消息应携带出库单号: " + second.getMsg());

        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillCode", "REL-BILL-083-001"));
        long count = daoProvider.daoFor(ErpLogShipment.class).findAllByQuery(q).size();
        assertEquals(1L, count, "同出库单仅 1 条发运单（无重复行）");
    }

    /** 组 2：既有发运单 CANCELLED 后再建放行（新 code，关联原出库单）。 */
    @Test
    public void testCancelledShipmentAllowsRecreate() {
        Long carrierId = seedCarrier();

        Map<String, Object> d1 = shipmentData("SHP-083-CX-1", "TRK-083-CX-1", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-083-CX");
        d1.put("status", ErpLogConstants.SHIPMENT_STATUS_CANCELLED);
        assertEquals(0, save(d1).getStatus(), "CANCELLED 发运单创建应成功");

        Map<String, Object> d2 = shipmentData("SHP-083-CX-2", "TRK-083-CX-2", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-083-CX");
        ApiResponse<?> second = save(d2);
        assertEquals(0, second.getStatus(), "CANCELLED 不阻断新建（state-machine.md §3 重新发运语义）: " + second);
    }

    /** 组 3：无 relatedBill（手工发运）不触发守卫，可多条创建。 */
    @Test
    public void testNoRelatedBillNotConstrained() {
        Long carrierId = seedCarrier();

        Map<String, Object> d1 = shipmentData("SHP-083-MAN-1", "TRK-083-MAN-1", carrierId, null, null);
        Map<String, Object> d2 = shipmentData("SHP-083-MAN-2", "TRK-083-MAN-2", carrierId, null, null);
        assertEquals(0, save(d1).getStatus(), "手工发运（无 relatedBill）首次创建应成功");
        assertEquals(0, save(d2).getStatus(), "手工发运（无 relatedBill）再次创建应成功（守卫不触发）");
    }

    /** 组 4：不同 relatedBillCode（或仅 type/code 其一为空）互不阻断。 */
    @Test
    public void testDifferentRelatedBillNotBlocked() {
        Long carrierId = seedCarrier();

        Map<String, Object> d1 = shipmentData("SHP-083-DIFF-1", "TRK-083-DIFF-1", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-083-A");
        Map<String, Object> d2 = shipmentData("SHP-083-DIFF-2", "TRK-083-DIFF-2", carrierId,
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT, "REL-BILL-083-A");
        Map<String, Object> d3 = shipmentData("SHP-083-DIFF-3", "TRK-083-DIFF-3", carrierId,
                null, "REL-BILL-083-A");
        assertEquals(0, save(d1).getStatus(), "首个出库单发运创建应成功");
        assertEquals(0, save(d2).getStatus(), "不同 relatedBillType 相同单号不阻断（维度为 type+code 联合）");
        assertEquals(0, save(d3).getStatus(), "relatedBillType 为空不触发守卫（维度不完整）");
    }

    // ---------- seed helpers ----------

    private Long seedCarrier() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", "CAR-083-" + System.nanoTime());
        d.put("carrierName", "测试承运商");
        d.put("carrierType", "EXPRESS");
        d.put("gatewayId", "1");
        d.put("isActive", 1);
        ApiResponse<?> resp = executeMutation("ErpLogCarrier__save", d);
        assertNotNull(resp.getData(), "承运商创建应成功: " + resp);
        return Long.valueOf(String.valueOf(((Map<?, ?>) resp.getData()).get("id")));
    }

    private Map<String, Object> shipmentData(String code, String trackingNo, Long carrierId,
                                             String relatedBillType, String relatedBillCode) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("carrierId", carrierId);
        d.put("status", ErpLogConstants.SHIPMENT_STATUS_DRAFT);
        d.put("trackingNo", trackingNo);
        if (relatedBillType != null) {
            d.put("relatedBillType", relatedBillType);
        }
        if (relatedBillCode != null) {
            d.put("relatedBillCode", relatedBillCode);
        }
        return d;
    }

    private ApiResponse<?> save(Map<String, Object> data) {
        return executeMutation("ErpLogShipment__save", data);
    }

    private ApiResponse<?> executeMutation(String action, Map<String, Object> data) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("data", data);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(GraphQLOperationType.mutation, action,
                ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
