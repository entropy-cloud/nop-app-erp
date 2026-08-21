package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.service.ErpCsConstants;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 计时器会话纯函数计算器（RC-R1.66，plan D1/D2/D3；镜像 {@link SlaDeadlineCalculator} 范式）。
 *
 * <p>时长口径（owner doc time-tracking.md §2.3）：有效时长 = 墙钟时长 − 累计暂停时长。
 * 暂停中的会话「当前未闭合暂停」计入暂停侧；12h 封顶时刻按 startTime + maxMinutes + 总暂停 反推。
 */
public final class TimerSessionCalculator {

    private TimerSessionCalculator() {
    }

    /** 会话进行中（RUNNING/PAUSED 均占据单计时器槽位，plan D1）。 */
    public static boolean isOpen(ErpCsTicketTimerSession session) {
        String status = session.getStatus();
        return ErpCsConstants.TIMER_SESSION_STATUS_RUNNING.equals(status)
                || ErpCsConstants.TIMER_SESSION_STATUS_PAUSED.equals(status);
    }

    /** 截至此刻的总暂停分钟（累计 + 未闭合暂停）。 */
    public static long totalPauseMinutes(ErpCsTicketTimerSession session, LocalDateTime now) {
        long cumulative = session.getCumulativePauseMinutes() == null ? 0L : session.getCumulativePauseMinutes();
        if (ErpCsConstants.TIMER_SESSION_STATUS_PAUSED.equals(session.getStatus())
                && session.getPauseStartDateTime() != null) {
            cumulative += minutesBetween(session.getPauseStartDateTime().toLocalDateTime(), now);
        }
        return cumulative;
    }

    /** 有效运行分钟 = 墙钟(开始→now) − 总暂停。 */
    public static long elapsedActiveMinutes(ErpCsTicketTimerSession session, LocalDateTime now) {
        long wall = minutesBetween(session.getStartTime().toLocalDateTime(), now);
        return Math.max(0L, wall - totalPauseMinutes(session, now));
    }

    /** 12h 上限判定：进行中且有效运行分钟 > maxActiveMinutes。 */
    public static boolean isOverdue(ErpCsTicketTimerSession session, LocalDateTime now, long maxActiveMinutes) {
        return isOpen(session) && elapsedActiveMinutes(session, now) > maxActiveMinutes;
    }

    /** 封顶停止时刻 = startTime + maxActiveMinutes + 总暂停（使 duration 恰为 maxActiveMinutes，plan D3）。 */
    public static LocalDateTime cappedStopTime(ErpCsTicketTimerSession session, LocalDateTime now,
                                               long maxActiveMinutes) {
        return session.getStartTime().toLocalDateTime()
                .plusMinutes(maxActiveMinutes).plusMinutes(totalPauseMinutes(session, now));
    }

    /** D2 映射（agentId String 化后为恒等直写）：userId 字符串直写 ErpCsTimeEntry.agentId，空白 → null。 */
    public static String toEntryAgentId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return userId.trim();
    }

    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        return Math.abs(ChronoUnit.MINUTES.between(from, to));
    }
}
