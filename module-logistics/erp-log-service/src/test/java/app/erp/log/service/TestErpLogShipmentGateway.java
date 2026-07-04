package app.erp.log.service;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.dao.entity.ErpLogShipmentLog;
import app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 承运商网关全链行为测试（Phase 5）。mock 承运商覆盖：
 * <ul>
 *   <li>状态机 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 全路径 + trackingNo/labelUrl 回写；</li>
 *   <li>webhook HMAC 签名校验（通过/失败）+ 幂等；</li>
 *   <li>5xx 重试至死信（保留 ADVISED + remark 错误）；4xx 不重试直接死信；</li>
 *   <li>取消（ADVISED→CANCELLED）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogShipmentGateway extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;

    @BeforeEach
    void resetMock() {
        // 重试间隔置 0 避免拖慢测试
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_RETRY_BASE_INTERVAL_SECS, "0,0,0");
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_GATEWAY_MAX_RETRIES, 2);
        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_SUCCESS;
    }

    @Test
    public void testFullStateMachineFlow() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-FULL-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DRAFT, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        // DRAFT→ADVISED
        ErpLogShipment afterAdvise = shipmentBiz.advise(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, afterAdvise.getStatus());

        // ADVISED→DISPATCHED（completeDeliveryOrder 回写 trackingNo/labelUrl）
        ErpLogShipment afterComplete = shipmentBiz.completeShipment(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, afterComplete.getStatus());
        assertEquals("MOCK-GW-FULL-1", afterComplete.getTrackingNo());
        assertNotNull(afterComplete.getLabelUrl());
        assertTrue(afterComplete.getLabelUrl().contains("MOCK-GW-FULL-1"));

        // 轮询推进 DISPATCHED→IN_TRANSIT（mock 首次 trackShipment 返回 IN_TRANSIT）
        int advanced1 = shipmentBiz.scanForPolling(CTX);
        assertTrue(advanced1 >= 1);
        ErpLogShipment inTransit = reload(shipmentId);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, inTransit.getStatus());

        // 再次轮询 IN_TRANSIT→DELIVERED（mock 第二次 trackShipment 返回 DELIVERED）
        shipmentBiz.scanForPolling(CTX);
        ErpLogShipment delivered = reload(shipmentId);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, delivered.getStatus());
        assertNotNull(delivered.getActualDeliveryDate());

        // 网关日志已落库
        assertTrue(!findLogs(shipmentId).isEmpty(), "网关交互日志应落库");
    }

    @Test
    public void testGateway5xxRetryDeadLetter() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-RETRY-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_5XX;
        ErpLogShipment result = shipmentBiz.completeShipment(shipmentId, CTX);

        // 死信：保留 ADVISED + remark 错误
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, result.getStatus());
        assertNotNull(result.getRemark());
        assertTrue(result.getRemark().contains("网关重试耗尽"), "remark 应含死信标记");

        // 失败日志落库
        List<ErpLogShipmentLog> logs = findLogs(shipmentId);
        assertTrue(logs.stream().anyMatch(l -> !Boolean.TRUE.equals(l.getIsSuccess())), "应有失败网关日志");
    }

    @Test
    public void testGateway4xxNoRetryDeadLetter() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-RETRY-2", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_4XX;
        ErpLogShipment result = shipmentBiz.completeShipment(shipmentId, CTX);

        // 4xx 不重试直接死信：保留 ADVISED
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, result.getStatus());
        assertNotNull(result.getRemark());
        assertTrue(result.getRemark().contains("不可重试"), "remark 应含 4xx 不可重试标记");
    }

    @Test
    public void testWebhookInvalidSignatureRejected() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-WH-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        // 设置 trackingNo（DISPATCHED 运单应有 trackingNo）
        ormTemplate.runInSession(session -> {
            ErpLogShipment s = reload(shipmentId);
            s.setTrackingNo("MOCK-GW-WH-1");
            daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(s);
            return null;
        });

        String payload = "{\"trackingNo\":\"MOCK-GW-WH-1\",\"eventType\":\"IN_TRANSIT\"}";
        // 错误签名
        assertThrows(NopException.class,
                () -> shipmentBiz.handleTrackingWebhook("MOCK-CAR", "badsignature", payload, CTX));
    }

    @Test
    public void testWebhookIdempotentDuplicate() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-WH-2", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        ormTemplate.runInSession(session -> {
            ErpLogShipment s = reload(shipmentId);
            s.setTrackingNo("MOCK-GW-WH-2");
            daoProvider.daoFor(ErpLogShipment.class).saveOrUpdateEntity(s);
            return null;
        });

        String payload = "{\"trackingNo\":\"MOCK-GW-WH-2\",\"eventType\":\"IN_TRANSIT\"}";
        String sig = hmacSha256(payload, "MOCK-CAR");
        shipmentBiz.handleTrackingWebhook("MOCK-CAR", sig, payload, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(shipmentId).getStatus());

        // 重复回调不重复推进（仍 IN_TRANSIT，不报错）
        shipmentBiz.handleTrackingWebhook("MOCK-CAR", sig, payload, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(shipmentId).getStatus());
    }

    @Test
    public void testCancelShipment() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-CNL-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        ErpLogShipment result = shipmentBiz.cancelShipment(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_CANCELLED, result.getStatus());
    }

    // ---------- helpers ----------

    private Long seedCarrier(String code) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpLogCarrier> dao = daoProvider.daoFor(ErpLogCarrier.class);
            ErpLogCarrier carrier = new ErpLogCarrier();
            carrier.setCode(code);
            carrier.setCarrierName("Mock 承运商");
            carrier.setCarrierType("EXPRESS");
            carrier.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
            carrier.setIsActive(1);
            dao.saveEntity(carrier);
            return carrier.getId();
        });
    }

    private Long seedShipment(String code, Long carrierId, String status, String relatedBillType,
                              String settlementStatus) {
        ErpLogShipment s = new ErpLogShipment();
        s.setCode(code);
        s.setCarrierId(carrierId);
        s.setStatus(status);
        s.setRelatedBillType(relatedBillType);
        s.setFreightSettlementStatus(settlementStatus);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        return s.getId();
    }

    private ErpLogShipment reload(Long shipmentId) {
        return daoProvider.daoFor(ErpLogShipment.class).getEntityById(shipmentId);
    }

    private List<ErpLogShipmentLog> findLogs(Long shipmentId) {
        IEntityDao<ErpLogShipmentLog> dao = daoProvider.daoFor(ErpLogShipmentLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("shipmentId", shipmentId));
        return dao.findAllByQuery(q);
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
