package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmActivity;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRM 活动提醒 + 活动时间线端到端测试（plan 2026-07-04-0700-1 Phase 1）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpCrmEvent__complete/cancel} 与
 * {@code ErpCrmEvent__findDueReminders/getLeadTimeline}，覆盖：
 * Event 状态机迁移合法性、completed 事件回写 Lead.lastContactDate、
 * 到期提醒窗口过滤、时间线 Event+Activity 合并倒序。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmEventReminderTimeline extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCompleteAndCancelAndDerivation() {
        Long leadId = 3001L;
        Long eventCompletedId = 3101L;
        Long eventCancelledId = 3102L;
        Long eventPlannedId = 3103L;
        ormTemplate.runInSession(() -> {
            seedLead(leadId, "LEAD-EVT-001");
            // 一条已完成的早期事件（completed 时间较早）→ 决定 lastContactDate
            seedEvent(eventCompletedId, "EVT-C-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    leadId, LocalDateTime.of(2026, 7, 1, 10, 0));
            // 一条将取消的事件
            seedEvent(eventCancelledId, "EVT-X-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    leadId, LocalDateTime.of(2026, 7, 2, 14, 0));
            // 一条保持 PLANNED 的事件（未来）→ 决定 nextActivityDate
            seedEvent(eventPlannedId, "EVT-P-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    leadId, LocalDateTime.of(2026, 7, 3, 9, 0));
        });

        // complete: PLANNED → COMPLETED
        assertEquals(0, complete(eventCompletedId).getStatus(), "complete 应成功");
        ErpCrmEvent completed = reloadEvent(eventCompletedId);
        assertEquals(ErpCrmConstants.EVENT_STATUS_COMPLETED, completed.getStatus(), "PLANNED → COMPLETED");

        // cancel: PLANNED → CANCELLED
        assertEquals(0, cancel(eventCancelledId).getStatus(), "cancel 应成功");
        ErpCrmEvent cancelled = reloadEvent(eventCancelledId);
        assertEquals(ErpCrmConstants.EVENT_STATUS_CANCELLED, cancelled.getStatus(), "PLANNED → CANCELLED");

        // 派生回写 Lead：lastContactDate = 最近 COMPLETED 事件 startDateTime = 2026-07-01 10:00
        ErpCrmLead lead = reloadLead(leadId);
        assertEquals(LocalDateTime.of(2026, 7, 1, 10, 0), lead.getLastContactDate(),
                "lastContactDate = 最近 COMPLETED 事件 startDateTime 最大值");
        // nextActivityDate = 最近 PLANNED 事件 startDateTime 最小值 = 2026-07-03 09:00（取消的已不计入）
        assertEquals(LocalDateTime.of(2026, 7, 3, 9, 0), lead.getNextActivityDate(),
                "nextActivityDate = 最近 PLANNED 事件 startDateTime 最小值（CANCELLED 不计入）");
    }

    @Test
    public void testIllegalTransitionRejected() {
        Long eventId = 3201L;
        ormTemplate.runInSession(() -> {
            seedLead(3301L, "LEAD-EVT-002");
            seedEvent(eventId, "EVT-ILLEGAL-001", ErpCrmConstants.EVENT_STATUS_COMPLETED,
                    3301L, LocalDateTime.of(2026, 7, 1, 10, 0));
        });
        // COMPLETED 不可再次 complete
        ApiResponse<?> bad = complete(eventId);
        assertEquals(ErpCrmErrors.ERR_EVENT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "COMPLETED 不可再次 complete → ERR_EVENT_ILLEGAL_STATUS_TRANSITION");
        // COMPLETED 不可 cancel
        ApiResponse<?> badCancel = cancel(eventId);
        assertEquals(ErpCrmErrors.ERR_EVENT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), badCancel.getCode(),
                "COMPLETED 不可 cancel → ERR_EVENT_ILLEGAL_STATUS_TRANSITION");
    }

    @Test
    public void testEventWithoutLeadSkipsDerivation() {
        Long eventId = 3401L;
        ormTemplate.runInSession(() -> seedEvent(eventId, "EVT-NOLEAD-001",
                ErpCrmConstants.EVENT_STATUS_PLANNED, null, LocalDateTime.of(2026, 7, 1, 10, 0)));
        // 无 relatedLeadId，complete 不应抛错（跳过派生）
        assertEquals(0, complete(eventId).getStatus(), "无关联 Lead 的 Event complete 应成功");
        assertEquals(ErpCrmConstants.EVENT_STATUS_COMPLETED, reloadEvent(eventId).getStatus(),
                "PLANNED → COMPLETED");
    }

    @Test
    public void testFindDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        ormTemplate.runInSession(() -> {
            // 窗口内（now+10min）
            seedEvent(3501L, "EVT-DUE-001", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    null, now.plusMinutes(10));
            // 窗口外（now+200min）
            seedEvent(3502L, "EVT-DUE-002", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    null, now.plusMinutes(200));
            // 已完成不在提醒范围
            seedEvent(3503L, "EVT-DUE-003", ErpCrmConstants.EVENT_STATUS_COMPLETED,
                    null, now.plusMinutes(5));
        });
        ApiResponse<?> resp = findDueReminders(60);
        assertEquals(0, resp.getStatus(), "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "窗口内应有 PLANNED 事件");
        // 仅窗口内 PLANNED 事件（EVT-DUE-001）
        assertEquals(1, list.size(), "仅窗口内 PLANNED 事件（排除窗口外与 COMPLETED）");
    }

    @Test
    public void testGetLeadTimeline() {
        Long leadId = 3601L;
        ormTemplate.runInSession(() -> {
            seedLead(leadId, "LEAD-TL-001");
            seedEvent(3611L, "EVT-TL-001", ErpCrmConstants.EVENT_STATUS_COMPLETED,
                    leadId, LocalDateTime.of(2026, 7, 2, 10, 0));
            seedEvent(3612L, "EVT-TL-002", ErpCrmConstants.EVENT_STATUS_PLANNED,
                    leadId, LocalDateTime.of(2026, 7, 3, 10, 0));
            seedActivity(3621L, leadId, "ACT-TL-001", LocalDateTime.of(2026, 7, 1, 10, 0));
        });
        ApiResponse<?> resp = getLeadTimeline(leadId);
        assertEquals(0, resp.getStatus(), "getLeadTimeline 应成功");
        List<?> list = (List<?>) resp.getData();
        assertNotNull(list);
        assertEquals(3, list.size(), "Event 2 条 + Activity 1 条 = 3 条时间线");
        // 按时间倒序：07-03 > 07-02 > 07-01
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) list.get(0);
        assertEquals(ErpCrmConstants.TIMELINE_SOURCE_EVENT, first.get("sourceType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> last = (Map<String, Object>) list.get(2);
        assertEquals(ErpCrmConstants.TIMELINE_SOURCE_ACTIVITY, last.get("sourceType"));
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> complete(Long eventId) {
        return rpc(mutation, "ErpCrmEvent__complete", Map.of("eventId", eventId));
    }

    private ApiResponse<?> cancel(Long eventId) {
        return rpc(mutation, "ErpCrmEvent__cancel", Map.of("eventId", eventId));
    }

    private ApiResponse<?> findDueReminders(Integer windowMinutes) {
        return rpc(query, "ErpCrmEvent__findDueReminders", Map.of("windowMinutes", windowMinutes));
    }

    private ApiResponse<?> getLeadTimeline(Long leadId) {
        return rpc(query, "ErpCrmEvent__getLeadTimeline", Map.of("leadId", leadId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data)));
    }

    // ---------- seed helpers ----------

    private void seedLead(Long id, String code) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        lead.setContactName("联系人" + id);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
    }

    private void seedEvent(Long id, String code, String status, Long relatedLeadId,
                           LocalDateTime startDateTime) {
        ErpCrmEvent event = new ErpCrmEvent();
        event.setId(id);
        event.setCode(code);
        event.setOrgId(ORG_ID);
        event.setEventType("CALL");
        event.setSubject("事件-" + code);
        event.setStatus(status);
        event.setPriority("NORMAL");
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(startDateTime != null ? startDateTime.plusMinutes(30) : null);
        event.setRelatedLeadId(relatedLeadId);
        daoProvider.daoFor(ErpCrmEvent.class).saveEntity(event);
    }

    private void seedActivity(Long id, Long leadId, String summary, LocalDateTime activityDate) {
        ErpCrmActivity activity = new ErpCrmActivity();
        activity.setId(id);
        activity.setLeadId(leadId);
        activity.setOrgId(ORG_ID);
        activity.setActivityType("NOTE");
        activity.setSummary(summary);
        activity.setActivityDate(activityDate);
        daoProvider.daoFor(ErpCrmActivity.class).saveEntity(activity);
    }

    // ---------- reload helpers ----------

    private ErpCrmEvent reloadEvent(Long id) {
        return daoProvider.daoFor(ErpCrmEvent.class).getEntityById(id);
    }

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }
}
