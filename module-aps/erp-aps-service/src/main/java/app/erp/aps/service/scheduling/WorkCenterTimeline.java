package app.erp.aps.service.scheduling;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个工作中心（machineId）的繁忙时间轴（内存模型，不持久化）。
 *
 * <p>繁忙区间来源：（1）维护停机 {@code ErpApsConstraint}（MAINTENANCE 类型）；
 * （2）已被排定的 PLANNED 工序占用区间。capacity=1：同一时段只能容纳一个工序。
 *
 * <p>提供前向/后向可用时段查找：
 * <ul>
 *   <li>{@link #findFreeSlotForward}：从 {@code from} 起正向找最早可容纳 {@code durationMinutes} 的空闲起点。</li>
 *   <li>{@link #findFreeSlotBackward}：从 {@code before} 起逆向找最晚可容纳 {@code durationMinutes} 的空闲起点。</li>
 * </ul>
 *
 * <p>设计参考 {@code docs/design/aps/scheduling.md §1.2}（ErpApsTimeSlot 概念仅内存表达）。
 */
public class WorkCenterTimeline {

    private final long machineId;
    /** 已排序的繁忙区间（按 start 升序，区间互不重叠）。 */
    private final List<Interval> busy = new ArrayList<>();

    public WorkCenterTimeline(long machineId) {
        this.machineId = machineId;
    }

    public long getMachineId() {
        return machineId;
    }

    public List<Interval> getBusy() {
        return Collections.unmodifiableList(busy);
    }

    /** 追加一个繁忙区间（维护停机或已排定工序）。自动按 start 重排。 */
    public void addBusy(LocalDateTime start, LocalDateTime end, String reason) {
        if (start == null || end == null || !start.isBefore(end)) {
            return;
        }
        busy.add(new Interval(start, end, reason));
        busy.sort(Interval::compareTo);
    }

    /**
     * 前向查找：返回最早的空闲起点 {@code s}，满足 {@code s >= from} 且 {@code [s, s+durationMinutes]}
     * 不与任何繁忙区间重叠。找不到（超出展望期 {@code horizonEnd}）返回 {@code null}。
     */
    public LocalDateTime findFreeSlotForward(LocalDateTime from, long durationMinutes, LocalDateTime horizonEnd) {
        if (from == null || durationMinutes <= 0) {
            return null;
        }
        LocalDateTime candidate = from;
        int guard = 0;
        // 每次重叠则跳过该繁忙区间的 end，最多循环 busy.size()+1 次。
        while (guard++ <= busy.size() + 1) {
            LocalDateTime end = candidate.plusMinutes(durationMinutes);
            if (horizonEnd != null && end.isAfter(horizonEnd)) {
                return null;
            }
            Interval hit = firstOverlap(candidate, end);
            if (hit == null) {
                return candidate;
            }
            // 跳到繁忙区间结束之后重试
            candidate = hit.end;
        }
        return null;
    }

    /**
     * 后向查找：返回最晚的空闲起点 {@code s}，满足 {@code s + durationMinutes <= before} 且
     * {@code [s, s+durationMinutes]} 不与任何繁忙区间重叠。找不到返回 {@code null}。
     */
    public LocalDateTime findFreeSlotBackward(LocalDateTime before, long durationMinutes) {
        if (before == null || durationMinutes <= 0) {
            return null;
        }
        LocalDateTime candidateEnd = before;
        int guard = 0;
        while (guard++ <= busy.size() + 1) {
            LocalDateTime start = candidateEnd.minusMinutes(durationMinutes);
            if (start == null) {
                return null;
            }
            Interval hit = firstOverlap(start, candidateEnd);
            if (hit == null) {
                return start;
            }
            // 收缩到繁忙区间开始之前重试
            candidateEnd = hit.start;
        }
        return null;
    }

    private Interval firstOverlap(LocalDateTime s, LocalDateTime e) {
        for (Interval iv : busy) {
            if (iv.overlaps(s, e)) {
                return iv;
            }
        }
        return null;
    }

    /** 繁忙区间（半开半闭 [start, end)）。 */
    public static final class Interval implements Comparable<Interval> {
        final LocalDateTime start;
        final LocalDateTime end;
        final String reason;

        public Interval(LocalDateTime start, LocalDateTime end, String reason) {
            this.start = start;
            this.end = end;
            this.reason = reason;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public String getReason() {
            return reason;
        }

        boolean overlaps(LocalDateTime s, LocalDateTime e) {
            return s.isBefore(end) && e.isAfter(start);
        }

        @Override
        public int compareTo(Interval o) {
            return this.start.compareTo(o.start);
        }
    }
}
