package app.erp.log.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物流产费过账行为测试（Phase 5，3.18 path-1/path-2）。
 *
 * <ul>
 *   <li>SALES_DELIVERY 运单 DELIVERED→FREIGHT 凭证 + freightSettlementStatus SETTLED；</li>
 *   <li>重复 DELIVERED 幂等抛 ERR_LOG_SHIPMENT_ALREADY_DELIVERED；</li>
 *   <li>PURCHASE_RECEIPT 运单仅发事件不出凭证（path-2 Landed Cost Deferred）。</li>
 * </ul>
 *
 * <p>触发路径：seed DISPATCHED 运单（含 trackingNo），webhook DELIVERED 事件推进 DISPATCHED→DELIVERED 并触发 onDelivered。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogFreightPosting extends JunitAutoTestCase {

    @RegisterExtension
    static LogFrozenClockExtension frozenClock = new LogFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    /**
     * A4.2.174 探针语义翻转（RC-R1.83，P1-RC-083 修复后回归）：
     * 同一出库单（relatedBillType+relatedBillCode 相同）第二笔发运单经 BizModel save 路径被
     * {@code defaultPrepareSave} 重复发运守卫拒绝 → 仅 1 条发运单可 DELIVERED → 仅 1 张 FREIGHT 凭证
     * （原探针证实双发运单双凭证重复过账；守卫落地后路径收敛为单凭证）。
     */
    @Test
    public void testDuplicateShipmentSameRelatedBillSecondRejectedSingleVoucher() {
        long partnerId = 8804L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-FRT-CAR", partnerId);
        });

        // 同 relatedBillType+relatedBillCode 创建 2 条发运单：第二笔被重复发运守卫拒绝
        Map<String, Object> d1 = shipmentData("FRT-DUP-1", "MOCK-FRT-DUP-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-DUP-001",
                ErpLogConstants.FREIGHT_TERMS_PREPAID, new BigDecimal("100"));
        Map<String, Object> d2 = shipmentData("FRT-DUP-2", "MOCK-FRT-DUP-2", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED,
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY, "REL-BILL-DUP-001",
                ErpLogConstants.FREIGHT_TERMS_PREPAID, new BigDecimal("120"));
        ApiResponse<?> first = executeMutation("ErpLogShipment__save", d1);
        ApiResponse<?> second = executeMutation("ErpLogShipment__save", d2);
        assertEquals(0, first.getStatus(), "首次创建应成功: " + first);
        assertTrue(second.getStatus() != 0, "同出库单重复发运应被拒绝: " + second);
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_RELATED_BILL_DUPLICATE.getErrorCode(), second.getCode(),
                "应抛重复发运防护专用错误码");

        // 首笔 shipment webhook DELIVERED → 单凭证路径保持
        String payload1 = "{\"trackingNo\":\"MOCK-FRT-DUP-1\",\"eventType\":\"DELIVERED\"}";
        String sig1 = hmacSha256(payload1, "MOCK-FRT-CAR");
        ErpLogShipment r1 = ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-FRT-CAR", sig1, payload1, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, r1.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, r1.getFreightSettlementStatus());

        // 凭证数断言：该出库单仅 1 条发运单 → 仅 1 张 FREIGHT 凭证（重复过账路径被守卫切断）
        List<ErpFinVoucherBillR> links1 = findBillLinks("FRT-DUP-1");
        List<ErpFinVoucherBillR> links2 = findBillLinks("FRT-DUP-2");
        assertEquals(1, links1.size(), "发运单 FRT-DUP-1 DELIVERED 应产生 1 张 FREIGHT 凭证");
        assertEquals(0, links2.size(), "发运单 FRT-DUP-2 被守卫拒绝，不应存在 FREIGHT 凭证");
    }

    @Test
    public void testSalesFreightPostedAndSettled() {
        long partnerId = 8801L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-FRT-CAR", partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("FRT-SAL-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-FRT-SAL-1",
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.FREIGHT_TERMS_PREPAID, new BigDecimal("150"),
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        String payload = "{\"trackingNo\":\"MOCK-FRT-SAL-1\",\"eventType\":\"DELIVERED\",\"signedBy\":\"李四\"}";
        String sig = hmacSha256(payload, "MOCK-FRT-CAR");
        ErpLogShipment result = ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-FRT-CAR", sig, payload, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "运费结算状态应为 SETTLED");

        // FREIGHT 凭证经业财回链可查
        assertTrue(!findBillLinks("FRT-SAL-1").isEmpty(), "FREIGHT 凭证回链已落库");
    }

    @Test
    public void testDuplicateDeliveredIdempotentThrows() {
        long partnerId = 8802L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-FRT-CAR", partnerId);
        });
        // 模拟已结算的运单（DISPATCHED + SETTLED），webhook DELIVERED 推进触发 onDelivered 时已 SETTLED → 抛错
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("FRT-IDEM-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-FRT-IDEM-1",
                ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY,
                ErpLogConstants.FREIGHT_TERMS_PREPAID, new BigDecimal("200"),
                ErpLogConstants.SETTLEMENT_STATUS_SETTLED));

        String payload = "{\"trackingNo\":\"MOCK-FRT-IDEM-1\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, "MOCK-FRT-CAR");
        // DISPATCHED→DELIVERED 推进成功 → onDelivered 见 SETTLED → 抛 ERR_LOG_SHIPMENT_ALREADY_DELIVERED
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-FRT-CAR", sig, payload, CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_SHIPMENT_ALREADY_DELIVERED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testPurchaseReceiptNoVoucher() {
        long partnerId = 8803L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-FRT-CAR", partnerId);
        });
        Long shipmentId = ormTemplate.runInSession(session -> seedShipment("FRT-PUR-1", carrierId,
                ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, "MOCK-FRT-PUR-1",
                ErpLogConstants.RELATED_BILL_TYPE_PURCHASE_RECEIPT,
                ErpLogConstants.FREIGHT_TERMS_COLLECT, new BigDecimal("300"),
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));

        String payload = "{\"trackingNo\":\"MOCK-FRT-PUR-1\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, "MOCK-FRT-CAR");
        ErpLogShipment result = ormTemplate.runInSession(session -> shipmentBiz.handleTrackingWebhook("MOCK-FRT-CAR", sig, payload, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, result.getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, result.getFreightSettlementStatus(),
                "path-2 事件交接后 settlement 标记 SETTLED（事件占位）");
        // PURCHASE_RECEIPT 不出 FREIGHT 凭证（path-2 仅事件交接，Landed Cost 归 Deferred）
        assertTrue(findBillLinks("FRT-PUR-1").isEmpty(), "PURCHASE_RECEIPT 运单不应生成 FREIGHT 凭证");
    }

    // ---------- seed helpers ----------

    private Map<String, Object> shipmentData(String code, String trackingNo, Long carrierId, String status,
                                             String relatedBillType, String relatedBillCode, String freightTerms,
                                             BigDecimal freightAmount) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("carrierId", carrierId);
        d.put("status", status);
        d.put("trackingNo", trackingNo);
        d.put("relatedBillType", relatedBillType);
        d.put("relatedBillCode", relatedBillCode);
        d.put("freightTerms", freightTerms);
        d.put("freightAmount", freightAmount);
        d.put("freightCurrencyId", 1L);
        d.put("orgId", 1L);
        d.put("businessDate", java.time.LocalDate.of(2026, 7, 1));
        return d;
    }

    private ApiResponse<?> executeMutation(String action, Map<String, Object> data) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("data", data);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(GraphQLOperationType.mutation, action,
                ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedFinancePrereqs() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        seedSubject("6601", "销售费用", "EXPENSE", "DEBIT");
        seedSubject("1002", "银行存款", "ASSET", "DEBIT");
        seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
        // GraphQL save 路径会校验 to-one 引用，需组织/币种存在（id=1 与 orgId/freightCurrencyId 对应）
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

    private Long seedShipment(String code, Long carrierId, String status, String trackingNo,
                              String relatedBillType, String freightTerms, BigDecimal freightAmount,
                              String settlementStatus) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(carrierId);
        s.setStatus(status);
        s.setTrackingNo(trackingNo);
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
