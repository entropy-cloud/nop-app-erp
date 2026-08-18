package app.erp.cs.service.processor;

import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.TimerSessionCalculator;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDateTime;

/**
 * stopTimer per-mutation Processor（RC-R1.66，UC-CS-11 ④；owner doc §2.2/§2.3）。
 *
 * <p>停止 = 置 STOPPED + 生成 ErpCsTimeEntry（startTime=会话开始、endTime=停止时刻、
 * duration = 墙钟 − Σ暂停（分钟）、source=TIMER_IMPORT、approvalStatus=NULL 即 DRAFT 待补充提交）。
 * 停止操作发生在 >12h 后 → 12h 惰性结算先行封顶（duration=720），本次 stop 幂等返回已结算会话（plan D3）。
 */
public class ErpCsTicketTimerSessionStopTimerProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCsTicketTimerSessionOps ops;

    public ErpCsTicketTimerSession stopTimer(Long sessionId, IServiceContext context) {
        if (!ErpCsConfigs.isTimeTrackingEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId);
        }
        ErpCsTicketTimerSession session = ops.requireSession(sessionId);
        // 12h 惰性结算先行：本次调用完成封顶结算（含 720 分钟封顶条目）→ 幂等返回，不重复生成条目
        if (ops.settleIfOverdue(session, context)) {
            return session;
        }
        if (ErpCsConstants.TIMER_SESSION_STATUS_STOPPED.equals(session.getStatus())) {
            throw new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN)
                    .param(ErpCsErrors.ARG_SESSION_ID, session.getId())
                    .param(ErpCsErrors.ARG_CURRENT_STATUS, session.getStatus());
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        ops.closeOpenPause(session, now);
        long wall = TimerSessionCalculator.minutesBetween(session.getStartTime().toLocalDateTime(), now);
        long duration = Math.max(0L, wall - (session.getCumulativePauseMinutes() == null ? 0L : session.getCumulativePauseMinutes()));
        ops.stopSession(session, now, context);
        ops.generateTimeEntry(session, duration, context);
        return session;
    }

    protected IEntityDao<ErpCsTicketTimerSession> dao() {
        return daoProvider.daoFor(ErpCsTicketTimerSession.class);
    }
}
