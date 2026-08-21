package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.SlaDeadlineCalculator;
import app.erp.cs.service.statemachine.ErpCsTicketStateMachine;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCsTicket resolve per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含工单解决编排（SLA 计时停止 + duration 计算 + isSlaCompleted 判定 + CSAT 触发 +
 * 无匹配知识库建议创建推送）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketResolveProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpCsTicketResolveProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpCsSurveyBiz surveyBiz;
    @Inject
    ErpCsTicketStateMachine stateMachine;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpCsTicket resolve(String ticketId, String resolution, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        try {
            stateMachine.assertCanResolve(from);
        } catch (NopException e) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS, e);
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        // 停 SLA 计时算 duration（分钟）；startDateTime 为空时 duration 留空
        if (ticket.getStartDateTime() != null) {
            long minutes = SlaDeadlineCalculator.minutesBetween(ticket.getStartDateTime().toLocalDateTime(), now);
            ticket.setDuration((int) minutes);
        }
        // 标记 isSlaCompleted = resolvedAt <= deadlineDateTime
        LocalDateTime deadline = ticket.getDeadlineDateTime() != null ? ticket.getDeadlineDateTime().toLocalDateTime() : null;
        boolean completed = deadline == null || !now.isAfter(deadline);
        ticket.setIsSlaCompleted(completed);
        ticket.setStatus(stateMachine.resolveTargetStatus());
        if (resolution != null) {
            ticket.setRemark(resolution);
        }
        dao().updateEntity(ticket);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, stateMachine.resolveTargetStatus(),
                "标记解决: " + (resolution == null ? "" : resolution), context);

        // CSAT 触发（config-gated）：trigger-status 默认 RESOLVED
        if (ErpCsConfigs.isSurveyEnabled()
                && Objects.equals(ErpCsConfigs.getSurveyTriggerStatus(), ErpCsConstants.TICKET_STATUS_RESOLVED)) {
            surveyBiz.createSurvey(ticketId, context);
        }
        // 无匹配知识库建议创建推送（UC-CS-05 ⑨，config-gated）
        suggestKnowledgeCreation(ticket, context);
        return ticket;
    }

    /**
     * UC-CS-05 ⑨ 后置（RC-R1.69，P1-RC-058，config-gated by
     * {@code erp-cs.knowledge-suggest-on-resolve} 默认 true）：工单 resolve 时若无 ADOPT_KNOWLEDGE
     * 审计行 → 派发「建议创建知识库条目」notify（模板种子 7204 {@code cs.knowledge-suggest-create}，
     * USER_LIST {@code ${handlerUserId}}）。接收人 = handler（assignedToId 回退 operatorId）。
     * try/catch 降级范式（notify 失败不阻断 resolve 主流程）。
     */
    protected void suggestKnowledgeCreation(ErpCsTicket ticket, IServiceContext context) {
        if (!ErpCsConfigs.isKnowledgeSuggestOnResolve()) {
            return;
        }
        if (hasAdoptKnowledgeAction(ticket.getId(), context)) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("handlerUserId", ticket.getAssignedToId() != null
                    ? ticket.getAssignedToId() : context.getUserId());
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_KNOWLEDGE_SUGGEST_CREATE, ctx, context);
        } catch (Exception e) {
            LOG.warn("knowledge-suggest-create 通知派发失败（降级，resolve 主流程继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    private boolean hasAdoptKnowledgeAction(String ticketId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_ADOPT_KNOWLEDGE));
        q.setLimit(1);
        return !ticketActionBiz.findList(q, null, context).isEmpty();
    }

    private ErpCsTicket requireTicket(String ticketId, IServiceContext context) {
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        ErpCsTicket ticket = dao().getEntityById(ticketId);
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return ticket;
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current, String expected, Throwable cause) {
        return new NopException(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION, cause)
                .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, expected);
    }

    private void writeAction(ErpCsTicket ticket, String actionType, String fromStatus, String toStatus,
                             String content, IServiceContext context) {
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticket.getId());
        action.setActionType(actionType);
        action.setFromStatus(fromStatus);
        action.setToStatus(toStatus);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    private IEntityDao<ErpCsTicket> dao() {
        return daoProvider.daoFor(ErpCsTicket.class);
    }
}
