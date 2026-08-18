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

import java.sql.Timestamp;

/**
 * pauseTimer per-mutation Processor（RC-R1.66，UC-CS-11 ③；owner doc §2.3）。
 *
 * <p>RUNNING→PAUSED；暂停原因可选；12h 惰性结算先行（结算后已 STOPPED 则拒绝暂停）。
 */
public class ErpCsTicketTimerSessionPauseTimerProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCsTicketTimerSessionOps ops;

    public ErpCsTicketTimerSession pauseTimer(Long sessionId, String pauseReason, IServiceContext context) {
        assertTimeTrackingEnabled(sessionId);
        ErpCsTicketTimerSession session = ops.requireSession(sessionId);
        // 12h 惰性结算先行（REQUIRES_NEW 独立事务物化）：超时封顶停止后本操作按「会话已停止」拒绝
        if (ops.settleIfOverdueInNewTx(sessionId, context)) {
            throw notOpenAfterSettle(sessionId);
        }
        assertOpen(session);
        if (!ErpCsConstants.TIMER_SESSION_STATUS_RUNNING.equals(session.getStatus())) {
            throw illegalState(session, ErpCsConstants.TIMER_SESSION_STATUS_RUNNING);
        }
        session.setPauseStartDateTime(Timestamp.valueOf(CoreMetrics.currentDateTime()));
        if (pauseReason != null && !pauseReason.trim().isEmpty()) {
            session.setPauseReason(pauseReason.trim());
        }
        session.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_PAUSED);
        dao().updateEntity(session);
        return session;
    }

    /** 结算后拒绝：状态即结算后的 STOPPED（外层实体为回滚前快照，不读取其状态）。 */
    protected NopException notOpenAfterSettle(Long sessionId) {
        return new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN)
                .param(ErpCsErrors.ARG_SESSION_ID, sessionId)
                .param(ErpCsErrors.ARG_CURRENT_STATUS, ErpCsConstants.TIMER_SESSION_STATUS_STOPPED);
    }

    protected void assertTimeTrackingEnabled(Long sessionId) {
        if (!ErpCsConfigs.isTimeTrackingEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId);
        }
    }

    protected void assertOpen(ErpCsTicketTimerSession session) {
        if (ErpCsConstants.TIMER_SESSION_STATUS_STOPPED.equals(session.getStatus())) {
            throw notOpen(session);
        }
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
