package app.erp.hr.service.scheduling;

import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次时间 + 跨天班次（夜班）迟到/早退/缺勤计算（shift-scheduling.md §4.1/§4.2）。
 *
 * <p>纯逻辑无 IoC，便于单测覆盖。
 */
public final class ShiftAttendanceCalculator {

    private ShiftAttendanceCalculator() {
    }

    /**
     * 判断班次是否为跨天班次（夜班）：endTime 早于 startTime（如 23:00→08:00）。
     */
    public static boolean isCrossDayShift(ErpHrShift shift) {
        LocalTime start = parseTime(shift.getStartTime());
        LocalTime end = parseTime(shift.getEndTime());
        return end.isBefore(start) || end.equals(start);
    }

    /**
     * 计算迟到分钟数（0 表示未迟到）。
     *
     * @param shift         班次模板
     * @param clockIn       实际签到时间（可空）
     * @param assignmentDate 排班日期（日历归属开始日期，§九.6）
     * @return 迟到分钟数（>0 = 迟到；0 = 准时或未打卡）
     */
    public static int calcLateMinutes(ErpHrShift shift, LocalDateTime clockIn, LocalDate assignmentDate) {
        if (clockIn == null) {
            return 0;
        }
        int graceLate = nzInt(shift.getGraceLateMinutes(), 15);
        LocalTime startTime = parseTime(shift.getStartTime());
        LocalDateTime expectedClockIn = LocalDateTime.of(assignmentDate, startTime);
        long diffMinutes = java.time.Duration.between(expectedClockIn, clockIn).toMinutes();
        if (diffMinutes <= graceLate) {
            return 0;
        }
        return (int) diffMinutes;
    }

    /**
     * 计算早退分钟数（0 表示未早退）。
     *
     * <p>跨天班次 endTime 取次日（§4.2）。
     *
     * @param shift         班次模板
     * @param clockOut      实际签退时间（可空）
     * @param assignmentDate 排班日期
     */
    public static int calcEarlyLeaveMinutes(ErpHrShift shift, LocalDateTime clockOut, LocalDate assignmentDate) {
        if (clockOut == null) {
            return 0;
        }
        int graceEarly = nzInt(shift.getGraceEarlyLeaveMinutes(), 15);
        LocalTime endTime = parseTime(shift.getEndTime());
        LocalDate endDate = isCrossDayShift(shift) ? assignmentDate.plusDays(1) : assignmentDate;
        LocalDateTime expectedClockOut = LocalDateTime.of(endDate, endTime);
        long diffMinutes = java.time.Duration.between(clockOut, expectedClockOut).toMinutes();
        if (diffMinutes <= graceEarly) {
            return 0;
        }
        return (int) diffMinutes;
    }

    /**
     * 判断是否缺勤：有排班且 requireClockIn=true 但无 clockIn → 缺勤。
     */
    public static boolean isAbsentByNoClockIn(ErpHrShift shift, LocalDateTime clockIn) {
        if (shift.getRequireClockIn() == null || !shift.getRequireClockIn()) {
            return false;
        }
        return clockIn == null;
    }

    static LocalTime parseTime(String hhmm) {
        if (hhmm == null || hhmm.trim().isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_CROSS_DAY_INVALID)
                    .param(ErpHrErrors.ARG_SHIFT_ID, "missing-time");
        }
        try {
            String[] parts = hhmm.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalTime.of(h, m);
        } catch (Exception e) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_CROSS_DAY_INVALID, e)
                    .param(ErpHrErrors.ARG_SHIFT_ID, hhmm);
        }
    }

    static int nzInt(Integer v, int defaultValue) {
        return v == null ? defaultValue : v;
    }
}
