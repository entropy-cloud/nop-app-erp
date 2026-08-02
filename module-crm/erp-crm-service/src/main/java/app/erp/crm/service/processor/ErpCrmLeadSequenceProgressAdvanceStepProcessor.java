package app.erp.crm.service.processor;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.support.SequenceStepAdvancer;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCrmLeadSequenceProgress advanceStep per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含销售序列步骤推进编排：completionCondition 匹配 + 序列完成判定 + 下一步 autoCreateEvent。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadSequenceProgressAdvanceStepProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    SequenceStepAdvancer sequenceStepAdvancer;

    @Inject
    IErpCrmLeadBiz leadBiz;

    @Inject
    IErpCrmEventBiz eventBiz;

    public ErpCrmLeadSequenceProgress advanceStep(Long progressId, Long eventId, IServiceContext context) {
        ErpCrmLeadSequenceProgress progress = requireProgress(progressId);
        ErpCrmEvent event = eventBiz.requireEntity(String.valueOf(eventId), null, context);
        List<ErpCrmSequenceStep> steps = loadSteps(progress.getSequenceId());

        SequenceStepAdvancer.AdvanceResult result =
                sequenceStepAdvancer.advance(progress, event, steps);

        progress.setCurrentStepIndex(result.getNewStepIndex());
        if (result.isSequenceCompleted()) {
            progress.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED);
            progress.setCompletedAt(result.getCompletedAt() != null ? Timestamp.valueOf(result.getCompletedAt()) : null);
        }
        dao().updateEntity(progress);

        // 推进时若下一步 autoCreateEvent → 建下一步 Event
        if (!result.isSequenceCompleted() && result.isEventCreationNeeded() && result.getNextStep() != null) {
            ErpCrmLead lead = leadBiz.requireEntity(String.valueOf(progress.getLeadId()), null, context);
            createEventForStep(result.getNextStep(), lead, progress, context);
        }
        return progress;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmLeadSequenceProgress requireProgress(Long progressId) {
        ErpCrmLeadSequenceProgress progress = dao().getEntityById(progressId);
        if (progress == null) {
            throw new UnknownEntityException(ErpCrmLeadSequenceProgress.class.getName(), progressId);
        }
        return progress;
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
