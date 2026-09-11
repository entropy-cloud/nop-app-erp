
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.biz.IErpCrmLeadSequenceProgressBiz;
import app.erp.crm.biz.IErpCrmSequenceAssignmentBiz;
import app.erp.crm.biz.IErpCrmSequenceBiz;
import app.erp.crm.biz.IErpCrmSequenceStepBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequence;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.processor.ErpCrmLeadSequenceProgressAdvanceStepProcessor;
import app.erp.crm.service.processor.ErpCrmLeadSequenceProgressAssignSequenceProcessor;
import app.erp.crm.service.processor.ErpCrmLeadSequenceProgressSwitchSequenceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
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

// 族 A/U20 豁免登记：本类为BizModel；daoFor 目标（ErpCrmEvent、ErpCrmSequence、ErpCrmSequenceStep）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 销售序列进度 BizModel。{@link #assignSequence} / {@link #advanceStep} / {@link #switchSequence} 各自委托独立
 * per-mutation Processor（R6.6，{@code processor-extension-pattern.md}）。{@link #scanOverdueSteps} /
 * {@link #getSequencePerformance} 为只读查询保留在内联。
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
        extends AbstractErpCrudBizModel<ErpCrmLeadSequenceProgress>
        implements IErpCrmLeadSequenceProgressBiz {

    @Inject
    IErpCrmSequenceAssignmentBiz sequenceAssignmentBiz;
    @Inject
    IErpCrmSequenceStepBiz sequenceStepBiz;
    @Inject
    IErpCrmEventBiz eventBiz;

    @Inject
    ErpCrmLeadSequenceProgressAssignSequenceProcessor assignSequenceProcessor;
    @Inject
    ErpCrmLeadSequenceProgressAdvanceStepProcessor advanceStepProcessor;
    @Inject
    ErpCrmLeadSequenceProgressSwitchSequenceProcessor switchSequenceProcessor;

    public ErpCrmLeadSequenceProgressBizModel() {
        setEntityName(ErpCrmLeadSequenceProgress.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress assignSequence(@Name("leadId") String leadId, IServiceContext context) {
        return assignSequenceProcessor.assignSequence(leadId, context);
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress advanceStep(@Name("progressId") String progressId,
                                                  @Name("eventId") String eventId,
                                                   IServiceContext context) {
        return advanceStepProcessor.advanceStep(progressId, eventId, context);
    }

    @Override
    @BizMutation
    public ErpCrmLeadSequenceProgress switchSequence(@Name("leadId") String leadId,
                                                     @Name("newSequenceId") String newSequenceId,
                                                      IServiceContext context) {
        return switchSequenceProcessor.switchSequence(leadId, newSequenceId, context);
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> scanOverdueSteps(IServiceContext context) {
        int grace = ErpCrmConfigs.sequenceGracePeriodDays();
        int maxOverdue = ErpCrmConfigs.sequenceMaxOverdueSteps();
        LocalDateTime now = CoreMetrics.currentDateTime();

        List<ErpCrmLeadSequenceProgress> inProgress = loadAllInProgress();
        // 按 sequenceId 分组以避免重复加载 steps
        Map<String, List<ErpCrmSequenceStep>> stepsBySequence = new HashMap<>();
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

        List<String> sequenceIds = sequences.stream().map(ErpCrmSequence::getId).collect(Collectors.toList());
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
    //   数据权限在调用方 @BizMutation 入口已校验。

    protected ErpCrmLeadSequenceProgress findActiveProgress(String leadId) {
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

    protected List<ErpCrmLeadSequenceProgress> loadProgressBySequenceIds(List<String> sequenceIds) {
        if (sequenceIds == null || sequenceIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("sequenceId", sequenceIds));
        return dao().findAllByQuery(q);
    }

    protected List<ErpCrmSequenceStep> loadSteps(String sequenceId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sequenceId", sequenceId));
        List<ErpCrmSequenceStep> steps = stepDao().findAllByQuery(q);
        steps.sort(Comparator
                .comparingInt((ErpCrmSequenceStep s) ->
                        s.getStepOrder() != null ? s.getStepOrder() : Integer.MAX_VALUE)
                .thenComparingLong(s -> s.getId() != null ? ConvertHelper.toLong(s.getId()) : Long.MAX_VALUE));
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
     * 计算当前进度窗的连续逾期深度（P1-CK-crm2-001 修正，plan 2026-09-11-2350-1 Phase 2）。
     * 单步 due 时间 = startedAt + 累计 dueDays[0..stepIndex] + grace（到期时刻随 index 单调递增）。
     * 已完成步骤（index < currentIndex）无 per-step 完成时间戳、不可回溯判定其历史是否逾期——
     * 语义收敛为「当前步逾期深度」：先判当前步，当前步未逾期 → 0（按期推进零误报）；当前步已逾期
     * 才向更早步反序累计连续逾期数。数据模型限制下最接近 owner doc sales-sequence.md §3 的可计算口径。
     */
    protected int countConsecutiveOverdueSteps(ErpCrmLeadSequenceProgress progress,
                                                List<ErpCrmSequenceStep> steps,
                                                LocalDateTime now, int grace) {
        if (progress.getStartedAt() == null || steps.isEmpty()) {
            return 0;
        }
        int currentIndex = progress.getCurrentStepIndex() != null ? progress.getCurrentStepIndex() : 0;
        int lastIndex = Math.min(currentIndex, steps.size() - 1);
        // 预计算累计 dueDays[0..i]（到期时刻单调递增）
        long[] cumulative = new long[lastIndex + 1];
        long acc = 0;
        for (int i = 0; i <= lastIndex; i++) {
            ErpCrmSequenceStep step = steps.get(i);
            if (step.getDueDays() != null) {
                acc += step.getDueDays();
            }
            cumulative[i] = acc;
        }
        // 反序扫描：当前步未逾期即 0；已逾期才向更早步累计
        int count = 0;
        for (int i = lastIndex; i >= 0; i--) {
            LocalDateTime dueAt = progress.getStartedAt().toLocalDateTime().plusDays(cumulative[i] + grace);
            if (now.isAfter(dueAt)) {
                count++;
            } else {
                break;
            }
        }
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
