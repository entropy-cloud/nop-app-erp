package app.erp.cs.service.processor;

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

import java.time.LocalDateTime;

/**
 * resumeTimer per-mutation Processor（RC-R1.66，UC-CS-11 ③；owner doc §2.3）。
 *
 * <p>PAUSED→RUNNING；未闭合暂停结算入累计暂停（cumulativePauseMinutes）；
 * 12h 惰性结算先行（结算后已 STOPPED 则拒绝恢复）。
 */
public class ErpCsTicketTimerSessionResumeTimerProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCsTicketTimerSessionOps ops;

    public ErpCsTicketTimerSession resumeTimer(String sessionId, IServiceContext context) {
        if (!ErpCsConfigs.isTimeTrackingEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId);
        }
        ErpCsTicketTimerSession session = ops.requireSession(sessionId);
        // 12h 惰性结算先行（REQUIRES_NEW 独立事务物化）：超时封顶停止后本操作按「会话已停止」拒绝
        if (ops.settleIfOverdueInNewTx(sessionId, context)) {
            throw new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId)
                    .param(ErpCsErrors.ARG_CURRENT_STATUS, ErpCsConstants.TIMER_SESSION_STATUS_STOPPED);
        }
        if (ErpCsConstants.TIMER_SESSION_STATUS_STOPPED.equals(session.getStatus())) {
            throw notOpen(session);
        }
        if (!ErpCsConstants.TIMER_SESSION_STATUS_PAUSED.equals(session.getStatus())) {
            throw illegalState(session, ErpCsConstants.TIMER_SESSION_STATUS_PAUSED);
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        ops.closeOpenPause(session, now);
        session.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING);
        dao().updateEntity(session);
        return session;
    }

    protected NopException notOpen(ErpCsTicketTimerSession session) {
        return new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN)
                .param(ErpCsErrors.ARG_SESSION_ID, session.getId())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, session.getStatus());
    }

    protected NopException illegalState(ErpCsTicketTimerSession session, String expected) {
        return new NopException(ErpCsErrors.ERR_CS_TIMER_ILLEGAL_STATE)
                .param(ErpCsErrors.ARG_SESSION_ID, session.getId())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, session.getStatus())
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpCsTicketTimerSession> dao() {
        return daoProvider.daoFor(ErpCsTicketTimerSession.class);
    }
}
