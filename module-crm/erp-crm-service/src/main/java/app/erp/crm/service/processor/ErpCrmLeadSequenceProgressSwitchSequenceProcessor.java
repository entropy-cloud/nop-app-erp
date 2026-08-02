package app.erp.crm.service.processor;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmSequenceBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequence;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCrmLeadSequenceProgress switchSequence per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含销售序列切换编排：旧活跃序列 SKIPPED 快照 + 新序列 stepIndex=0 进度 + 首步 autoCreateEvent。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadSequenceProgressSwitchSequenceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCrmLeadBiz leadBiz;

    @Inject
    IErpCrmSequenceBiz sequenceBiz;

    @Inject
    IErpCrmEventBiz eventBiz;

    public ErpCrmLeadSequenceProgress switchSequence(Long leadId, Long newSequenceId, IServiceContext context) {
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
            dao().updateEntity(old);
        }

        // 新序列 stepIndex=0 进度
        ErpCrmLeadSequenceProgress progress = dao().newEntity();
        progress.setLeadId(leadId);
        progress.setSequenceId(newSequenceId);
        progress.setOrgId(lead.getOrgId());
        progress.setCurrentStepIndex(0);
        progress.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS);
        progress.setStartedAt(CoreMetrics.currentTimestamp());
        dao().saveEntity(progress);

        // 首步 autoCreateEvent
        List<ErpCrmSequenceStep> steps = loadSteps(newSequenceId);
        if (!steps.isEmpty() && Boolean.TRUE.equals(steps.get(0).getAutoCreateEvent())) {
            createEventForStep(steps.get(0), lead, progress, context);
        }
        return progress;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmLeadSequenceProgress findActiveProgress(Long leadId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addFilter(eq("status", ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS));
        q.setLimit(1);
        return dao().findAllByQuery(q).stream().findFirst().orElse(null);
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

    private IEntityDao<ErpCrmLeadSequenceProgress> dao() {
        return daoProvider.daoFor(ErpCrmLeadSequenceProgress.class);
    }

    protected IEntityDao<ErpCrmSequenceStep> stepDao() {
        return daoProvider.daoFor(ErpCrmSequenceStep.class);
    }

    protected IEntityDao<ErpCrmEvent> eventDao() {
        return daoProvider.daoFor(ErpCrmEvent.class);
    }
}
