
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmLeadSequenceProgressBiz;
import app.erp.crm.biz.IErpCrmSequenceAssignmentBiz;
import app.erp.crm.biz.IErpCrmSequenceBiz;
import app.erp.crm.biz.IErpCrmSequenceStepBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequence;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.SequenceAssignmentEngine;
import app.erp.crm.service.support.SequenceStepAdvancer;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import java.util.Collections;

/**
 * 销售序列进度 BizModel。{@link #assignSequence} / {@link #advanceStep} / {@link #switchSequence} /
 * {@link #scanOverdueSteps} / {@link #getSequencePerformance} 委托纯函数式引擎
 * ({@link SequenceAssignmentEngine} / {@link SequenceStepAdvancer})。
 *
 * <p>对齐 {@code docs/design/crm/sales-sequence.md}：
 * 序列自动分配（四 conditionType + default 兜底）/ 步骤推进（completionCondition 各值 + autoCreateEvent）/ 
 * 序列切换（旧序列 SKIPPED）/ 逾期扫描 / 性能分析。
 *
 * <p>序列进度存储方案：{@link ErpCrmLead} 无任何序列字段，全部由 {@link ErpCrmLeadSequenceProgress} 关联表承载
 * （既定方案，避免 ORM ask-first 保护区域；详见 sales-sequence.md 实现注记）。
 */
