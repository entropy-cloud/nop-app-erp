package app.erp.log.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.job.ErpLogTrackingPollJob;
import app.erp.log.service.spi.mock.MockCarrierGatewayClientFactory;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 追踪轮询兜底 Job 测试（RC-R1.38，P1-RC-085，UC-LOG-03「定时轮询间隔可配置（默认 4 小时）」）。
 *
 * <p>覆盖：调度路径调 scanForPolling 推进多运单 DISPATCHED→IN_TRANSIT→DELIVERED + onDelivered 运费过账
 * 联动（freightSettlementStatus SETTLED + FREIGHT 凭证回链）；cron 空值跳过；单条 onDelivered 失败隔离
 * （已 SETTLED 运单触发 ERR_LOG_SHIPMENT_ALREADY_DELIVERED，LOG.error 隔离不中断其余运单）。
 *
 * <p>手工装配 Job bean（镜像 TestErpLogDraftEscalationJob 范式）。mock 网关 trackShipment 按调用次数
 * 确定性推进（首次 IN_TRANSIT，其后 DELIVERED），两轮 execute() 走完 DISPATCHED→DELIVERED。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogTrackingPollJob extends JunitAutoTestCase {

    @RegisterExtension
    static LogFrozenClockExtension frozenClock = new LogFrozenClockExtension();

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
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_TRACKING_POLLING_CRON, "");
        MockCarrierGatewayClientFactory.failureMode = MockCarrierGatewayClientFactory.FAILURE_MODE_SUCCESS;
    }

    private ErpLogTrackingPollJob newWiredJob() {
        ErpLogTrackingPollJob job = new ErpLogTrackingPollJob();
        job.setShipmentBiz(shipmentBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    // ---------- ① 调度路径推进多运单 + 运费过账联动 ----------

    @Test
    public void testJobAdvancesMultipleShipmentsWithFreightPosting() {
        long partnerId = 8851L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-POLL-CAR", partnerId);
        });
        Long sh1 = ormTemplate.runInSession(session -> seedShipmentWithFreight("POLL-1", carrierId, "MOCK-POLL-1",
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        Long sh2 = ormTemplate.runInSession(session -> seedShipmentWithFreight("POLL-2", carrierId, "MOCK-POLL-2",
                ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        setCron("0 0 */4 * * ?");

        // 第一轮：DISPATCHED → IN_TRANSIT
        newWiredJob().execute();
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(sh1).getStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, reload(sh2).getStatus());

        // 第二轮：IN_TRANSIT → DELIVERED + onDelivered 运费过账联动
        newWiredJob().execute();
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(sh1).getStatus());
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(sh2).getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, reload(sh1).getFreightSettlementStatus(),
                "DELIVERED 后运费过账联动应标记 SETTLED");
        assertNotNull(reload(sh1).getActualDeliveryDate());
        assertTrue(!findBillLinks("POLL-1").isEmpty(), "FREIGHT 凭证回链应落库");
        assertTrue(!findBillLinks("POLL-2").isEmpty(), "FREIGHT 凭证回链应落库");
    }

    // ---------- ② cron 空值跳过 ----------

    @Test
    public void testCronEmptySkipsScan() {
        long partnerId = 8852L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-POLL-CAR", partnerId);
        });
        Long sh1 = ormTemplate.runInSession(session -> seedShipmentWithFreight("POLL-SKIP-1", carrierId,
                "MOCK-POLL-SKIP-1", ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        setCron("");

        newWiredJob().execute();

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, reload(sh1).getStatus(),
                "cron 空时不应触发 scanForPolling");
    }

    // ---------- ③ 单条 onDelivered 失败隔离（已 SETTLED 触发幂等码，LOG.error 隔离不中断） ----------

    @Test
    public void testSingleOnDeliveredFailureIsolated() {
        long partnerId = 8853L;
        Long carrierId = ormTemplate.runInSession(session -> {
            seedFinancePrereqs();
            return seedCarrier("MOCK-POLL-CAR", partnerId);
        });
        // 异常单：已 SETTLED（DELIVERED 后 onDelivered 抛 ERR_LOG_SHIPMENT_ALREADY_DELIVERED）
        Long bad = ormTemplate.runInSession(session -> seedShipmentWithFreight("POLL-BAD-1", carrierId,
                "MOCK-POLL-BAD-1", ErpLogConstants.SETTLEMENT_STATUS_SETTLED));
        // 正常单
        Long good = ormTemplate.runInSession(session -> seedShipmentWithFreight("POLL-GOOD-1", carrierId,
                "MOCK-POLL-GOOD-1", ErpLogConstants.SETTLEMENT_STATUS_PENDING));
        setCron("0 0 */4 * * ?");

        // 两轮推进；单条 onDelivered 失败不应中断（execute 顶层 + Processor 逐条双保险）
        newWiredJob().execute();
        newWiredJob().execute();

        // 异常单：状态仍推进 DELIVERED（推进与过账分离），结算状态保持 SETTLED 不被覆盖
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(bad).getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, reload(bad).getFreightSettlementStatus());
        // 正常单：DELIVERED + SETTLED + 凭证回链
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, reload(good).getStatus());
        assertEquals(ErpLogConstants.SETTLEMENT_STATUS_SETTLED, reload(good).getFreightSettlementStatus());
        assertTrue(!findBillLinks("POLL-GOOD-1").isEmpty(), "正常单 FREIGHT 凭证回链应落库");
    }

    // ---------- helpers ----------

    private void setCron(String cron) {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_TRACKING_POLLING_CRON, cron);
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

    private Long seedShipmentWithFreight(String code, Long carrierId, String trackingNo, String settlementStatus) {
        ErpLogShipment s = new ErpLogShipment();
        s.setBusinessDate(LocalDate.of(2026, 7, 1));
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(carrierId);
        s.setStatus(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
        s.setTrackingNo(trackingNo);
        s.setRelatedBillType(ErpLogConstants.RELATED_BILL_TYPE_SALES_DELIVERY);
        s.setFreightTerms(ErpLogConstants.FREIGHT_TERMS_PREPAID);
        s.setFreightAmount(new BigDecimal("150"));
        s.setFreightCurrencyId(1L);
        s.setFreightSettlementStatus(settlementStatus);
        daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        return s.getId();
    }

    private ErpLogShipment reload(Long shipmentId) {
        return daoProvider.daoFor(ErpLogShipment.class).getEntityById(shipmentId);
    }

    private void seedFinancePrereqs() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        seedSubject("6601", "销售费用", "EXPENSE", "DEBIT");
        seedSubject("1002", "银行存款", "ASSET", "DEBIT");
        seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
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
}
