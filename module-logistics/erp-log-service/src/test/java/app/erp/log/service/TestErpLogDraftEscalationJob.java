package app.erp.log.service;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.job.ErpLogDraftEscalationJob;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRAFT 发运单超阈值升级 Job 测试（RC-R1.37，P1-RC-084，UC-LOG-01「超过 24 小时未确认的 DRAFT 发运单触发升级通知」）。
 *
 * <p>覆盖：超阈值 DRAFT → 派发 log.draft-escalation 通知 + recipient==createdBy（D2 裁决
 * USER_LIST ${submitterUserId}）；未超阈值零动作；cron 空值跳过；单条失败隔离（异常单不阻断其余）；
 * 无 ACTIVE 模板静默跳过。
 *
 * <p>手工装配 Job bean（镜像 TestErpCtApprovalTimeoutJob.newWiredJob 范式——biz_* 代理 bean 的
 * lazy props 在测试容器按需创建时不赋值）。updateTime 以 {@code orm_disableAutoStamp(true)} +
 * 显式赋值 seed 旧时点（ORM 自动盖章会覆盖为 now）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogDraftEscalationJob extends JunitAutoTestCase {

    static final String SHIPPER_USER = "log-shipper-1";
    static final String SHIPPER_USER_2 = "log-shipper-2";
    static final String SHIPPER_USER_3 = "log-shipper-3";

    @RegisterExtension
    static LogFrozenClockExtension frozenClock = new LogFrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    app.erp.notify.biz.IErpSysNotificationBiz notificationBiz;

    @AfterEach
    public void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_CRON, "");
    }

    private ErpLogDraftEscalationJob newWiredJob() {
        ErpLogDraftEscalationJob job = new ErpLogDraftEscalationJob();
        job.setShipmentBiz(shipmentBiz);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    // ---------- ① 超阈值扫描 + 通知落库 + recipient==createdBy ----------

    @Test
    public void testOverThresholdDraftEscalatesToCreator() {
        seedTemplate(9201L, "{\"userIds\":[\"${submitterUserId}\"]}");
        seedShipment("DRAFT-ESC-1", SHIPPER_USER, oldTs());
        setCron("0 30 1 * * ?");

        newWiredJob().execute();

        List<ErpSysNotification> notifications = notificationsOf(SHIPPER_USER);
        assertEquals(1, notifications.size(), "超阈值 DRAFT 发运单应派发 1 条升级通知");
        ErpSysNotification n = notifications.get(0);
        assertEquals(ErpLogConstants.NOTIFY_EVENT_DRAFT_ESCALATION, n.getNotificationType());
        assertEquals(SHIPPER_USER, n.getRecipientUserId(), "接收人应为发货员 createdBy（D2 USER_LIST 插值）");
        assertEquals(ErpNotifyConstants.STATUS_SENT, n.getStatus());
    }

    // ---------- ② 未超阈值 → 零动作 ----------

    @Test
    public void testNotOverThresholdUntouched() {
        seedTemplate(9202L, "{\"userIds\":[\"${submitterUserId}\"]}");
        seedShipment("DRAFT-ESC-2", SHIPPER_USER, recentTs());
        setCron("0 30 1 * * ?");

        newWiredJob().execute();

        assertTrue(notificationsOf(SHIPPER_USER).isEmpty(), "未超阈值不应派发通知");
        ErpLogShipment shipment = daoProvider.daoFor(ErpLogShipment.class)
                .findFirstByQuery(eqQuery("code", "DRAFT-ESC-2"));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DRAFT, shipment.getStatus(), "未超阈值发运单状态不变");
    }

    // ---------- ③ cron 空值跳过 ----------

    @Test
    public void testCronEmptySkipsScan() {
        seedTemplate(9203L, "{\"userIds\":[\"${submitterUserId}\"]}");
        seedShipment("DRAFT-ESC-3", SHIPPER_USER, oldTs());
        setCron("");

        newWiredJob().execute();

        assertTrue(notificationsOf(SHIPPER_USER).isEmpty(), "cron 空时不应扫描派发");
    }

    // ---------- ④ 单条失败隔离（异常单不阻断其余） ----------

    @Test
    public void testSingleFailureIsolated() {
        seedTemplate(9204L, "{\"userIds\":[\"${submitterUserId}\"]}");
        seedShipment("DRAFT-ESC-4A", SHIPPER_USER, oldTs());
        seedShipment("DRAFT-ESC-4B", SHIPPER_USER_2, oldTs());
        seedShipment("DRAFT-ESC-4C", SHIPPER_USER_3, oldTs());
        setCron("0 30 1 * * ?");

        ErpLogDraftEscalationJob job = new ThrowingDraftEscalationJob("DRAFT-ESC-4B");
        job.setShipmentBiz(shipmentBiz);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        // 不应抛出（runDraftEscalation 逐条 try/catch 隔离）
        job.execute();

        assertEquals(1, notificationsOf(SHIPPER_USER).size(), "正常单 A 应派发");
        assertEquals(0, notificationsOf(SHIPPER_USER_2).size(), "异常单 B 跳过不派发");
        assertEquals(1, notificationsOf(SHIPPER_USER_3).size(), "正常单 C 不受阻断继续派发");
    }

    // ---------- ⑤ 无 ACTIVE 模板静默跳过 ----------

    @Test
    public void testNoActiveTemplateSilentSkip() {
        seedShipment("DRAFT-ESC-5", SHIPPER_USER, oldTs());
        setCron("0 30 1 * * ?");

        // 不应抛出（notify 无 ACTIVE 模板 config-gated 静默跳过）
        newWiredJob().execute();

        assertTrue(notificationsOf(SHIPPER_USER).isEmpty(), "无 ACTIVE 模板不应落库通知");
    }

    // ---------- ⑥ config 键绑定断言 ----------

    @Test
    public void testConfigKeysBinding() {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_HOURS, 48L);
        assertEquals(48L, AppConfig.var(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_HOURS,
                ErpLogConfigs.DEFAULT_DRAFT_ESCALATION_HOURS), "draft-escalation-hours 可经 AppConfig 绑定读写");
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_HOURS, 24L);
        assertEquals(24L, AppConfig.var(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_HOURS,
                ErpLogConfigs.DEFAULT_DRAFT_ESCALATION_HOURS));
    }

    // ---------- helpers ----------

    /** 测试专用子类：对指定 code 的运单 escalateShipment 抛异常，验证逐条失败隔离。 */
    static class ThrowingDraftEscalationJob extends ErpLogDraftEscalationJob {
        private final String failCode;

        ThrowingDraftEscalationJob(String failCode) {
            this.failCode = failCode;
        }

        @Override
        protected boolean escalateShipment(ErpLogShipment shipment, long timeoutHours, IServiceContext ctx) {
            if (failCode.equals(shipment.getCode())) {
                throw new IllegalStateException("simulated failure: " + shipment.getCode());
            }
            return super.escalateShipment(shipment, timeoutHours, ctx);
        }
    }

    private void setCron(String cron) {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_CRON, cron);
    }

    private Timestamp oldTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 100L * 3600_000L);
    }

    private Timestamp recentTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 1L * 3600_000L);
    }

    private void seedShipment(String code, String createdBy, Timestamp updateTime) {
        Long carrierId = seedCarrier("MOCK-ESC-CAR");
        ormTemplate.runInSession(() -> {
            ErpLogShipment s = daoProvider.daoFor(ErpLogShipment.class).newEntity();
            s.orm_disableAutoStamp(true);
            s.setCreatedBy(createdBy);
            s.setUpdatedBy(createdBy);
            s.setCreateTime(updateTime);
            s.setUpdateTime(updateTime);
            s.setCode(code);
            s.setCarrierId(carrierId);
            s.setStatus(ErpLogConstants.SHIPMENT_STATUS_DRAFT);
            s.setBusinessDate(LocalDate.of(2026, 7, 1));
            daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
        });
    }

    private Long seedCarrier(String code) {
        app.erp.log.dao.entity.ErpLogCarrier carrier = new app.erp.log.dao.entity.ErpLogCarrier();
        carrier.setCode(code);
        carrier.setCarrierName("Mock 承运商");
        carrier.setCarrierType("EXPRESS");
        carrier.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
        carrier.setIsActive(1);
        daoProvider.daoFor(app.erp.log.dao.entity.ErpLogCarrier.class).saveEntity(carrier);
        return carrier.getId();
    }

    private void seedTemplate(Long id, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpLogConstants.NOTIFY_EVENT_DRAFT_ESCALATION);
            t.setName("TPL-DRAFT-ESCALATION");
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("发运单 ${shipmentCode} 超时未确认");
            t.setBodyTpl("发运单 ${shipmentCode}（ID ${shipmentId}）已超过 ${elapsedHours} 小时未确认，请及时处理");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", ErpLogConstants.NOTIFY_EVENT_DRAFT_ESCALATION));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }
}