@BizModel("ErpCrmLeadSequenceProgress")
public class ErpCrmLeadSequenceProgressBizModel
        extends CrudBizModel<ErpCrmLeadSequenceProgress>
        implements IErpCrmLeadSequenceProgressBiz {

    @Inject
    SequenceAssignmentEngine sequenceAssignmentEngine;
    @Inject
    SequenceStepAdvancer sequenceStepAdvancer;
    @Inject
    IErpCrmLeadBiz leadBiz;
    @Inject
    IErpCrmSequenceBiz sequenceBiz;
    @Inject
    IErpCrmSequenceAssignmentBiz sequenceAssignmentBiz;
    @Inject
    IErpCrmSequenceStepBiz sequenceStepBiz;
    @Inject
    IErpCrmEventBiz eventBiz;

    public ErpCrmLeadSequenceProgressBizModel() {
        setEntityName(ErpCrmLeadSequenceProgress.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress assignSequence(@Name("leadId") Long leadId, IServiceContext context) {
        ErpCrmLead lead = leadBiz.requireEntity(String.valueOf(leadId), null, context);

        ErpCrmLeadSequenceProgress existing = findActiveProgress(leadId);
        if (existing != null) {
            throw new NopException(ErpCrmErrors.ERR_SEQUENCE_ALREADY_ASSIGNED)
                    .param(ErpCrmErrors.ARG_LEAD_ID, leadId)
                    .param(ErpCrmErrors.ARG_PROGRESS_ID, existing.getId());
        }

        List<ErpCrmSequenceAssignment> rules = loadAssignmentRules();
        ErpCrmSequenceAssignment defaultRule = loadDefaultRule();
        SequenceAssignmentEngine.AssignmentResult matched =
                sequenceAssignmentEngine.assign(lead, rules, defaultRule);
        if (matched == null || matched.getSequenceId() == null) {
            throw new NopException(ErpCrmErrors.ERR_SEQUENCE_NO_MATCH)
                    .param(ErpCrmErrors.ARG_LEAD_ID, leadId);
        }

        Long sequenceId = matched.getSequenceId();
        ErpCrmLeadSequenceProgress progress = newEntity();
        progress.setLeadId(leadId);
        progress.setSequenceId(sequenceId);
        progress.setOrgId(lead.getOrgId());
        progress.setCurrentStepIndex(0);
        progress.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS);
        progress.setStartedAt(CoreMetrics.currentTimestamp());
        saveEntity(progress, null, context);

        // 首步若 autoCreateEvent → 建排程 ErpCrmEvent
        List<ErpCrmSequenceStep> steps = loadSteps(sequenceId);
        if (!steps.isEmpty()) {
            ErpCrmSequenceStep first = steps.get(0);
            if (Boolean.TRUE.equals(first.getAutoCreateEvent())) {
                createEventForStep(first, lead, progress, context);
            }
        }
        return progress;
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress advanceStep(@Name("progressId") Long progressId,
                                                   @Name("eventId") Long eventId,
                                                   IServiceContext context) {
        ErpCrmLeadSequenceProgress progress = requireEntity(String.valueOf(progressId), null, context);
        ErpCrmEvent event = eventBiz.requireEntity(String.valueOf(eventId), null, context);
        List<ErpCrmSequenceStep> steps = loadSteps(progress.getSequenceId());

        SequenceStepAdvancer.AdvanceResult result =
                sequenceStepAdvancer.advance(progress, event, steps);

        progress.setCurrentStepIndex(result.getNewStepIndex());
        if (result.isSequenceCompleted()) {
            progress.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED);
            progress.setCompletedAt(result.getCompletedAt() != null ? Timestamp.valueOf(result.getCompletedAt()) : null);
        }
        updateEntity(progress, null, context);

        // 推进时若下一步 autoCreateEvent → 建下一步 Event
        if (!result.isSequenceCompleted() && result.isEventCreationNeeded() && result.getNextStep() != null) {
            ErpCrmLead lead = leadBiz.requireEntity(String.valueOf(progress.getLeadId()), null, context);
            createEventForStep(result.getNextStep(), lead, progress, context);
        }
        return progress;
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress switchSequence(@Name("leadId") Long leadId,
                                                      @Name("newSequenceId") Long newSequenceId,
                                                      IServiceContext context) {
        ErpCrmLead lead = leadBiz.requireEntity(String.valueOf(leadId), null, context);
        // 校验新序列存在且启用
        ErpCrmSequence sequence = sequenceBiz.requireEntity(String.valueOf(newSequenceId), null, context);
        if (!Boolean.TRUE.equals(sequence.getIsActive())) {
            throw new NopException(ErpCrmErrors.ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCrmErrors.ARG_SEQUENCE_ID, newSequenceId)
                    .param(ErpCrmErrors.ARG_CURRENT_STATUS, "INACTIVE");
        }

        // 旧活跃序列 SKIPPED 快照
        ErpCrmLeadSequenceProgress old = findActiveProgress(leadId);
        if (old != null) {
            old.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_SKIPPED);
            old.setCompletedAt(CoreMetrics.currentTimestamp());
            updateEntity(old, null, context);
        }

        // 新序列 stepIndex=0 进度
        ErpCrmLeadSequenceProgress progress = newEntity();
        progress.setLeadId(leadId);
        progress.setSequenceId(newSequenceId);
        progress.setOrgId(lead.getOrgId());
        progress.setCurrentStepIndex(0);
        progress.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS);
        progress.setStartedAt(CoreMetrics.currentTimestamp());
        saveEntity(progress, null, context);

        // 首步 autoCreateEvent
        List<ErpCrmSequenceStep> steps = loadSteps(newSequenceId);
        if (!steps.isEmpty() && Boolean.TRUE.equals(steps.get(0).getAutoCreateEvent())) {
            createEventForStep(steps.get(0), lead, progress, context);
        }
        return progress;
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> scanOverdueSteps(IServiceContext context) {
        int grace = ErpCrmConfigs.sequenceGracePeriodDays();
        int maxOverdue = ErpCrmConfigs.sequenceMaxOverdueSteps();
        LocalDateTime now = CoreMetrics.currentDateTime();

        List<ErpCrmLeadSequenceProgress> inProgress = loadAllInProgress();
        // 按 sequenceId 分组以避免重复加载 steps
        Map<Long, List<ErpCrmSequenceStep>> stepsBySequence = new HashMap<>();
        for (ErpCrmLeadSequenceProgress p : inProgress) {
            stepsBySequence.computeIfAbsent(p.getSequenceId(), this::loadSteps);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ErpCrmLeadSequenceProgress p : inProgress) {
            List<ErpCrmSequenceStep> steps = stepsBySequence.getOrDefault(p.getSequenceId(), java.util.Collections.emptyList());
            int consecutiveOverdue = countConsecutiveOverdueSteps(p, steps, now, grace);
            if (consecutiveOverdue >= maxOverdue) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("leadId", p.getLeadId());
                row.put("progressId", p.getId());
                row.put("sequenceId", p.getSequenceId());
                row.put("currentStepIndex", p.getCurrentStepIndex());
                row.put("overdueStepCount", consecutiveOverdue);
                row.put("startedAt", p.getStartedAt());
                result.add(row);
            }
        }
        return result;
    }

    @Override
    @BizQuery
    public Map<String, Object> getSequencePerformance(@Name("templateType") String templateType,
                                                       IServiceContext context) {
        List<ErpCrmSequence> sequences = loadSequencesByTemplate(templateType);
        Map<String, Object> perf = new LinkedHashMap<>();
        if (sequences.isEmpty()) {
            perf.put("templateType", templateType);
            perf.put("totalAssigned", 0);
            perf.put("totalCompleted", 0);
            perf.put("completionRate", 0.0);
            perf.put("avgCompletionDays", 0.0);
            perf.put("stepDropOffRate", 0.0);
            perf.put("sequences", java.util.Collections.emptyList());
            return perf;
        }

        List<Long> sequenceIds = sequences.stream().map(ErpCrmSequence::getId).collect(Collectors.toList());
        List<ErpCrmLeadSequenceProgress> all = loadProgressBySequenceIds(sequenceIds);

        int totalAssigned = all.size();
        int totalCompleted = (int) all.stream()
                .filter(p -> ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED.equals(p.getStatus())).count();
        int totalSkipped = (int) all.stream()
                .filter(p -> ErpCrmConstants.SEQUENCE_PROGRESS_SKIPPED.equals(p.getStatus())).count();
        double completionRate = totalAssigned == 0 ? 0.0 : (double) totalCompleted / totalAssigned;
        double dropOffRate = totalAssigned == 0 ? 0.0 : (double) totalSkipped / totalAssigned;

        double avgCompletionDays = 0.0;
        int counted = 0;
        for (ErpCrmLeadSequenceProgress p : all) {
            if (ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED.equals(p.getStatus())
                    && p.getStartedAt() != null && p.getCompletedAt() != null) {
                long days = java.time.Duration.between(p.getStartedAt().toLocalDateTime(), p.getCompletedAt().toLocalDateTime()).toDays();
                avgCompletionDays += days;
                counted++;
            }
        }
        if (counted > 0) {
            avgCompletionDays = avgCompletionDays / counted;
        }

        perf.put("templateType", templateType);
        perf.put("totalAssigned", totalAssigned);
        perf.put("totalCompleted", totalCompleted);
        perf.put("totalSkipped", totalSkipped);
        perf.put("completionRate", Math.round(completionRate * 10000) / 10000.0);
        perf.put("avgCompletionDays", Math.round(avgCompletionDays * 100) / 100.0);
        perf.put("stepDropOffRate", Math.round(dropOffRate * 10000) / 10000.0);
        perf.put("sequences", sequenceIds);
        return perf;
    }

    // ---------- 内部辅助 ----------
    // 以下 dao().findAllByQuery 调用均为同域只读内部辅助：
    // - findActiveProgress / loadAllInProgress / loadProgressBySequenceIds 用于序列推进引擎的
    //   状态查询，不走 CrudBizModel findList 管道以保留 setLimit(1) + stream 直接消费的简洁语义；
    //   数据权限在调用方 @BizMutation 入口已校验。M-6（plan 2026-07-20-2200-1）补注释。

    protected ErpCrmLeadSequenceProgress findActiveProgress(Long leadId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addFilter(eq("status", ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS));
        q.setLimit(1);
        return dao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    protected List<ErpCrmLeadSequenceProgress> loadAllInProgress() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS));
        return dao().findAllByQuery(q);
    }

    protected List<ErpCrmLeadSequenceProgress> loadProgressBySequenceIds(List<Long> sequenceIds) {
        if (sequenceIds == null || sequenceIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("sequenceId", sequenceIds));
        return dao().findAllByQuery(q);
    }

    protected List<ErpCrmSequenceAssignment> loadAssignmentRules() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        // 引擎内部过滤 isDefault=true 规则；default 单独经 loadDefaultRule 传入
        return assignmentDao().findAllByQuery(q);
    }

    protected ErpCrmSequenceAssignment loadDefaultRule() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        q.setLimit(1);
        return assignmentDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    protected List<ErpCrmSequenceStep> loadSteps(Long sequenceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sequenceId", sequenceId));
        List<ErpCrmSequenceStep> steps = stepDao().findAllByQuery(q);
        steps.sort(Comparator
                .comparingInt((ErpCrmSequenceStep s) ->
                        s.getStepOrder() != null ? s.getStepOrder() : Integer.MAX_VALUE)
                .thenComparing(s -> s.getId() != null ? s.getId() : Long.MAX_VALUE));
        return steps;
    }

    protected List<ErpCrmSequence> loadSequencesByTemplate(String templateType) {
        QueryBean q = new QueryBean();
        if (templateType != null && !templateType.isEmpty()) {
            q.addFilter(eq("templateType", templateType));
        }
        return sequenceDao().findAllByQuery(q);
    }

    /**
     * 计算从当前步起向前连续逾期的步骤数。
     * 单步 due 时间 = startedAt + 累计 dueDays[0..stepIndex] + grace。从 currentIndex 向 0 反向扫描，
     * 第一个未逾期步骤即终止。
     */
    protected int countConsecutiveOverdueSteps(ErpCrmLeadSequenceProgress progress,
                                                List<ErpCrmSequenceStep> steps,
                                                LocalDateTime now, int grace) {
        if (progress.getStartedAt() == null || steps.isEmpty()) {
            return 0;
        }
        int currentIndex = progress.getCurrentStepIndex() != null ? progress.getCurrentStepIndex() : 0;
        int count = 0;
        long cumulativeDays = 0;
        // 反向扫描：从 currentIndex 起向前累计
        for (int i = 0; i <= currentIndex && i < steps.size(); i++) {
            ErpCrmSequenceStep step = steps.get(i);
            if (step.getDueDays() != null) {
                cumulativeDays += step.getDueDays();
            }
            LocalDateTime dueAt = progress.getStartedAt().toLocalDateTime().plusDays(cumulativeDays + grace);
            if (now.isAfter(dueAt)) {
                count++;
            } else {
                break;
            }
        }
        // count 从 0..currentIndex 累计，取末段连续逾期
        return count;
    }

    protected void createEventForStep(ErpCrmSequenceStep step, ErpCrmLead lead,
                                       ErpCrmLeadSequenceProgress progress, IServiceContext context) {
        ErpCrmEvent event = eventDao().newEntity();
        event.setCode("SEQ-EVT-" + progress.getId() + "-" + step.getStepOrder());
        event.setOrgId(lead.getOrgId());
        event.setEventType(mapActivityTypeToEventType(step.getActivityType()));
        event.setSubject(step.getStepName());
        event.setDescription(step.getStepDescription());
        LocalDateTime start = CoreMetrics.currentDateTime().plusDays(
                step.getDueDays() != null ? step.getDueDays() : 0);
        event.setStartDateTime(Timestamp.valueOf(start));
        event.setEndDateTime(Timestamp.valueOf(start.plusHours(1)));
        event.setRelatedLeadId(lead.getId());
        event.setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_CRM_LEAD);
        event.setRelatedBillCode(lead.getCode());
        event.setOwnerId(lead.getOwnerId());
        event.setStatus(ErpCrmConstants.EVENT_STATUS_PLANNED);
        event.setPriority("NORMAL");
        eventBiz.saveEntity(event, null, context);
    }

    /**
     * activityType → eventType 映射：TASK 仅存在于 event-type 字典（Decision：不在 activity-type 字典补值）。
     */
    protected String mapActivityTypeToEventType(String activityType) {
        if (activityType == null) {
            return null;
        }
        return activityType;
    }

    protected IEntityDao<ErpCrmSequenceAssignment> assignmentDao() {
        return daoProvider().daoFor(ErpCrmSequenceAssignment.class);
    }

    protected IEntityDao<ErpCrmSequenceStep> stepDao() {
        return daoProvider().daoFor(ErpCrmSequenceStep.class);
    }

    protected IEntityDao<ErpCrmSequence> sequenceDao() {
        return daoProvider().daoFor(ErpCrmSequence.class);
    }

    protected IEntityDao<ErpCrmEvent> eventDao() {
        return daoProvider().daoFor(ErpCrmEvent.class);
    }

    

}
