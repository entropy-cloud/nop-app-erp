package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.support.SequenceStepAdvancer;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售序列步骤推进引擎单元测试（plan 2026-07-07-1430-3 §Phase 3）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：各 completionCondition 满足/不满足、末步完成、autoCreateEvent 建下一步、EMAIL_* 降级、
 * TASK 字典口径映射、非法状态拒绝。
 */
public class TestSequenceStepAdvancer extends BaseTestCase {

    private final SequenceStepAdvancer advancer = new SequenceStepAdvancer();

    @Test
    public void testCallCompletedAdvances() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("CALL");
        ErpCrmSequenceStep current = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        ErpCrmSequenceStep next = newStep(2, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);

        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Arrays.asList(current, next));
        assertEquals(1, result.getNewStepIndex(), "currentStepIndex 0 → 1");
        assertFalse(result.isSequenceCompleted(), "仍有下一步 → 未完成");
        assertEquals(next, result.getNextStep());
        assertFalse(result.isEventCreationNeeded(), "next.autoCreateEvent=false");
    }

    @Test
    public void testEventTypeMismatchRejects() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("EMAIL"); // 与步骤 CALL 不匹配
        ErpCrmSequenceStep current = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        assertThrows(NopException.class,
                () -> advancer.advance(p, event, Collections.singletonList(current)),
                "eventType 不匹配 → ERR_SEQUENCE_STEP_NOT_DUE");
    }

    @Test
    public void testEventNotCompletedRejects() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("CALL");
        event.setStatus(ErpCrmConstants.EVENT_STATUS_PLANNED); // 未完成
        ErpCrmSequenceStep current = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        assertThrows(NopException.class,
                () -> advancer.advance(p, event, Collections.singletonList(current)),
                "status != COMPLETED → ERR_SEQUENCE_STEP_NOT_DUE");
    }

    @Test
    public void testEmailOpenedDegradesToEventTypeMatch() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        // EMAIL_OPENED 本期降级：eventType=EMAIL + status=COMPLETED 即视为满足（邮件跟踪 successor）
        ErpCrmEvent event = newCompletedEvent("EMAIL");
        ErpCrmSequenceStep current = newStep(1, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Collections.singletonList(current));
        assertTrue(result.isSequenceCompleted(), "末步完成（仅一步）→ sequenceCompleted=true");
        assertNotNull(result.getCompletedAt());
    }

    @Test
    public void testEmailRepliedDegradesToEventTypeMatch() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("EMAIL");
        ErpCrmSequenceStep current = newStep(1, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_REPLIED, false);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Collections.singletonList(current));
        assertTrue(result.isSequenceCompleted());
    }

    @Test
    public void testTaskDoneMapsToEventType() {
        // TASK 仅存在于 event-type 字典（Decision：不在 activity-type 字典补值）
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("TASK");
        ErpCrmSequenceStep current = newStep(1, "TASK", ErpCrmConstants.STEP_COMPLETION_TASK_DONE, false);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Collections.singletonList(current));
        assertTrue(result.isSequenceCompleted());
    }

    @Test
    public void testMeetingHeldAdvances() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("MEETING");
        ErpCrmSequenceStep current = newStep(1, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);
        ErpCrmSequenceStep next = newStep(2, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Arrays.asList(current, next));
        assertEquals(1, result.getNewStepIndex());
    }

    @Test
    public void testLastStepMarksCompleted() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 1); // 已在末步
        ErpCrmEvent event = newCompletedEvent("CALL");
        ErpCrmSequenceStep s1 = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        ErpCrmSequenceStep s2 = newStep(2, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Arrays.asList(s1, s2));
        assertTrue(result.isSequenceCompleted(), "末步完成 → sequenceCompleted=true");
        assertNotNull(result.getCompletedAt(), "completedAt 已写");
        assertEquals(1, result.getNewStepIndex(), "末步不越界");
        assertNull(result.getNextStep(), "无下一步");
    }

    @Test
    public void testAutoCreateEventForNextStep() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("CALL");
        ErpCrmSequenceStep current = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        ErpCrmSequenceStep next = newStep(2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, true);
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Arrays.asList(current, next));
        assertTrue(result.isEventCreationNeeded(), "next.autoCreateEvent=true → 标记建 Event");
        assertEquals(next, result.getNextStep());
    }

    @Test
    public void testIllegalStatusRejects() {
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        p.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED);
        ErpCrmEvent event = newCompletedEvent("CALL");
        ErpCrmSequenceStep step = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        assertThrows(NopException.class,
                () -> advancer.advance(p, event, Collections.singletonList(step)),
                "status != IN_PROGRESS → ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION");
    }

    @Test
    public void testStepOrderRespected() {
        // steps 乱序传入 → 按 stepOrder 排序后推进
        ErpCrmLeadSequenceProgress p = newProgress(1L, 0);
        ErpCrmEvent event = newCompletedEvent("CALL");
        ErpCrmSequenceStep s1 = newStep(1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
        ErpCrmSequenceStep s2 = newStep(2, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);
        // 故意逆序传入
        SequenceStepAdvancer.AdvanceResult result = advancer.advance(p, event, Arrays.asList(s2, s1));
        assertEquals(1, result.getNewStepIndex(), "按 stepOrder 排序后推进");
        assertEquals(s2, result.getNextStep());
    }

    // ---------- helpers ----------

    private ErpCrmLeadSequenceProgress newProgress(Long id, int stepIndex) {
        ErpCrmLeadSequenceProgress p = new ErpCrmLeadSequenceProgress();
        p.setId(id);
        p.setCurrentStepIndex(stepIndex);
        p.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS);
        return p;
    }

    private ErpCrmEvent newCompletedEvent(String eventType) {
        ErpCrmEvent e = new ErpCrmEvent();
        e.setId(System.nanoTime());
        e.setEventType(eventType);
        e.setStatus(ErpCrmConstants.EVENT_STATUS_COMPLETED);
        return e;
    }

    private ErpCrmSequenceStep newStep(int order, String activityType, String condition, boolean autoCreateEvent) {
        ErpCrmSequenceStep s = new ErpCrmSequenceStep();
        s.setId(System.nanoTime());
        s.setStepOrder(order);
        s.setActivityType(activityType);
        s.setCompletionCondition(condition);
        s.setAutoCreateEvent(autoCreateEvent);
        return s;
    }
}
