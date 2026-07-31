package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.SlaDeadlineCalculator;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ErpCsTicket resolve per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含工单解决编排（SLA 计时停止 + duration 计算 + isSlaCompleted 判定 + CSAT 触发）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketResolveProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpCsSurveyBiz surveyBiz;

    public ErpCsTicket resolve(Long ticketId, String resolution, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
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
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_RESOLVED);
        if (resolution != null) {
            ticket.setRemark(resolution);
        }
        dao().updateEntity(ticket);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, ErpCsConstants.TICKET_STATUS_RESOLVED,
                "标记解决: " + (resolution == null ? "" : resolution), context);

        // CSAT 触发（config-gated）：trigger-status 默认 RESOLVED
        if (ErpCsConfigs.isSurveyEnabled()
                && Objects.equals(ErpCsConfigs.getSurveyTriggerStatus(), ErpCsConstants.TICKET_STATUS_RESOLVED)) {
            surveyBiz.createSurvey(ticketId, context);
        }
        return ticket;
    }

    private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        ErpCsTicket ticket = dao().getEntityById(ticketId);
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return ticket;
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current, String expected) {
        return new NopException(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION)
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
