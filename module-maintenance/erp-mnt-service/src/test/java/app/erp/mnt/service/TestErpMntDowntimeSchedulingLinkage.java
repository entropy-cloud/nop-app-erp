package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 停机→排产联动发布侧测试（RC-R1.76 / P1-RC-068 / UC-MAIN-06，plan 2026-08-19-0445-3 Phase 2 Proof；
 * mfg 消费侧门控见 mfg-service {@code TestErpMfgJobCardDowntimeGate}）。
 *
 * <p>覆盖：
 * ① record → 设备 DOWN + notify 7208 落库（USER_LIST 模板断言接收人；ROLE 生产计划员模板无角色数据
 *    config-gated 空投递断言）② complete → RUNNING + notify 7209 落库 ③ config
 * {@code erp-mnt.downtime-notify-enabled} 关闭零通知 ④ Facade 开放窗口查询数学断言
 * （开放/已完/无工作中心/设备非 DOWN 四态）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntDowntimeSchedulingLinkage extends JunitAutoTestCase {

    static final Long EQUIPMENT_ID = 701L;
    static final Long WORKCENTER_ID = 7101L;
    static final String RECIPIENT = "mnt-planner-user";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(600000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- ① record → DOWN + 7208 落库（USER_LIST） + ROLE 空投递 ----------

    @Test
    public void testRecordNotifiesPlannerAndSetsDown() {
        seedEquipment(EQUIPMENT_ID, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        seedNotifyTemplate(7301L, ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME, "USER_LIST", RECIPIENT);
        Long downtimeId = seedDowntime(nextId(), EQUIPMENT_ID, LocalDateTime.now().minusMinutes(10));

        ApiResponse<?> resp = rpc(mutation, "ErpMntDowntimeEntry__record", Map.of("downtimeId", downtimeId));
        assertEquals(0, resp.getStatus(), "record 应成功: " + resp);
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, equipmentStatus(),
                "record 设备置 DOWN");

        List<ErpSysNotification> notifications = findNotifications(
                ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME);
        assertEquals(1, notifications.size(), "7208 通知应落库（USER_LIST 模板）");
        assertEquals(RECIPIENT, notifications.get(0).getRecipientUserId(), "接收人匹配模板 USER_LIST");
    }

    @Test
    public void testRecordWithRoleTemplateNoRoleDataEmptyDelivery() {
        seedEquipment(EQUIPMENT_ID, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        // 部署 seed 7208 同款 ROLE 生产计划员模板；测试库无 nop_auth_role 角色数据 → config-gated 空投递
        seedNotifyTemplate(7302L, ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME, "ROLE", null);
        Long downtimeId = seedDowntime(nextId(), EQUIPMENT_ID, LocalDateTime.now().minusMinutes(10));

        ApiResponse<?> resp = rpc(mutation, "ErpMntDowntimeEntry__record", Map.of("downtimeId", downtimeId));
        assertEquals(0, resp.getStatus(), "record 应成功（空投递不阻断主流程）: " + resp);
        assertEquals(0, findNotifications(ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME).size(),
                "无角色数据 → ROLE 解析空 → 零 ErpSysNotification 行");
    }

    // ---------- ② complete → RUNNING + 7209 落库 ----------

    @Test
    public void testCompleteNotifiesRecoveredAndRestoresRunning() {
        seedEquipment(EQUIPMENT_ID, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        seedNotifyTemplate(7303L, ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_RECOVERED, "USER_LIST", RECIPIENT);
        Long downtimeId = seedDowntime(nextId(), EQUIPMENT_ID, LocalDateTime.now().minusHours(1));

        assertEquals(0, rpc(mutation, "ErpMntDowntimeEntry__record",
                Map.of("downtimeId", downtimeId)).getStatus());
        assertEquals(0, rpc(mutation, "ErpMntDowntimeEntry__complete",
                Map.of("downtimeId", downtimeId)).getStatus(), "complete 应成功");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "complete 恢复设备 RUNNING");

        List<ErpSysNotification> notifications = findNotifications(
                ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_RECOVERED);
        assertEquals(1, notifications.size(), "7209 通知应落库");
        assertEquals(RECIPIENT, notifications.get(0).getRecipientUserId());
    }

    // ---------- ③ config 关闭零通知 ----------

    @Test
    public void testNotifyDisabledSkipsDispatch() {
        AppConfig.getConfigProvider().assignConfigValue(ErpMntConstants.CONFIG_DOWNTIME_NOTIFY_ENABLED, "false");
        try {
            seedEquipment(EQUIPMENT_ID, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedNotifyTemplate(7304L, ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME, "USER_LIST", RECIPIENT);
            Long downtimeId = seedDowntime(nextId(), EQUIPMENT_ID, LocalDateTime.now().minusMinutes(10));

            assertEquals(0, rpc(mutation, "ErpMntDowntimeEntry__record",
                    Map.of("downtimeId", downtimeId)).getStatus());
            assertEquals(0, findNotifications(ErpMntConstants.NOTIFY_EVENT_EQUIPMENT_DOWNTIME).size(),
                    "config 关闭 → 零通知（停机主流程不受影响）");
            assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, equipmentStatus(),
                    "设备状态联动不受通知开关影响（独立 config）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpMntConstants.CONFIG_DOWNTIME_NOTIFY_ENABLED, "true");
        }
    }

    // ---------- ④ 开放窗口查询数学断言（四态） ----------

    @Test
    public void testFindOpenDowntimeEquipmentWorkcenters() {
        Long eqOpen = 801L;        // 开放：endTime null + DOWN + 工作中心映射 → 出窗
        Long eqDone = 802L;        // 已完：endTime 已置 → 不出窗
        Long eqNoWc = 803L;        // 无工作中心 → 不出窗
        Long eqNotDown = 804L;     // 设备非 DOWN（RUNNING）→ 不出窗
        seedEquipment(eqOpen, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
        seedEquipment(eqDone, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
        seedEquipment(eqNoWc, null, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
        seedEquipment(eqNotDown, WORKCENTER_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        seedDowntime(nextId(), eqOpen, LocalDateTime.now().minusHours(2));
        Long doneId = seedDowntime(nextId(), eqDone, LocalDateTime.now().minusHours(3));
        seedDowntime(nextId(), eqNoWc, LocalDateTime.now().minusHours(1));
        seedDowntime(nextId(), eqNotDown, LocalDateTime.now().minusHours(1));
        // eqDone 补 endTime（已完）——独立会话加载已提交种子行后回写
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
            dao.getEntityById(doneId).setEndTime(Timestamp.valueOf(LocalDateTime.now().minusMinutes(30)));
        });

        ApiResponse<?> resp = rpc(query, "ErpMntDowntimeEntry__findOpenDowntimeEquipmentWorkcenters",
                Map.of());
        assertEquals(0, resp.getStatus(), "查询应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windows = (List<Map<String, Object>>) resp.getData();

        assertEquals(1, windows.size(), "仅开放窗口出窗（已完/无工作中心/设备非 DOWN 均排除）");
        Map<String, Object> window = windows.get(0);
        assertEquals(eqOpen, toLong(window.get("equipmentId")));
        assertEquals(WORKCENTER_ID, toLong(window.get("workcenterId")), "经 equipment.workcenterId 桥接");
        assertNotNull(window.get("startTime"), "窗口含起始时间");
    }

    // ---------- helpers ----------

    private void seedEquipment(Long id, Long workcenterId, String status) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
            ErpMntEquipment equipment = new ErpMntEquipment();
            equipment.setId(id);
            equipment.setCode("EQ-" + id);
            equipment.setName("设备" + id);
            equipment.setWorkcenterId(workcenterId);
            equipment.setStatus(status);
            dao.saveEntity(equipment);
        });
    }

    /** startTime 为 ORM mandatory 列，种子恒带起始时间（record 不再兜底覆盖）。 */
    private Long seedDowntime(Long id, Long equipmentId, LocalDateTime startTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
            ErpMntDowntimeEntry downtime = new ErpMntDowntimeEntry();
            downtime.setId(id);
            downtime.setEquipmentId(equipmentId);
            downtime.setStartTime(Timestamp.valueOf(startTime));
            downtime.setReason("DT-LINK");
            dao.saveEntity(downtime);
        });
        return id;
    }

    private void seedNotifyTemplate(Long id, String eventType, String resolver, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(eventType);
            t.setName("停机排产通知");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("设备停机: ${equipmentCode}");
            t.setBodyTpl("设备 ${equipmentCode}（工作中心 ${workcenterId}）停机，原因 ${reason}");
            t.setRecipientResolver(resolver);
            t.setRecipientConfig("USER_LIST".equals(resolver)
                    ? "{\"userIds\":[\"" + recipientUserId + "\"]}"
                    : "{\"roles\":[\"生产计划员\"]}");
            t.setMergeWindowSeconds(300);
            t.setMergeStrategy("MERGE_BY_USER_TYPE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private List<ErpSysNotification> findNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private String equipmentStatus() {
        return daoProvider.daoFor(ErpMntEquipment.class).getEntityById(EQUIPMENT_ID).getStatus();
    }

    private static Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
