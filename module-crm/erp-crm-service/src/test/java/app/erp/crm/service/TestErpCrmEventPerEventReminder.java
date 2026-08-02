package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmEvent;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * findDueReminders per-event reminderMinutesBefore 测试（R1.24 / P1-MA2-076）。
 *
 * <p>覆盖：(1) per-event reminderMinutesBefore=1440（提前 1 天）+ startDateTime=now+1天 → 命中
 * （旧全局 60min 窗口不命中，证明 per-event 生效）；(2) reminderMinutesBefore=null + startDateTime=now+30min →
 * 命中（fallback 全局 60min 行为不变）；(3) reminderMinutesBefore=15 + startDateTime=now+45min → 不命中（45>15）；
 * (4) PLANNED 守卫（COMPLETED 事件不计入）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmEventPerEventReminder extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testPerEventReminderLargerThanGlobalWindowHits() {
        LocalDateTime now = CoreMetrics.currentDateTime();
        ormTemplate.runInSession(() -> {
            // reminder=1440（1 天）+ startDateTime=now+1天：旧全局 60min 窗口不会命中，per-event 应命中
            seedEvent(4501L, "EVT-PER-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    now.plusMinutes(1440), 1440);
        });
        ApiResponse<?> resp = findDueReminders(60);
        assertEquals(0, resp.getStatus(), "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertEquals(1, list.size(), "per-event reminderMinutesBefore=1440 应使命中（全局 60min 窗口下本不命中）");
        Map<?, ?> first = (Map<?, ?>) list.get(0);
        assertEquals("EVT-PER-001", first.get("code"));
    }

    @Test
    public void testNullReminderFallsBackToGlobalWindow() {
        LocalDateTime now = CoreMetrics.currentDateTime();
        ormTemplate.runInSession(() -> {
            // reminder=null + startDateTime=now+30min：fallback 全局 60min → 命中（行为不变）
            seedEvent(4502L, "EVT-NULL-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    now.plusMinutes(30), null);
            // reminder=null + startDateTime=now+200min：fallback 全局 60min → 不命中
            seedEvent(4503L, "EVT-NULL-002", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    now.plusMinutes(200), null);
        });
        ApiResponse<?> resp = findDueReminders(60);
        assertEquals(0, resp.getStatus(), "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertEquals(1, list.size(), "null reminder fallback 全局 60min：仅 now+30min 命中，now+200min 不命中");
        Map<?, ?> hit = (Map<?, ?>) list.get(0);
        assertEquals("EVT-NULL-001", hit.get("code"));
    }

    @Test
    public void testPerEventReminderNarrowerExcludesFarEvent() {
        LocalDateTime now = CoreMetrics.currentDateTime();
        ormTemplate.runInSession(() -> {
            // reminder=15 + startDateTime=now+45min：45>15 → 不命中
            seedEvent(4504L, "EVT-NAR-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    now.plusMinutes(45), 15);
            // reminder=15 + startDateTime=now+10min：10<=15 → 命中（对照组，证明不是全排除）
            seedEvent(4505L, "EVT-NAR-002", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    now.plusMinutes(10), 15);
        });
        ApiResponse<?> resp = findDueReminders(60);
        assertEquals(0, resp.getStatus(), "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertEquals(1, list.size(), "reminder=15 时 now+45min 不命中（45>15），now+10min 命中");
        Map<?, ?> hit = (Map<?, ?>) list.get(0);
        assertEquals("EVT-NAR-002", hit.get("code"));
    }

    @Test
    public void testPlannedGuardExcludesCompleted() {
        LocalDateTime now = CoreMetrics.currentDateTime();
        ormTemplate.runInSession(() -> {
            // COMPLETED 事件即使 reminder 窗口内也不计入提醒范围
            seedEvent(4506L, "EVT-DONE-001", ErpCrmConstants.EVENT_STATUS_COMPLETED,
                    now.plusMinutes(5), 60);
        });
        ApiResponse<?> resp = findDueReminders(60);
        assertEquals(0, resp.getStatus(), "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertTrue(list == null || list.isEmpty(), "COMPLETED 事件不计入到期提醒（PLANNED 守卫）");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> findDueReminders(Integer windowMinutes) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(query, "ErpCrmEvent__findDueReminders",
                        ApiRequest.build(Map.of("windowMinutes", windowMinutes))));
    }

    // ---------- seed helpers ----------

    private void seedEvent(Long id, String code, String status, LocalDateTime startDateTime,
                           Integer reminderMinutesBefore) {
        IEntityDao<ErpCrmEvent> dao = daoProvider.daoFor(ErpCrmEvent.class);
        ErpCrmEvent event = new ErpCrmEvent();
        event.setId(id);
        event.setCode(code);
        event.setOrgId(ORG_ID);
        event.setEventType("CALL");
        event.setSubject("事件-" + code);
        event.setStatus(status);
        event.setPriority("NORMAL");
        event.setStartDateTime(Timestamp.valueOf(startDateTime));
        event.setEndDateTime(Timestamp.valueOf(startDateTime.plusMinutes(30)));
        event.setReminderMinutesBefore(reminderMinutesBefore);
        dao.saveEntity(event);
    }
}
