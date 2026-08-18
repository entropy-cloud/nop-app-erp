package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTimeEntryBiz;
import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.TimerSessionCalculator;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 计时器会话共享步骤（RC-R1.66，plan D3/D6）：12h 惰性结算 + 停止生成条目。
 *
 * <p>被四个 per-mutation Processor 与 findActiveTimer 共用；方法非 final，下游可经 Delta 同名 bean 覆盖定制。
 * 结算语义（plan D3）：会话进行中且有效运行分钟 &gt; max-hours×60 时，置 STOPPED +
 * stopTime = startTime + 上限 + 总暂停（反推封顶时刻）+ 生成封顶条目（duration=720，source=TIMER_IMPORT）。
 *
 * <p>两种结算事务边界：{@link #settleIfOverdue}（当前事务，调用方正常提交持久化——start/stop 成功路径）与
 * {@link #settleIfOverdueInNewTx}（REQUIRES_NEW 独立事务，pause/resume 拒绝路径与 findActiveTimer 读取路径
 * ——外层异常回滚/只读会话不影响结算物化，镜像 R1.65 CsTicketMonthSeqCodeRuleVariable REQUIRES_NEW 先例）。
 */
public class ErpCsTicketTimerSessionOps {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpCsTicketTimerSessionOps.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTimeEntryBiz timeEntryBiz;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    ITransactionTemplate transactionTemplate;

    /**
     * 12h 惰性结算（当前事务）：超限则封顶停止并生成条目；调用方正常返回提交后持久化。
     *
     * @return true = 本次调用完成结算（会话现已 STOPPED）；false = 未超限或本已停止
     */
    public boolean settleIfOverdue(ErpCsTicketTimerSession session, IServiceContext context) {
        if (!TimerSessionCalculator.isOpen(session)) {
            return false;
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        long maxMinutes = maxActiveMinutes();
        if (!TimerSessionCalculator.isOverdue(session, now, maxMinutes)) {
            return false;
        }
        doSettle(session, now, maxMinutes, context);
        return true;
    }

    /**
     * 12h 惰性结算（REQUIRES_NEW 独立事务）：按 id 重载会话结算并提交——外层事务后续回滚/只读不影响结算物化。
     * 用于 pause/resume 拒绝路径与 findActiveTimer 读取路径。
     */
    public boolean settleIfOverdueInNewTx(Long sessionId, IServiceContext context) {
        Boolean settled = ormTemplate.runInNewSession(session ->
                transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                    ErpCsTicketTimerSession reloaded = dao().getEntityById(sessionId);
                    return reloaded != null && settleIfOverdue(reloaded, context);
                }));
        return Boolean.TRUE.equals(settled);
    }

    /** 封顶结算本体：关闭未闭合暂停 → 置 STOPPED（封顶时刻反推）→ 生成 720 分钟封顶条目。 */
    protected void doSettle(ErpCsTicketTimerSession session, LocalDateTime now, long maxMinutes,
                            IServiceContext context) {
        closeOpenPause(session, now);
        LocalDateTime cappedStop = TimerSessionCalculator.cappedStopTime(session, now, maxMinutes);
        stopSession(session, cappedStop, context);
        generateTimeEntry(session, maxMinutes, context);
    }

    /** 关闭未闭合暂停：累计暂停 += 暂停起点→now；pauseStartDateTime 置空。 */
    public void closeOpenPause(ErpCsTicketTimerSession session, LocalDateTime now) {
        if (session.getPauseStartDateTime() != null) {
            long open = TimerSessionCalculator.minutesBetween(session.getPauseStartDateTime().toLocalDateTime(), now);
            long cumulative = session.getCumulativePauseMinutes() == null ? 0L : session.getCumulativePauseMinutes();
            session.setCumulativePauseMinutes((int) (cumulative + open));
            session.setPauseStartDateTime(null);
        }
    }

    /** 置会话 STOPPED（activeFlag 释放单计时器 UK 槽位，plan D1）。 */
    public void stopSession(ErpCsTicketTimerSession session, LocalDateTime stopTime, IServiceContext context) {
        session.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_STOPPED);
        session.setActiveFlag(null);
        session.setStopTime(java.sql.Timestamp.valueOf(stopTime));
        dao().updateEntity(session);
    }

    /**
     * 停止/封顶结算生成 ErpCsTimeEntry（UC-CS-11 ④）：duration 分钟、source=TIMER_IMPORT、
     * approvalStatus 留 NULL（= DRAFT，待客服补充 description/isBillable 后 submit，plan D4）；
     * D6 费率：isBillable 且 billingRate 缺失且 default-billing-rate&gt;0 时填充 + billableAmount = duration/60 × rate。
     */
    public ErpCsTimeEntry generateTimeEntry(ErpCsTicketTimerSession session, long durationMinutes,
                                            IServiceContext context) {
        ErpCsTimeEntry entry = timeEntryBiz.newEntity();
        entry.setOrgId(session.getOrgId());
        entry.setTicketId(session.getTicketId());
        long entryAgent = TimerSessionCalculator.toEntryAgentId(session.getAgentId());
        if (!Objects.equals(String.valueOf(entryAgent), session.getAgentId())) {
            // D2 残留风险登记：非数字 userId 映射为 0 哨兵（BIGINT agentId 为语义孤儿列，归 successor 统一）
            LOG.warn("计时条目 agentId 映射哨兵 0：session.agentId={} 非数字 userId", session.getAgentId());
        }
        entry.setAgentId(entryAgent);
        entry.setStartTime(session.getStartTime());
        entry.setEndTime(session.getStopTime());
        entry.setDuration((int) durationMinutes);
        entry.setIsBillable(Boolean.TRUE);
        BigDecimal rate = ErpCsConfigs.getDefaultBillingRate();
        if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
            entry.setBillingRate(rate);
            entry.setBillableAmount(rate.multiply(BigDecimal.valueOf(durationMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        }
        entry.setSource(ErpCsConstants.TIME_ENTRY_SOURCE_TIMER_IMPORT);
        timeEntryBiz.saveEntity(entry, null, context);
        return entry;
    }

    /** 按会话 id 加载（不存在抛领域错误码）。 */
    public ErpCsTicketTimerSession requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_FOUND)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId);
        }
        ErpCsTicketTimerSession session = dao().getEntityById(sessionId);
        if (session == null) {
            throw new NopException(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_FOUND)
                    .param(ErpCsErrors.ARG_SESSION_ID, sessionId);
        }
        return session;
    }

    public long maxActiveMinutes() {
        int hours = ErpCsConfigs.getTimerMaxHours();
        return (hours <= 0 ? 12 : hours) * 60L;
    }

    protected IEntityDao<ErpCsTicketTimerSession> dao() {
        return daoProvider.daoFor(ErpCsTicketTimerSession.class);
    }
}
