package app.erp.log.service.processor;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.LogFrozenClockExtension;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalOrder;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SALES_DELIVERY 交付状态回写 sales 集成测试（RC-R1.85，P1-RC-087，UC-LOG-06 步骤 5）。
 *
 * <p>D3 裁决（直接 Facade）：logistics 经 {@code IErpSalDeliveryBiz}（按 relatedBillCode 解析出库单→orderId）
 * + {@code IErpSalOrderBiz.updateDeliveryStatus} 回写源订单 {@code deliveryStatus=DELIVERED}
 * （复用既有发货进度字段与动作，sales 侧零改动）；logistics→sales 单向 Java 边（矩阵 §2.4 登记）。
 *
 * <p>覆盖 4 组：回写成功（sales 侧状态断言 + 运费过账不受影响）/ 非 SALES_DELIVERY 零回写 /
 * sales 侧失败隔离（DELIVERED 仍成立 + 运费过账不受影响，含 throwingProxy 单元注入组）/
 * 幂等（重复 onDelivered 不重复回写）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogSalesDeliveryLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static LogFrozenClockExtension frozenClock = new LogFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 1);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    ErpLogShipmentHandleTrackingWebhookProcessor webhookProcessor;

    /** 组 1：SALES_DELIVERY 回写成功——订单 deliveryStatus=DELIVERED + DELIVERED 主迁移 + 运费过账（SETTLED + FREIGHT 凭证）。 */
    @Test
    public void testSalesDeliveryWriteBackSuccess() {
        String carrierCode = "MOCK-SAL-LK-CAR";
        String deliveryCode = "SAL-DLV-LK-001";
        ormTemplate.runInSession(s -> {
            seedFinancePrereqs();
            Long carrierId = seedCarrier(carrierCode).getId();
            seedSalesDelivery(deliveryCode, seedSalesOrder("SAL-ORD-LK-001"));
            seedDispatchedShipment("SHP-SAL-LK-1", "TRK-SAL-LK-1", carrierId,
                    ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, deliveryCode, new BigDecimal("150"));
            return null;
        });

        String payload = "{\"trackingNo\":\"TRK-SAL-LK-1\",\"eventType\":\"DELIVERED\",\"signedBy\":\"王五\"}";
        ErpLogShipment result = ormTemplate.runInSession(s ->
                shipmentBiz.handleTrackingWebhook(carrierCode, hmacSha256(payload, carrierCode), payload, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "运费过账不受回写影响（SETTLED）");
        ErpSalOrder order = findOrder("SAL-ORD-LK-001");
        assertEquals(ErpLogConstants.SALES_DELIVERY_STATUS_DELIVERED, order.getDeliveryStatus(),
                "sales 订单发货进度应回写为 DELIVERED");
        assertTrue(!findBillLinks("SHP-SAL-LK-1").isEmpty(), "FREIGHT 凭证回链已落库");
    }

    /** 组 2：非 SALES_DELIVERY（PURCHASE_RECEIPT）零回写——同名单据存在也不触及 sales 订单。 */
    @Test
    public void testNonSalesDeliveryZeroWriteBack() {
        String carrierCode = "MOCK-SAL-LK-CAR2";
        String sharedCode = "SHARED-BILL-LK-002";
        ormTemplate.runInSession(s -> {
            seedFinancePrereqs();
            Long carrierId = seedCarrier(carrierCode).getId();
            seedSalesDelivery(sharedCode, seedSalesOrder("SAL-ORD-LK-002"));
            seedDispatchedShipment("SHP-SAL-LK-2", "TRK-SAL-LK-2", carrierId,
                    ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT, sharedCode, new BigDecimal("80"));
            return null;
        });

        String payload = "{\"trackingNo\":\"TRK-SAL-LK-2\",\"eventType\":\"DELIVERED\"}";
        ErpLogShipment result = ormTemplate.runInSession(s ->
                shipmentBiz.handleTrackingWebhook(carrierCode, hmacSha256(payload, carrierCode), payload, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        ErpSalOrder order = findOrder("SAL-ORD-LK-002");
        assertEquals("UNDELIVERED", order.getDeliveryStatus(),
                "PURCHASE_RECEIPT 路径零 sales 回写（同名单据存在也不触及）");
    }

    /** 组 3：sales 侧失败隔离——出库单缺失（查询空）时 DELIVERED 仍成立 + 运费过账不受影响。 */
    @Test
    public void testSalesSideFailureIsolated() {
        String carrierCode = "MOCK-SAL-LK-CAR3";
        ormTemplate.runInSession(s -> {
            seedFinancePrereqs();
            Long carrierId = seedCarrier(carrierCode).getId();
            seedDispatchedShipment("SHP-SAL-LK-3", "TRK-SAL-LK-3", carrierId,
                    ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "SAL-DLV-NOT-EXIST", new BigDecimal("120"));
            return null;
        });

        String payload = "{\"trackingNo\":\"TRK-SAL-LK-3\",\"eventType\":\"DELIVERED\"}";
        ErpLogShipment result = assertDoesNotThrow(() -> ormTemplate.runInSession(s ->
                        shipmentBiz.handleTrackingWebhook(carrierCode, hmacSha256(payload, carrierCode), payload, CTX)),
                "sales 侧异常/缺失应被隔离，不阻断 DELIVERED");

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "运费过账不受 sales 失败影响");
        assertTrue(!findBillLinks("SHP-SAL-LK-3").isEmpty(), "FREIGHT 凭证仍正常生成");
    }

    /** 组 3 补充（纯单元）：sales Facade 抛异常时 onDelivered 不向调用方传播（降级）。 */
    @Test
    public void testSalesFacadeThrowIsolatedUnit() {
        ErpLogShipmentScanForPollingProcessor processor = new ErpLogShipmentScanForPollingProcessor();
        processor.salDeliveryBiz = FaultInjectionStubs.throwingProxy(IErpSalDeliveryBiz.class, "findFirst",
                FaultInjectionStubs.testFault("test.sales-facade-down"));
        processor.salOrderBiz = FaultInjectionStubs.throwingProxy(IErpSalOrderBiz.class, "updateDeliveryStatus",
                FaultInjectionStubs.testFault("test.sales-facade-down"));

        ErpLogShipment shipment = new ErpLogShipment();
        shipment.setCode("SHP-SAL-FAIL-1");
        shipment.setRelatedBillType(ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY);
        shipment.setRelatedBillCode("SAL-DLV-FAIL-1");
        shipment.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_PENDING);
        shipment.setFreightAmount(new BigDecimal("150"));
        shipment.setOrgId(null);
        shipment.setCarrierId(null);

        assertDoesNotThrow(() -> processor.onDelivered(shipment, new ServiceContextImpl()),
                "sales Facade 异常应被隔离（降级不阻断）");
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_PENDING, shipment.getFreightSettlementStatus(),
                "回写失败不影响既有失败语义（保持 PENDING，不误置 SETTLED）");
    }

    /** 组 4：幂等——重复 webhook 幂等短路 + 已 SETTLED 重放 onDelivered 抛既有守卫，sales 订单不重复回写。 */
    @Test
    public void testRepeatOnDeliveredNoDoubleWriteBack() {
        String carrierCode = "MOCK-SAL-LK-CAR4";
        String deliveryCode = "SAL-DLV-LK-004";
        ormTemplate.runInSession(s -> {
            seedFinancePrereqs();
            Long carrierId = seedCarrier(carrierCode).getId();
            seedSalesDelivery(deliveryCode, seedSalesOrder("SAL-ORD-LK-004"));
            seedDispatchedShipment("SHP-SAL-LK-4", "TRK-SAL-LK-4", carrierId,
                    ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, deliveryCode, new BigDecimal("90"));
            return null;
        });

        String payload = "{\"trackingNo\":\"TRK-SAL-LK-4\",\"eventType\":\"DELIVERED\"}";
        ErpLogShipment result = ormTemplate.runInSession(s ->
                shipmentBiz.handleTrackingWebhook(carrierCode, hmacSha256(payload, carrierCode), payload, CTX));
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus());

        ErpSalOrder afterFirst = findOrder("SAL-ORD-LK-004");
        assertEquals(ErpLogConstants.SALES_DELIVERY_STATUS_DELIVERED, afterFirst.getDeliveryStatus());
        int versionAfterFirst = afterFirst.getVersion();

        // 重复 webhook DELIVERED：advanceTracking 幂等短路，不重复触发 onDelivered
        ErpLogShipment replay = ormTemplate.runInSession(s ->
                shipmentBiz.handleTrackingWebhook(carrierCode, hmacSha256(payload, carrierCode), payload, CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, replay.getStatus(), "重复 webhook 幂等短路");

        // 直接重放 onDelivered（SETTLED 幂等守卫抛既有错误码，回写不重复执行）
        ErpLogShipment settled = ormTemplate.runInSession(s ->
                daoProvider.daoFor(ErpLogShipment.class).getEntityById(replay.getId()));
        io.nop.api.core.exceptions.NopException ex = org.junit.jupiter.api.Assertions.assertThrows(
                io.nop.api.core.exceptions.NopException.class,
                () -> ormTemplate.runInSession(s -> {
                    webhookProcessor.onDelivered(settled, CTX);
                    return null;
                }));
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_ALREADY_DELIVERED.getErrorCode(), ex.getErrorCode(),
                "已 SETTLED 重复 onDelivered 抛既有幂等守卫错误码");

        ErpSalOrder afterRepeat = findOrder("SAL-ORD-LK-004");
        assertEquals(ErpLogConstants.SALES_DELIVERY_STATUS_DELIVERED, afterRepeat.getDeliveryStatus());
        assertNotEquals(-1, afterRepeat.getVersion());
        assertTrue(afterRepeat.getVersion() <= versionAfterFirst + 1,
                "重复 onDelivered 不重复回写（version 最多容纳审计刷新，无业务回写重放）");
    }

    // ---------- seed helpers ----------

    private ErpSalOrder findOrder(String code) {
        return ormTemplate.runInSession(s -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", code));
            return daoProvider.daoFor(ErpSalOrder.class).findFirstByQuery(q);
        });
    }

    private Long seedSalesOrder(String code) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(1L);
        order.setCustomerId(8801L);
        order.setBusinessDate(BUSINESS_DATE);
        order.setCurrencyId(1L);
        order.setDocStatus("DRAFT");
        order.setApproveStatus("APPROVED");
        order.setDeliveryStatus("UNDELIVERED");
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        return order.getId();
    }

    private void seedSalesDelivery(String code, Long orderId) {
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode(code);
        delivery.setOrgId(1L);
        delivery.setOrderId(orderId);
        delivery.setCustomerId(8801L);
        delivery.setWarehouseId(1L);
        delivery.setBusinessDate(BUSINESS_DATE);
        delivery.setCurrencyId(1L);
        delivery.setDocStatus("DRAFT");
        delivery.setApproveStatus("APPROVED");
        daoProvider.daoFor(ErpSalDelivery.class).saveEntity(delivery);
    }

    private void seedDispatchedShipment(String code, String trackingNo, Long carrierId,
                                        String relatedBillType, String relatedBillCode, BigDecimal freight) {
        ErpLogShipment s = new ErpLogShipment();
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(carrierId);
        s.setStatus(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
        s.setTrackingNo(trackingNo);
        s.setRelatedBillType(relatedBillType);
        s.setRelatedBillCode(relatedBillCode);
        s.setFreightTerms(ErpLogConstants.FREIGHT_TERMS_PREPAID);
        s.setFreightAmount(freight);
        s.setFreightCurrencyId(1L);
        s.setFreightSettlementStatus(ErpLogConstants.SETTLEMENT_STATUS_PENDING);
        s.setBusinessDate(BUSINESS_DATE);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
    }

    private ErpLogCarrier seedCarrier(String code) {
        ErpLogCarrier c = new ErpLogCarrier();
        c.setCode(code);
        c.setCarrierName("回写测试承运商");
        c.setCarrierType("EXPRESS");
        c.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
        c.setPartnerId(8801L);
        c.setIsActive(1);
        daoProvider.daoFor(ErpLogCarrier.class).saveEntity(c);
        return c;
    }

    private void seedFinancePrereqs() {
        IEntityDao<app.erp.md.dao.entity.ErpMdOrganization> orgDao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdOrganization.class);
        if (orgDao.getEntityById(1L) == null) {
            app.erp.md.dao.entity.ErpMdOrganization org = new app.erp.md.dao.entity.ErpMdOrganization();
            org.setId(1L);
            org.setCode("ORG-1");
            org.setName("测试组织");
            org.setOrgType("COMPANY");
            org.setStatus("ACTIVE");
            orgDao.saveEntity(org);
        }
        IEntityDao<app.erp.md.dao.entity.ErpMdCurrency> curDao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class);
        if (curDao.getEntityById(1L) == null) {
            app.erp.md.dao.entity.ErpMdCurrency cur = new app.erp.md.dao.entity.ErpMdCurrency();
            cur.setId(1L);
            cur.setCode("CNY");
            cur.setName("人民币");
            curDao.saveEntity(cur);
        }
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        seedSubject("6601", "销售费用", "EXPENSE", "DEBIT");
        seedSubject("1002", "银行存款", "ASSET", "DEBIT");
        seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        app.erp.md.dao.entity.ErpMdSubject subject = new app.erp.md.dao.entity.ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class).saveEntity(subject);
    }

    private void seedAcctSchema(long orgId) {
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus("OPEN");
        daoProvider.daoFor(ErpFinAccountingPeriod.class).saveEntity(period);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode) {
        return ormTemplate.runInSession(s -> {
            QueryBean q = new QueryBean();
            q.addFilter(and(eq("billCode", billCode),
                    eq("businessType", ErpFinBusinessType.FREIGHT.name())));
            return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        });
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
