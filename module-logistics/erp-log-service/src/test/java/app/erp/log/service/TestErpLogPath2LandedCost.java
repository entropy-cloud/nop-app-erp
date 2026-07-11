package app.erp.log.service;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory;
import app.erp.pur.dao.entity.ErpPurReceive;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * path-2 运费→到岸成本自动编排集成测试（plan 2026-07-11-2329-1 Phase 3）。
 *
 * <p>config-gated {@code erp-log.path2-landed-cost-auto-create=true} 时，PURCHASE_RECEIPT 运单 DELIVERED
 * 自动调 {@code IErpInvLandedCostBiz.generateFreightLandedCost} 创建 DRAFT 到岸成本单（FREIGHT 费用行）。
 *
 * <ul>
 *   <li>正路径：config=true + freightAmount>0 → LandedCost DRAFT 创建 + SETTLED</li>
 *   <li>跳过路径：config=true + freightAmount=null → SETTLED + 无 LandedCost</li>
 *   <li>幂等路径：重复 DELIVERED → 抛 ERR_LOG_SHIPMENT_ALREADY_DELIVERED</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogPath2LandedCost extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;

    @BeforeEach
    void setUp() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpLogConfigs.CONFIG_RETRY_BASE_INTERVAL_SECS, "0,0,0");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpLogConfigs.CONFIG_GATEWAY_MAX_RETRIES, 2);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpLogConstants.CONFIG_PATH2_LANDED_COST_AUTO_CREATE, true);
        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_SUCCESS;
    }

    @Test
    public void testPath2AutoCreateLandedCost() {
        long partnerId = 9901L;
        String receiveCode = "RCV-PATH2-A";
        Long carrierId = ormTemplate.runInSession(session -> {
            seedPurchaseReceive(receiveCode, partnerId);
            return seedCarrier("MOCK-PATH2-A", partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("SHP-PATH2-A", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-PATH2-A-T",
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT,
                ErpLogConstants.FREIGHT_TERMS_COLLECT, new BigDecimal("350"),
                ErpLogConstants.SETTLEMENT_STATUS_PENDING, receiveCode));

        String payload = "{\"trackingNo\":\"MOCK-PATH2-A-T\",\"eventType\":\"DELIVERED\",\"signedBy\":\"王五\"}";
        String sig = hmacSha256(payload, "MOCK-PATH2-A");
        ErpLogShipment result = shipmentBiz.handleTrackingWebhook("MOCK-PATH2-A", sig, payload, CTX);

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "path-2 自动创建后 settlement 标记 SETTLED");

        ErpInvLandedCost landedCost = findLandedCostByReceiveCode(receiveCode);
        assertNotNull(landedCost, "DRAFT 到岸成本单应已创建");
        assertEquals("DRAFT", landedCost.getDocStatus(), "到岸成本单状态应为 DRAFT");
        assertEquals("UNSUBMITTED", landedCost.getApproveStatus(), "审核状态应为 UNSUBMITTED");
        assertTrue(new BigDecimal("350").compareTo(landedCost.getTotalCostAmount()) == 0,
                "运费金额应匹配");
        assertEquals("BY_AMOUNT", landedCost.getAllocationMethod(), "默认分摊方法 BY_AMOUNT");

        ErpInvLandedCostLine line = findFreightLine(landedCost.getId());
        assertNotNull(line, "FREIGHT 费用行应已创建");
        assertEquals("FREIGHT", line.getCostElement());
        assertTrue(new BigDecimal("350").compareTo(line.getAmount()) == 0, "费用行金额应匹配");
        assertEquals(partnerId, line.getApPartnerId(), "AP partner 默认取 receive.supplierId");
    }

    @Test
    public void testPath2SkipWhenFreightAmountNull() {
        long partnerId = 9902L;
        String receiveCode = "RCV-PATH2-B";
        Long carrierId = ormTemplate.runInSession(session -> {
            seedPurchaseReceive(receiveCode, partnerId);
            return seedCarrier("MOCK-PATH2-B", partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("SHP-PATH2-B", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-PATH2-B-T",
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT,
                ErpLogConstants.FREIGHT_TERMS_COLLECT, null,
                ErpLogConstants.SETTLEMENT_STATUS_PENDING, receiveCode));

        String payload = "{\"trackingNo\":\"MOCK-PATH2-B-T\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, "MOCK-PATH2-B");
        ErpLogShipment result = shipmentBiz.handleTrackingWebhook("MOCK-PATH2-B", sig, payload, CTX);

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "freightAmount=null 时标记 SETTLED（无可分摊运费）");
        assertNull(findLandedCostByReceiveCode(receiveCode), "freightAmount=null 不应创建到岸成本单");
    }

    @Test
    public void testPath2IdempotentRejectsDuplicateDelivered() {
        long partnerId = 9903L;
        String receiveCode = "RCV-PATH2-C";
        Long carrierId = ormTemplate.runInSession(session -> {
            seedPurchaseReceive(receiveCode, partnerId);
            return seedCarrier("MOCK-PATH2-C", partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("SHP-PATH2-C", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-PATH2-C-T",
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT,
                ErpLogConstants.FREIGHT_TERMS_COLLECT, new BigDecimal("200"),
                ErpLogConstants.SETTLEMENT_STATUS_SETTLED, receiveCode));

        String payload = "{\"trackingNo\":\"MOCK-PATH2-C-T\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, "MOCK-PATH2-C");
        NopException ex = assertThrows(NopException.class,
                () -> shipmentBiz.handleTrackingWebhook("MOCK-PATH2-C", sig, payload, CTX));
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_ALREADY_DELIVERED.getErrorCode(), ex.getErrorCode());
    }

    // ---------- seed helpers ----------

    private Long seedCarrier(String code, long partnerId) {
        IEntityDao<ErpLogCarrier> dao = daoProvider.daoFor(ErpLogCarrier.class);
        ErpLogCarrier carrier = new ErpLogCarrier();
        carrier.setCode(code);
        carrier.setCarrierName("Mock 承运商 " + code);
        carrier.setCarrierType("EXPRESS");
        carrier.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
        carrier.setPartnerId(partnerId);
        carrier.setIsActive(1);
        dao.saveEntity(carrier);
        return carrier.getId();
    }

    private void seedPurchaseReceive(String code, long supplierId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setOrgId(1L);
        receive.setSupplierId(supplierId);
        receive.setWarehouseId(1L);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(1L);
        receive.setExchangeRate(BigDecimal.ONE);
        receive.setAmountSource(new BigDecimal("1000"));
        receive.setAmountFunctional(new BigDecimal("1000"));
        receive.setTotalAmount(new BigDecimal("1000"));
        receive.setReceiveStatus("RECEIVED");
        receive.setReceiveType("PURCHASE_ORDER");
        receive.setDocStatus("DONE");
        receive.setApproveStatus("APPROVED");
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private Long seedShipment(String code, Long carrierId, String status, String trackingNo,
                              String relatedBillType, String freightTerms, BigDecimal freightAmount,
                              String settlementStatus, String relatedBillCode) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(LocalDate.of(2026, 7, 1));
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(carrierId);
        s.setStatus(status);
        s.setTrackingNo(trackingNo);
        s.setRelatedBillType(relatedBillType);
        s.setRelatedBillCode(relatedBillCode);
        s.setFreightTerms(freightTerms);
        s.setFreightAmount(freightAmount);
        s.setFreightCurrencyId(1L);
        s.setFreightSettlementStatus(settlementStatus);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        return s.getId();
    }

    private ErpInvLandedCost findLandedCostByReceiveCode(String receiveCode) {
        IEntityDao<ErpPurReceive> receiveDao = daoProvider.daoFor(ErpPurReceive.class);
        QueryBean rq = new QueryBean();
        rq.addFilter(eq("code", receiveCode));
        ErpPurReceive receive = receiveDao.findFirstByQuery(rq);
        if (receive == null) {
            return null;
        }
        IEntityDao<ErpInvLandedCost> dao = daoProvider.daoFor(ErpInvLandedCost.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiveId", receive.getId()));
        q.addOrderField("code", true);
        return dao.findFirstByQuery(q);
    }

    private ErpInvLandedCostLine findFreightLine(Long landedCostId) {
        IEntityDao<ErpInvLandedCostLine> dao = daoProvider.daoFor(ErpInvLandedCostLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("landedCostId", landedCostId),
                eq("costElement", "FREIGHT")
        ));
        return dao.findFirstByQuery(q);
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
