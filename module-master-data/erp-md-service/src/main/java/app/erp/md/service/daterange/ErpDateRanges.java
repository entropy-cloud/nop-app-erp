package app.erp.md.service.daterange;

import app.erp.md.dao.daterange.IDateRange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 日期区间运算原语（C3 日期范围有效性模式，docs/design/date-ranged-validity-pattern.md §3.2 / §5）。
 *
 * <p>纯函数工具类，无 IoC 依赖，跨域 service 经 {@code app-erp-master-data-service} 依赖即可调用。
 *
 * <p>语义约定（owner doc §3.1）：[validFrom, validTo] 双侧闭区间；任一侧 {@code null} 表示该侧开放。
 * TIMESTAMP 变体（如 {@code ErpSalPricingRule}）调用方应先截断到 {@link LocalDate} 或使用
 * {@link #contains(IDateRange, Date)} 重载。
 */
public final class ErpDateRanges {

    private ErpDateRanges() {
    }

    /**
     * 判断 date 是否落在区间内（含起止日）。
     *
     * <p>NULL 处理：{@code from == null} 视为左侧开放；{@code to == null} 视为右侧开放；
     * 两者均 {@code null} 视为永久有效（返回 {@code true}）。
     */
    public static boolean contains(IDateRange range, LocalDate date) {
        if (range == null || date == null) {
            return false;
        }
        LocalDate from = range.getValidFrom();
        LocalDate to = range.getValidTo();
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    /**
     * {@link java.util.Date} 重载，适配 TIMESTAMP 变体（如 {@code ErpSalPricingRule}）。
     * 内部按系统默认时区截断到 {@link LocalDate} 后比较。
     */
    public static boolean contains(IDateRange range, Date date) {
        if (date == null) {
            return false;
        }
        return contains(range, toLocalDate(date));
    }

    /**
     * 判断两区间是否有交集。
     *
     * <p>NULL 处理：{@code from == null} 视为负无穷；{@code to == null} 视为正无穷。
     * 两记录端点完全相同视为重叠（端点闭区间）。
     */
    public static boolean overlaps(IDateRange r1, IDateRange r2) {
        if (r1 == null || r2 == null) {
            return false;
        }
        LocalDate f1 = r1.getValidFrom();
        LocalDate t1 = r1.getValidTo();
        LocalDate f2 = r2.getValidFrom();
        LocalDate t2 = r2.getValidTo();
        // r1 完全在 r2 之前：t1 < f2
        if (t1 != null && f2 != null && t1.isBefore(f2)) {
            return false;
        }
        // r1 完全在 r2 之后：f1 > t2
        if (f1 != null && t2 != null && f1.isAfter(t2)) {
            return false;
        }
        return true;
    }

    /**
     * 在区间集合中筛选在 date 当天生效的记录。
     *
     * @param ranges 区间集合（{@code null} 返回空 List）
     * @param date  目标日（{@code null} 返回空 List）
     * @return 命中记录的新 List（保持入参顺序）；调用方对结果自行按策略处理
     */
    public static <T extends IDateRange> List<T> effectiveOn(List<T> ranges, LocalDate date) {
        List<T> hits = new ArrayList<>();
        if (ranges == null || date == null) {
            return hits;
        }
        for (T r : ranges) {
            if (contains(r, date)) {
                hits.add(r);
            }
        }
        return hits;
    }

    /**
     * 返回同维度区间集合的最大重叠数（同一时刻最多 N 条同时生效）。
     *
     * <p>用于诊断「同维度是否最多 1 条」的互斥策略校验前置检查（{@code longestOverlap > 1} 即存在重叠）。
     *
     * <p>算法：sweep line。所有端点展开为事件（from +1 / to -1），按时间升序扫描累计最大值。
     * NULL 端点视为 ±{@link LocalDate#MIN}/{@link LocalDate#MAX}。
     *
     * @param ranges 区间集合（{@code null} 或空返回 0）
     */
    public static int longestOverlap(List<? extends IDateRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return 0;
        }
        List<long[]> events = new ArrayList<>(ranges.size() * 2);
        for (int i = 0; i < ranges.size(); i++) {
            IDateRange r = ranges.get(i);
            if (r == null) {
                continue;
            }
            LocalDate from = r.getValidFrom();
            LocalDate to = r.getValidTo();
            // from=null 视为负无穷；to=null 视为正无穷
            long fromEpoch = from != null ? from.toEpochDay() : LocalDate.MIN.toEpochDay();
            long toEpoch = to != null ? to.toEpochDay() : LocalDate.MAX.toEpochDay();
            events.add(new long[]{fromEpoch, +1});
            events.add(new long[]{toEpoch + 1, -1}); // 闭区间：to+1 才退出
        }
        // 按 epoch 升序；同时刻事件 +1 优先（先入后出语义：扫描重叠峰值）
        events.sort((a, b) -> {
            if (a[0] != b[0]) {
                return Long.compare(a[0], b[0]);
            }
            return Long.compare(a[1], b[1]); // +1（进入）优先于 -1（退出），峰值取进入侧
        });
        int cur = 0;
        int peak = 0;
        for (long[] ev : events) {
            cur += ev[1];
            if (cur > peak) {
                peak = cur;
            }
        }
        return peak;
    }

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            // java.sql.Date.toLocalDate 直接走，无时区歧义
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
}
