package app.erp.log.service;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogCarrierGatewayIntegration extends JunitAutoTestCase {

    @RegisterExtension
    static LogFrozenClockExtension frozenClock = new LogFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;

    @BeforeEach
    void resetMock() {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_RETRY_BASE_INTERVAL_SECS, "0,0,0");
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_GATEWAY_MAX_RETRIES, 2);
        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_SUCCESS;
    }

    @Test
    public void testPollingAdvancesMultipleShipments() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long sh1 = ormTemplate.runInSession(session -> seedShipmentWithTracking("GW-MULTI-1", carrierId, "MOCK-GW-MULTI-1"));
        Long sh2 = ormTemplate.runInSession(session -> seedShipmentWithTracking("GW-MULTI-2", carrierId, "MOCK-GW-MULTI-2"));

        int advanced1 = ormTemplate.runInSession(session -> shipmentBiz.scanForPolling(CTX));
        assertTrue(advanced1 >= 2);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(sh1).getStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(sh2).getStatus());

        ormTemplate.runInSession(() -> shipmentBiz.scanForPolling(CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(sh1).getStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(sh2).getStatus());
        assertNotNull(reload(sh1).getActualDeliveryDate());
        assertNotNull(reload(sh2).getActualDeliveryDate());
    }

    @Test
    public void testWebhookInTransitThenDelivered() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipmentWithTracking("GW-WH-IT-1", carrierId, "MOCK-GW-WH-IT-1"));

        String payload1 = "{\"trackingNo\":\"MOCK-GW-WH-IT-1\",\"eventType\":\"IN_TRANSIT\"}";
        String sig1 = hmacSha256(payload1, "MOCK-CAR");
        ErpLogShipment result1 = ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-CAR", sig1, payload1, CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, result1.getStatus());

        String payload2 = "{\"trackingNo\":\"MOCK-GW-WH-IT-1\",\"eventType\":\"DELIVERED\",\"signedBy\":\"王五\"}";
        String sig2 = hmacSha256(payload2, "MOCK-CAR");
        ErpLogShipment result2 = ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-CAR", sig2, payload2, CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result2.getStatus());
        assertNotNull(result2.getActualDeliveryDate());
    }

    @Test
    public void testCancelAdvisedShipment() {
        Long carrierId = seedCarrier("MOCK-CAR");
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("GW-CNL-ADV-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_ADVISED,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        ErpLogShipment result = ormTemplate.runInSession(session -> shipmentBiz.cancelShipment(shipmentId, CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_CANCELLED, result.getStatus());
    }

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

    private Long seedShipmentWithTracking(String code, Long carrierId, String trackingNo) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setCode(code);
        s.setCarrierId(carrierId);
        s.setStatus(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
        s.setTrackingNo(trackingNo);
        s.setRelatedBillType(ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY);
        s.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_PENDING);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        return s.getId();
    }

    private Long seedShipment(String code, Long carrierId, String status, String relatedBillType,
                              String settlementStatus) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
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
