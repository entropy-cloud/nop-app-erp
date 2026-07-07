package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static app.erp.crm.service.ErpCrmErrors.ARG_CURRENT_STATUS;
import static app.erp.crm.service.ErpCrmErrors.ARG_EXPECTED_STATUS;
import static app.erp.crm.service.ErpCrmErrors.ARG_PROGRESS_ID;
import static app.erp.crm.service.ErpCrmErrors.ARG_STEP_INDEX;
import static app.erp.crm.service.ErpCrmErrors.ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION;
import static app.erp.crm.service.ErpCrmErrors.ERR_SEQUENCE_STEP_NOT_DUE;

/**
 * 销售序列步骤推进引擎。{@link ErpCrmEvent}.status=COMPLETED 且 eventType 匹配当前 step.activityType +
 * completionCondition 满足 → currentStepIndex+1；末步完成则 status=COMPLETED + completedAt；
 * 推进时若下一步 autoCreateEvent=true，标记需建 Event（由 BizModel 落库）。
 *
 * <p>对齐 {@code docs/design/crm/sales-sequence.md §序列推进 / §实现注记 activityType TASK 字典口径}：
 * <ul>
 *   <li>TASK 步骤映射到 event-type 字典（TASK 仅存在于 {@code erp-crm/event-type}，不在 activity-type 字典）；</li>
 *   <li>EMAIL_OPENED / EMAIL_REPLIED 完成条件本期降级为 eventType=EMAIL + status=COMPLETED（邮件跟踪归 successor）；</li>
 *   <li>其他完成条件按 eventType 等值匹配 + status=COMPLETED 判定。</li>
 * </ul>
 *
 * <p>纯函数式 + 注入加载函数便于单测：{@link #advance(ErpCrmLeadSequenceProgress, ErpCrmEvent, List)}
 * 不依赖 IoC，可独立构造 progress/event/steps 测试各 completionCondition 满足/不满足、末步完成、autoCreateEvent 建下一步。
 */
public class SequenceStepAdvancer {

    /**
     * 推进序列进度。
     *
     * @param progress        序列进度（status=IN_PROGRESS，否则抛 ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION）
     * @param completedEvent  触发推进的已完成事件（status 必须为 COMPLETED）
     * @param steps           序列步骤列表（按 stepOrder 升序）
     * @return 推进结果（含 newStepIndex / sequenceCompleted / 是否需要为下一步建 Event）
     */
    public AdvanceResult advance(ErpCrmLeadSequenceProgress progress,
                                  ErpCrmEvent completedEvent,
                                  List<ErpCrmSequenceStep> steps) {
        requireInProgress(progress);
        List<ErpCrmSequenceStep> ordered = orderSteps(steps);
        int currentIndex = progress.getCurrentStepIndex() != null ? progress.getCurrentStepIndex() : 0;
        ErpCrmSequenceStep currentStep = stepAt(ordered, currentIndex, progress);
        requireEventSatisfies(completedEvent, currentStep, progress, currentIndex);

        int nextIndex = currentIndex + 1;
        boolean sequenceCompleted = nextIndex >= ordered.size();
        AdvanceResult result = new AdvanceResult();
        result.setPreviousStepIndex(currentIndex);
        result.setNewStepIndex(sequenceCompleted ? currentIndex : nextIndex);
        result.setSequenceCompleted(sequenceCompleted);
        if (sequenceCompleted) {
            result.setCompletedAt(CoreMetrics.currentDateTime());
        } else {
            ErpCrmSequenceStep nextStep = ordered.get(nextIndex);
            result.setNextStep(nextStep);
            result.setEventCreationNeeded(Boolean.TRUE.equals(nextStep.getAutoCreateEvent()));
        }
        return result;
    }

    protected void requireInProgress(ErpCrmLeadSequenceProgress progress) {
        if (!ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS.equals(progress.getStatus())) {
            throw new NopException(ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION)
                    .param(ARG_PROGRESS_ID, progress.getId())
                    .param(ARG_CURRENT_STATUS, progress.getStatus())
                    .param(ARG_EXPECTED_STATUS, ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS);
        }
    }

    protected ErpCrmSequenceStep stepAt(List<ErpCrmSequenceStep> ordered, int index,
                                         ErpCrmLeadSequenceProgress progress) {
        if (index < 0 || index >= ordered.size()) {
            throw new NopException(ERR_SEQUENCE_STEP_NOT_DUE)
                    .param(ARG_PROGRESS_ID, progress.getId())
                    .param(ARG_STEP_INDEX, index);
        }
        return ordered.get(index);
    }

