package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * startTimer per-mutation Processor（RC-R1.66，UC-CS-11 ②⑨；plan D1-②/D2）。
 *
 * <p>单计时器不变量（服务端硬约束）：已有进行中（RUNNING/PAUSED）会话则拒绝
 * （{@code ERR_CS_TIMER_ALREADY_ACTIVE}——「自动停止+确认」由客户端 stop→start 编排，owner doc §2.2:92）。
 * 超时的旧会话先经 12h 惰性结算释放槽位后允许新启动。
 */
public class ErpCsTicketTimerSessionStartTimerProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCsTicketTimerSessionOps ops;

    @Inject
    IErpCsTicketBiz ticketBiz;

    public ErpCsTicketTimerSession startTimer(Long ticketId, IServiceContext context) {
        assertTimeTrackingEnabled(ticketId);
        ErpCsTicket ticket = ticketBiz.requireEntity(String.valueOf(ticketId), null, context);

        String agentId = context.getUserId();
        ErpCsTicketTimerSession active = findOpenSession(agentId);
        if (active != null) {
            // 12h 惰性结算优先：超时旧会话封顶停止后释放单计时器槽位（plan D3）
            if (ops.settleIfOverdue(active, context)) {
                active = findOpenSession(agentId);
            }
        }
        if (active != null) {
            throw new NopException(ErpCsErrors.ERR_CS_TIMER_ALREADY_ACTIVE)
                    .param(ErpCsErrors.ARG_AGENT_ID, agentId)
                    .param(ErpCsErrors.ARG_SESSION_ID, active.getId())
                    .param(ErpCsErrors.ARG_TICKET_ID, active.getTicketId());
        }

        ErpCsTicketTimerSession session = dao().newEntity();
        session.setOrgId(ticket.getOrgId());
        session.setAgentId(agentId);
        session.setTicketId(ticketId);
        LocalDateTime now = CoreMetrics.currentDateTime();
        session.setStartTime(Timestamp.valueOf(now));
        session.setCumulativePauseMinutes(0);
        session.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING);
        // 单计时器 UK 载体：进行中占位 'Y'（停止置 NULL，plan D1 选项 A）
        session.setActiveFlag(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG);
        dao().saveEntity(session);
        return session;
    }

    protected void assertTimeTrackingEnabled(Long ticketId) {
        if (!ErpCsConfigs.isTimeTrackingEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED)
                    .param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
    }

    /** 进行中会话 = activeFlag='Y'（RUNNING/PAUSED 均占位，UK(agentId,activeFlag) 单活跃约束）。 */
    protected ErpCsTicketTimerSession findOpenSession(String agentId) {
        io.nop.api.core.beans.query.QueryBean query = new io.nop.api.core.beans.query.QueryBean();
        query.addFilter(eq("agentId", agentId));
        query.addFilter(eq("activeFlag", ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG));
        query.setLimit(1);
        List<ErpCsTicketTimerSession> list = dao().findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }

    protected IEntityDao<ErpCsTicketTimerSession> dao() {
        return daoProvider.daoFor(ErpCsTicketTimerSession.class);
    }
}
