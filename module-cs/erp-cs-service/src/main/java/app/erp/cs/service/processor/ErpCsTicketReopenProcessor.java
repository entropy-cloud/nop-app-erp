package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.statemachine.ErpCsTicketStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCsTicket reopen per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含工单驳回重开编排（状态迁移 RESOLVED→IN_PROGRESS + 审计 + 取消未响应调查）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketReopenProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpCsSurveyBiz surveyBiz;
    @Inject
    ErpCsTicketStateMachine stateMachine;

    public ErpCsTicket reopen(String ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        try {
            stateMachine.assertCanReopen(from);
        } catch (NopException e) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_RESOLVED, e);
        }
        ticket.setStatus(stateMachine.reopenTargetStatus());
        // 恢复计时：保留原 startDateTime（duration 在下次 resolve 时累加重算，因 startDateTime 不变）
        dao().updateEntity(ticket);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, stateMachine.reopenTargetStatus(),
                "驳回重开", context);

        // reopen 时取消未响应的调查（避免误发）
        cancelUnrespondedSurvey(ticketId, context);
        return ticket;
    }

    private void cancelUnrespondedSurvey(String ticketId, IServiceContext context) {
        // 查找该工单未响应的调查（respondedAt 空），删除以避免误发
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = surveyBiz.findList(q, null, context);
        for (ErpCsSurvey survey : list) {
            if (survey.getRespondedAt() == null) {
                surveyBiz.delete(String.valueOf(survey.getId()), context);
            }
        }
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
