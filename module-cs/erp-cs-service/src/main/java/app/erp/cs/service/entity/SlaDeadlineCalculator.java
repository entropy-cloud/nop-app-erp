package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * SLA 截止时间（deadlineDateTime）计算器。权威：{@code docs/design/customer-service/sla.md §1.3}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 1 Decision。
 *
 * <p>计算模式：
 * <ul>
 *   <li>日历小时模式（{@code isWorkingDays=false}）：{@code deadline = from + resolveHours}</li>
 *   <li>工作日模式（{@code isWorkingDays=true}）：按 resolveDays/resolveHours 跳周末（Mon-Fri）。
 *       <b>首版仅跳周末</b>，不依赖节假日日历（plan Decision：节假日主数据未确认存在，归 Non-Goal）。
 *       工作时段窗口（workingHourStart/End）ORM 无字段，不实现精确工时累计。</li>
 * </ul>
 *
 * <p>策略无 resolveHours 且无 resolveDays → 返回 null（无法计算截止时间）。
 */
public final class SlaDeadlineCalculator {

    private SlaDeadlineCalculator() {
    }

    /**
     * 计算截止时间。
     *
     * @param from     起算时间（通常为 now）
     * @param policy   SLA 策略
     * @return 截止时间；策略无 hours/days 配置返回 null
     */
    public static LocalDateTime calculate(LocalDateTime from, ErpCsSlaPolicy policy) {
        if (policy == null) {
            return null;
        }
        Integer hours = policy.getResolveHours();
        Integer days = policy.getResolveDays();
        boolean hasHours = hours != null && hours > 0;
        boolean hasDays = days != null && days > 0;
        if (!hasHours && !hasDays) {
            return null;
        }
        Boolean workingDaysFlag = policy.getIsWorkingDays();
        boolean workingDayMode = workingDaysFlag != null && workingDaysFlag;

        if (!workingDayMode) {
            // 日历小时模式：直接 from + hours（days 折算为小时累加）
            long totalHours = (hasHours ? hours : 0) + (hasDays ? (long) days * 24 : 0);
            if (totalHours <= 0) {
                return null;
            }
            return from.plusHours(totalHours);
        }
        // 工作日模式：跳周末（Mon-Fri）。days 优先，hours 折算。
        long totalHoursToAdd = (hasHours ? hours : 0) + (hasDays ? (long) days * 24 : 0);
        if (totalHoursToAdd <= 0) {
            return null;
        }
        return addWorkingHoursSkippingWeekends(from, totalHoursToAdd);
    }

    /**
     * 按"自然小时"累加，但落在周末的时间跳到下一个周一 00:00 继续。
     * <p>注：首版不含工作时段窗口（午休/班次）和节假日，仅跳 Sat/Sun。
     */
    static LocalDateTime addWorkingHoursSkippingWeekends(LocalDateTime from, long hoursToAdd) {
        LocalDateTime cursor = from;
        long remaining = hoursToAdd;
        while (remaining > 0) {
            // 若当前落在周末，推到下周一 00:00
            cursor = skipToMondayIfWeekend(cursor);
            long tillEndOfDay = 24 - cursor.getHour();
            long chunk = Math.min(remaining, tillEndOfDay);
            LocalDateTime candidate = cursor.plusHours(chunk);
            // 如果跨越到次日且次日是周末，回退到当日 24:00（即次日 00:00 再由循环跳过）
            if (isWeekend(candidate.toLocalDate()) && candidate.getHour() != 0) {
                candidate = cursor.toLocalDate().plusDays(1).atStartOfDay();
                chunk = 24 - cursor.getHour();
            }
            cursor = candidate;
            remaining -= chunk;
        }
        // 最终若落在周末，推到周一
        return skipToMondayIfWeekend(cursor);
    }

    private static LocalDateTime skipToMondayIfWeekend(LocalDateTime t) {
        DayOfWeek dow = t.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            return t.plusDays(2).toLocalDate().atStartOfDay();
        }
        if (dow == DayOfWeek.SUNDAY) {
            return t.plusDays(1).toLocalDate().atStartOfDay();
        }
        return t;
    }

    private static boolean isWeekend(java.time.LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** 计算两时间点之间的分钟差（abs），用于 SLA 处理时长。 */
    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        return Math.abs(ChronoUnit.MINUTES.between(from, to));
    }
}
