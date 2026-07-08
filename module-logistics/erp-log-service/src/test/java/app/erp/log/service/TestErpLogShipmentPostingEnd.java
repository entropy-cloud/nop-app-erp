package app.erp.log.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogShipmentPostingEnd extends JunitAutoTestCase {

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
    public void testSalesDeliveryFullLifecycleWithFreightVoucher() {
        long partnerId = 8901L;
        String carrierCode = "MOCK-END-SAL";
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier(carrierCode, partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("END-SAL-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DRAFT,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.FREIGHT_TERMS_PREPAID, new BigDecimal("150"),
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        ErpLogShipment afterAdvise = shipmentBiz.advise(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, afterAdvise.getStatus());

        ErpLogShipment afterComplete = shipmentBiz.completeShipment(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, afterComplete.getStatus());
        assertNotNull(afterComplete.getTrackingNo());

        String trackingNo = afterComplete.getTrackingNo();
        String payload = "{\"trackingNo\":\"" + trackingNo + "\",\"eventType\":\"DELIVERED\",\"signedBy\":\"张三\"}";
        String sig = hmacSha256(payload, carrierCode);
        ErpLogShipment result = shipmentBiz.handleTrackingWebhook(carrierCode, sig, payload, CTX);

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus());
        assertTrue(!findBillLinks("END-SAL-1").isEmpty());
    }

    @Test
    public void testPurchaseReceiptFullLifecycleNoFreightVoucher() {
        long partnerId = 8902L;
        String carrierCode = "MOCK-END-PUR";
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier(carrierCode, partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("END-PUR-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DRAFT,
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT,
                ErpLogConstants.FREIGHT_TERMS_COLLECT, new BigDecimal("300"),
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        ErpLogShipment afterAdvise = shipmentBiz.advise(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_ADVISED, afterAdvise.getStatus());

        ErpLogShipment afterComplete = shipmentBiz.completeShipment(shipmentId, CTX);
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, afterComplete.getStatus());
        assertNotNull(afterComplete.getTrackingNo());

        String trackingNo = afterComplete.getTrackingNo();
        String payload = "{\"trackingNo\":\"" + trackingNo + "\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, carrierCode);
        ErpLogShipment result = shipmentBiz.handleTrackingWebhook(carrierCode, sig, payload, CTX);

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus());
        assertTrue(findBillLinks("END-PUR-1").isEmpty());
    }

    private void seedFinancePrereqs() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        seedSubject("6601", "销售费用", "EXPENSE", "DEBIT");
        seedSubject("1002", "银行存款", "ASSET", "DEBIT");
        seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
    }

    private Long seedCarrier(String code, long partnerId) {
        IEntityDao<ErpLogCarrier> dao = daoProvider.daoFor(ErpLogCarrier.class);
        ErpLogCarrier carrier = new ErpLogCarrier();
        carrier.setCode(code);
        carrier.setCarrierName("Mock 承运商");
        carrier.setCarrierType("EXPRESS");
        carrier.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
        carrier.setPartnerId(partnerId);
        carrier.setIsActive(1);
        dao.saveEntity(carrier);
        return carrier.getId();
    }

    private Long seedShipment(String code, Long carrierId, String status, String relatedBillType,
                              String freightTerms, BigDecimal freightAmount, String settlementStatus) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(carrierId);
        s.setStatus(status);
        s.setRelatedBillType(relatedBillType);
        s.setFreightTerms(freightTerms);
        s.setFreightAmount(freightAmount);
        s.setFreightCurrencyId(1L);
        s.setFreightSettlementStatus(settlementStatus);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        return s.getId();
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus("OPEN");
        dao.saveEntity(period);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode),
                eq("businessType", ErpFinBusinessType.FREIGHT.name())));
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