    /**
     * 校验事件是否满足当前步骤完成条件：status=COMPLETED + eventType 匹配 step.activityType + completionCondition 满足。
     * 完成条件映射：
     * <ul>
     *   <li>CALL_COMPLETED  → event.eventType=CALL &amp; status=COMPLETED</li>
     *   <li>MEETING_HELD    → event.eventType=MEETING &amp; status=COMPLETED</li>
     *   <li>TASK_DONE       → event.eventType=TASK &amp; status=COMPLETED（TASK 仅在 event-type 字典）</li>
     *   <li>EMAIL_OPENED/REPLIED → 降级：event.eventType=EMAIL &amp; status=COMPLETED（邮件跟踪 successor）</li>
     * </ul>
     */
    protected void requireEventSatisfies(ErpCrmEvent event, ErpCrmSequenceStep step,
                                          ErpCrmLeadSequenceProgress progress, int stepIndex) {
        if (event == null || !ErpCrmConstants.EVENT_STATUS_COMPLETED.equals(event.getStatus())) {
            throw new NopException(ERR_SEQUENCE_STEP_NOT_DUE)
                    .param(ARG_PROGRESS_ID, progress.getId())
                    .param(ARG_STEP_INDEX, stepIndex);
        }
        String condition = step.getCompletionCondition();
        String expectedEventType = resolveExpectedEventType(condition, step.getActivityType());
        if (expectedEventType != null && !expectedEventType.equals(event.getEventType())) {
            throw new NopException(ERR_SEQUENCE_STEP_NOT_DUE)
                    .param(ARG_PROGRESS_ID, progress.getId())
                    .param(ARG_STEP_INDEX, stepIndex);
        }
    }

    /**
     * 解析完成条件期望的 event.eventType。
     * activityType=TASK 步骤映射到 event-type 字典的 TASK（Decision：不在 activity-type 字典补值）。
     */
    protected String resolveExpectedEventType(String condition, String activityType) {
        if (condition == null) {
            return activityType;
        }
        switch (condition) {
            case ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED:
                return "CALL";
            case ErpCrmConstants.STEP_COMPLETION_MEETING_HELD:
                return "MEETING";
            case ErpCrmConstants.STEP_COMPLETION_TASK_DONE:
                return "TASK";
            case ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED:
            case ErpCrmConstants.STEP_COMPLETION_EMAIL_REPLIED:
                // EMAIL_* 本期降级为 eventType=EMAIL 匹配（邮件打开/回复跟踪归 successor）
                return "EMAIL";
            default:
                return activityType;
        }
    }

    protected List<ErpCrmSequenceStep> orderSteps(List<ErpCrmSequenceStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<ErpCrmSequenceStep> copy = new java.util.ArrayList<>(steps);
        copy.sort(Comparator
                .comparingInt((ErpCrmSequenceStep s) ->
                        s.getStepOrder() != null ? s.getStepOrder() : Integer.MAX_VALUE)
                .thenComparing(s -> s.getId() != null ? s.getId() : Long.MAX_VALUE));
        return copy;
    }

    // ---------- 结果 DTO ----------

    public static class AdvanceResult {
        private int previousStepIndex;
        private int newStepIndex;
        private boolean sequenceCompleted;
        private LocalDateTime completedAt;
        private ErpCrmSequenceStep nextStep;
        private boolean eventCreationNeeded;

        public int getPreviousStepIndex() {
            return previousStepIndex;
        }

        public void setPreviousStepIndex(int previousStepIndex) {
            this.previousStepIndex = previousStepIndex;
        }

        public int getNewStepIndex() {
            return newStepIndex;
        }

        public void setNewStepIndex(int newStepIndex) {
            this.newStepIndex = newStepIndex;
        }

        public boolean isSequenceCompleted() {
            return sequenceCompleted;
        }

        public void setSequenceCompleted(boolean sequenceCompleted) {
            this.sequenceCompleted = sequenceCompleted;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public ErpCrmSequenceStep getNextStep() {
            return nextStep;
        }

        public void setNextStep(ErpCrmSequenceStep nextStep) {
            this.nextStep = nextStep;
        }

        public boolean isEventCreationNeeded() {
            return eventCreationNeeded;
        }

        public void setEventCreationNeeded(boolean eventCreationNeeded) {
            this.eventCreationNeeded = eventCreationNeeded;
        }
    }
}
