package app.erp.md.service.daterange;

import app.erp.md.dao.daterange.IDateRange;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C3 日期范围有效性模式 helper 单元测试
 * （plan 2026-07-21-2225-1 Phase 2，docs/design/date-ranged-validity-pattern.md §3.2 / §5 / §6）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@link ErpDateRanges} 4 个原语 + 边界场景（空端点、相同端点、完全包含、相邻不重叠、空集合）</li>
 *   <li>{@link ErpDateRangeOverlapValidator#enforceMutex} 正负路径（互斥拒绝 / 允许通过 / selfId 排除 / 永久无区间跳过）</li>
 * </ul>
 *
 * <p>纯函数 helper 无需 IoC 容器，直接 JUnit 测试（不入 JunitAutoTestCase 管道）。
 */
public class TestErpDateRanges {

    // ---------- 测试用 IDateRange 实现 ----------

    private static final class Range implements IDateRange {
        final Long id;
        final LocalDate from;
        final LocalDate to;

        Range(Long id, LocalDate from, LocalDate to) {
            this.id = id;
            this.from = from;
            this.to = to;
        }

        @Override
        public LocalDate getValidFrom() {
            return from;
        }

        @Override
        public LocalDate getValidTo() {
            return to;
        }

        public Long getId() {
            return id;
        }

        @Override
        public String toString() {
            return "Range[" + id + ": " + from + ".." + to + "]";
        }
    }

    /**
     * 带 priority + stackable 字段的测试 IDateRange 实现（PRIORITY / STACKABLE 策略场景）。
     */
    private static final class PriorityRange implements IDateRange {
        final Long id;
        final LocalDate from;
        final LocalDate to;
        final int priority;
        final boolean stackable;

        PriorityRange(Long id, LocalDate from, LocalDate to, int priority) {
            this(id, from, to, priority, false);
        }

        PriorityRange(Long id, LocalDate from, LocalDate to, int priority, boolean stackable) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.priority = priority;
            this.stackable = stackable;
        }

        @Override
        public LocalDate getValidFrom() {
            return from;
        }

        @Override
        public LocalDate getValidTo() {
            return to;
        }

        public Long getId() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        public boolean getStackable() {
            return stackable;
        }

        @Override
        public String toString() {
            return "PriorityRange[" + id + ": " + from + ".." + to + " prio=" + priority + " stack=" + stackable + "]";
        }
    }

    private static final Predicate<PriorityRange> STACKABLE_GETTER = pr -> Boolean.TRUE.equals(pr.getStackable());

    private static final ErrorCode ERR_TEST_OVERLAP = ErrorCode.define(
            "erp.err.test.date-range.overlap",
            "测试错误码：{entityName} 区间冲突 id={conflictId}",
            ErpDateRangeOverlapValidator.ARG_ENTITY_NAME,
            ErpDateRangeOverlapValidator.ARG_CONFLICT_ID);

    private static final ErrorCode ERR_TEST_STACKABLE_CONFLICT = ErrorCode.define(
            "erp.err.test.date-range.stackable-conflict",
            "测试错误码：{entityName} 不可叠加冲突 id={conflictId}",
            ErpDateRangeOverlapValidator.ARG_ENTITY_NAME,
            ErpDateRangeOverlapValidator.ARG_CONFLICT_ID);

    // ---------- ErpDateRanges.contains ----------

    @Test
    public void containsClosedIntervalBothEnds() {
        // 端点闭区间：起止日均视为有效
        Range r = new Range(1L, ld("2026-01-01"), ld("2026-12-31"));
        assertTrue(ErpDateRanges.contains(r, ld("2026-01-01")), "起日含");
        assertTrue(ErpDateRanges.contains(r, ld("2026-12-31")), "止日含");
        assertTrue(ErpDateRanges.contains(r, ld("2026-06-15")), "中间含");
        assertFalse(ErpDateRanges.contains(r, ld("2025-12-31")), "前一日不含");
        assertFalse(ErpDateRanges.contains(r, ld("2027-01-01")), "后一日不含");
    }

    @Test
    public void containsOpenRightSideNullValidTo() {
        // validTo=null 视为右侧开放（无失效日）
        Range r = new Range(2L, ld("2026-01-01"), null);
        assertTrue(ErpDateRanges.contains(r, ld("2026-06-15")));
        assertTrue(ErpDateRanges.contains(r, ld("2099-12-31")), "无失效日：未来日均有效");
        assertFalse(ErpDateRanges.contains(r, ld("2025-12-31")), "起日前不含");
    }

    @Test
    public void containsOpenLeftSideNullValidFrom() {
        // validFrom=null 视为左侧开放（无生效日）
        Range r = new Range(3L, null, ld("2026-12-31"));
        assertTrue(ErpDateRanges.contains(r, ld("2020-01-01")), "无生效日：过去日均有效");
        assertTrue(ErpDateRanges.contains(r, ld("2026-12-31")));
        assertFalse(ErpDateRanges.contains(r, ld("2027-01-01")));
    }

    @Test
    public void containsBothNullPermanentAlwaysValid() {
        // 两侧均 null：视为永久有效
        Range r = new Range(4L, null, null);
        assertTrue(ErpDateRanges.contains(r, ld("1900-01-01")));
        assertTrue(ErpDateRanges.contains(r, ld("2099-12-31")));
    }

    @Test
    public void containsNullArgsReturnFalse() {
        assertFalse(ErpDateRanges.contains((IDateRange) null, ld("2026-01-01")));
        assertFalse(ErpDateRanges.contains(new Range(5L, ld("2026-01-01"), ld("2026-12-31")), (LocalDate) null));
    }

    @Test
    public void containsUtilDateAdapter() {
        // java.util.Date 重载（TIMESTAMP 变体适配）
        Range r = new Range(6L, ld("2026-01-01"), ld("2026-12-31"));
        java.util.Date mid = java.sql.Date.valueOf("2026-06-15");
        assertTrue(ErpDateRanges.contains(r, mid));
        java.util.Date out = java.sql.Date.valueOf("2027-01-01");
        assertFalse(ErpDateRanges.contains(r, out));
    }

    // ---------- ErpDateRanges.overlaps ----------

    @Test
    public void overlapsSameEndpointsTrue() {
        // 端点完全相同视为重叠（端点闭区间）
        Range a = new Range(1L, ld("2026-01-01"), ld("2026-06-30"));
        Range b = new Range(2L, ld("2026-01-01"), ld("2026-06-30"));
        assertTrue(ErpDateRanges.overlaps(a, b));
    }

    @Test
    public void overlapsAdjacentDayFalse() {
        // 相邻不重叠：a.to = 2026-06-30，b.from = 2026-07-01（端点闭区间，相邻日不交）
        Range a = new Range(1L, ld("2026-01-01"), ld("2026-06-30"));
        Range b = new Range(2L, ld("2026-07-01"), ld("2026-12-31"));
        assertFalse(ErpDateRanges.overlaps(a, b));
        // 反向同样不交
        assertFalse(ErpDateRanges.overlaps(b, a));
    }

    @Test
    public void overlapsFullyContained() {
        // 完全包含
        Range outer = new Range(1L, ld("2026-01-01"), ld("2026-12-31"));
        Range inner = new Range(2L, ld("2026-03-01"), ld("2026-06-30"));
        assertTrue(ErpDateRanges.overlaps(outer, inner));
        assertTrue(ErpDateRanges.overlaps(inner, outer));
    }

    @Test
    public void overlapsPartialCross() {
        // 部分交叉
        Range a = new Range(1L, ld("2026-01-01"), ld("2026-06-30"));
        Range b = new Range(2L, ld("2026-04-01"), ld("2026-12-31"));
        assertTrue(ErpDateRanges.overlaps(a, b));
    }

    @Test
    public void overlapsNullEndOpenInfinity() {
        // NULL 端点视为无穷：开放侧与任何区间必交
        Range openRight = new Range(1L, ld("2026-01-01"), null);
        Range future = new Range(2L, ld("2099-01-01"), ld("2099-12-31"));
        assertTrue(ErpDateRanges.overlaps(openRight, future));

        Range openLeft = new Range(3L, null, ld("2020-12-31"));
        Range ancient = new Range(4L, ld("1900-01-01"), ld("1900-12-31"));
        assertTrue(ErpDateRanges.overlaps(openLeft, ancient));
    }

    @Test
    public void overlapsNullArgsReturnFalse() {
        assertFalse(ErpDateRanges.overlaps(null, new Range(1L, null, null)));
        assertFalse(ErpDateRanges.overlaps(new Range(1L, null, null), null));
    }

    // ---------- ErpDateRanges.effectiveOn ----------

    @Test
    public void effectiveOnFiltersHits() {
        List<Range> ranges = Arrays.asList(
                new Range(1L, ld("2026-01-01"), ld("2026-06-30")),
                new Range(2L, ld("2026-07-01"), ld("2026-12-31")),
                new Range(3L, ld("2026-01-01"), null)  // 永久右侧开放
        );
        // 2026-03-15 命中 r1 + r3
        List<Range> hits = ErpDateRanges.effectiveOn(ranges, ld("2026-03-15"));
        assertEquals(2, hits.size());
        assertEquals(1L, hits.get(0).getId());
        assertEquals(3L, hits.get(1).getId());

        // 2026-08-15 命中 r2 + r3
        List<Range> hits2 = ErpDateRanges.effectiveOn(ranges, ld("2026-08-15"));
        assertEquals(2, hits2.size());
    }

    @Test
    public void effectiveOnEmptyAndNull() {
        assertEquals(0, ErpDateRanges.effectiveOn(null, ld("2026-01-01")).size());
        assertEquals(0, ErpDateRanges.effectiveOn(Collections.emptyList(), ld("2026-01-01")).size());
        assertEquals(0, ErpDateRanges.effectiveOn(
                Collections.singletonList(new Range(1L, ld("2026-01-01"), ld("2026-12-31"))), null).size());
    }

    // ---------- ErpDateRanges.longestOverlap ----------

    @Test
    public void longestOverlapEmptyAndNull() {
        assertEquals(0, ErpDateRanges.longestOverlap(null));
        assertEquals(0, ErpDateRanges.longestOverlap(Collections.emptyList()));
    }

    @Test
    public void longestOverlapSingleRange() {
        assertEquals(1, ErpDateRanges.longestOverlap(Collections.singletonList(
                new Range(1L, ld("2026-01-01"), ld("2026-12-31")))));
    }

    @Test
    public void longestOverlapNoOverlap() {
        // 三段不重叠：[01-01..04-30] [05-01..08-31] [09-01..12-31]
        List<Range> ranges = Arrays.asList(
                new Range(1L, ld("2026-01-01"), ld("2026-04-30")),
                new Range(2L, ld("2026-05-01"), ld("2026-08-31")),
                new Range(3L, ld("2026-09-01"), ld("2026-12-31")));
        assertEquals(1, ErpDateRanges.longestOverlap(ranges));
    }

    @Test
    public void longestOverlapTripleOverlap() {
        // 三段互相重叠（r1, r2, r3 在 06-15 同时有效）
        List<Range> ranges = Arrays.asList(
                new Range(1L, ld("2026-01-01"), ld("2026-12-31")),  // 全年
                new Range(2L, ld("2026-06-01"), ld("2026-06-30")),  // 6 月
                new Range(3L, ld("2026-06-15"), ld("2026-06-15"))); // 单日
        assertEquals(3, ErpDateRanges.longestOverlap(ranges));
    }

    @Test
    public void longestOverlapAdjacentDayNoOverlap() {
        // 相邻日不重叠（端点闭区间）
        List<Range> ranges = Arrays.asList(
                new Range(1L, ld("2026-01-01"), ld("2026-06-30")),
                new Range(2L, ld("2026-07-01"), ld("2026-12-31")));
        assertEquals(1, ErpDateRanges.longestOverlap(ranges));
    }

    @Test
    public void longestOverlapSameDayMultiple() {
        // 两条同日：[01-01..06-30] 与 [06-30..12-31] 在 06-30 重叠
        List<Range> ranges = Arrays.asList(
                new Range(1L, ld("2026-01-01"), ld("2026-06-30")),
                new Range(2L, ld("2026-06-30"), ld("2026-12-31")));
        assertEquals(2, ErpDateRanges.longestOverlap(ranges));
    }

    // ---------- ErpDateRangeOverlapValidator.enforceMutex ----------

    @Test
    public void enforceMutexRejectsOverlap() {
        // 候选与既有记录重叠 → 抛异常
        Range candidate = new Range(10L, ld("2026-03-01"), ld("2026-09-30"));
        List<Range> existing = new ArrayList<>();
        existing.add(new Range(1L, ld("2026-01-01"), ld("2026-06-30")));  // 重叠
        NopException ex = assertThrows(NopException.class,
                () -> ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, null));
        assertEquals(ERR_TEST_OVERLAP.getErrorCode(), ex.getErrorCode());
        assertEquals("Range", ex.getParam(ErpDateRangeOverlapValidator.ARG_ENTITY_NAME));
        assertEquals(1L, ex.getParam(ErpDateRangeOverlapValidator.ARG_CONFLICT_ID));
    }

    @Test
    public void enforceMutexPassesNoOverlap() {
        // 候选与既有不重叠 → 通过无异常
        Range candidate = new Range(10L, ld("2026-07-01"), ld("2026-12-31"));
        List<Range> existing = Collections.singletonList(
                new Range(1L, ld("2026-01-01"), ld("2026-06-30")));
        ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, null);
        // 无异常即通过
    }

    @Test
    public void enforceMutexExcludesSelfId() {
        // 更新场景：候选 id 与既有 id 相同 → 排除自身，不报重叠
        Range candidate = new Range(5L, ld("2026-01-01"), ld("2026-12-31"));
        List<Range> existing = new ArrayList<>();
        existing.add(new Range(5L, ld("2026-01-01"), ld("2026-12-31")));  // 与自身相同 id
        existing.add(new Range(6L, ld("2027-01-01"), ld("2027-12-31"))); // 与候选不重叠
        ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, 5L);
        // 无异常即通过
    }

    @Test
    public void enforceMutexSelfIdStringMatches() {
        // selfId 为 String 类型也能匹配 Long id（toString 一致性）
        Range candidate = new Range(7L, ld("2026-01-01"), ld("2026-12-31"));
        List<Range> existing = Collections.singletonList(
                new Range(7L, ld("2026-01-01"), ld("2026-12-31")));
        // 用 Long selfId 匹配 Long id（实际项目里 selfId 来自 entity.getId() = Long）
        ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, 7L);
    }

    @Test
    public void enforceMutexSkipCandidateBothNull() {
        // 候选 validFrom=validTo=null（永久无区间）→ 跳过校验
        Range candidate = new Range(10L, null, null);
        List<Range> existing = Collections.singletonList(
                new Range(1L, ld("2026-01-01"), ld("2026-12-31")));
        ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, null);
        // 无异常即跳过
    }

    @Test
    public void enforceMutexSkipOtherBothNull() {
        // 既有记录永久无区间 → 不参与（避免误报）
        Range candidate = new Range(10L, ld("2026-01-01"), ld("2026-12-31"));
        List<Range> existing = Collections.singletonList(
                new Range(1L, null, null));
        ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, null);
        // 无异常即跳过既有永久无区间记录
    }

    @Test
    public void enforceMutexEmptyExistingPasses() {
        Range candidate = new Range(10L, ld("2026-01-01"), ld("2026-12-31"));
        ErpDateRangeOverlapValidator.enforceMutex(candidate, Collections.emptyList(), ERR_TEST_OVERLAP, null);
        ErpDateRangeOverlapValidator.enforceMutex(candidate, null, ERR_TEST_OVERLAP, null);
        // 无异常即通过
    }

    @Test
    public void enforceMutexNullCandidatePasses() {
        ErpDateRangeOverlapValidator.enforceMutex(null,
                Collections.singletonList(new Range(1L, null, null)),
                ERR_TEST_OVERLAP, null);
    }

    @Test
    public void enforceMutexHalfOpenCandidateOverlaps() {
        // 候选 validTo=null（右侧开放），与既有 [2026-01-01..2026-12-31] 重叠
        Range candidate = new Range(10L, ld("2026-06-01"), null);
        List<Range> existing = Collections.singletonList(
                new Range(1L, ld("2026-01-01"), ld("2026-12-31")));
        assertThrows(NopException.class,
                () -> ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_TEST_OVERLAP, null));
    }

    // ---------- ErpDateRanges.pickHighestPriority (PRIORITY 策略) ----------

    @Test
    public void pickHighestPriorityEmptyListReturnsNull() {
        assertNull(ErpDateRanges.pickHighestPriority(null, Comparator.comparingInt(PriorityRange::getPriority)));
        assertNull(ErpDateRanges.pickHighestPriority(
                Collections.emptyList(), Comparator.comparingInt(PriorityRange::getPriority)));
    }

    @Test
    public void pickHighestPrioritySingleElementReturnsIt() {
        PriorityRange only = new PriorityRange(1L, ld("2026-01-01"), ld("2026-12-31"), 100);
        PriorityRange result = ErpDateRanges.pickHighestPriority(
                Collections.singletonList(only), Comparator.comparingInt(PriorityRange::getPriority));
        assertEquals(only, result);
    }

    @Test
    public void pickHighestPriorityMultipleReturnsFirstByComparator() {
        // 3 条同日生效，priority 数值越小优先级越高（sales 约定）
        PriorityRange r1 = new PriorityRange(1L, ld("2026-01-01"), ld("2026-12-31"), 100);  // 标准价
        PriorityRange r2 = new PriorityRange(2L, ld("2026-01-01"), ld("2026-12-31"), 50);   // 客户组价
        PriorityRange r3 = new PriorityRange(3L, ld("2026-01-01"), ld("2026-12-31"), 10);   // VIP 价
        List<PriorityRange> effective = Arrays.asList(r1, r2, r3);
        PriorityRange top = ErpDateRanges.pickHighestPriority(
                effective, Comparator.comparingInt(PriorityRange::getPriority));
        assertEquals(3L, top.getId(), "priority ASC 取首 → r3（VIP 价）");
    }

    @Test
    public void pickHighestPrioritySamePriorityStableByStreamOrder() {
        // 相同优先级时 stream().min 稳定取第一个最小元素（按入参顺序）
        PriorityRange r1 = new PriorityRange(1L, ld("2026-01-01"), ld("2026-12-31"), 100);
        PriorityRange r2 = new PriorityRange(2L, ld("2026-01-01"), ld("2026-12-31"), 100);
        PriorityRange top = ErpDateRanges.pickHighestPriority(
                Arrays.asList(r1, r2), Comparator.comparingInt(PriorityRange::getPriority));
        assertEquals(1L, top.getId(), "相同优先级稳定性：取 stream 第一个");
    }

    @Test
    public void pickHighestPriorityWorksWithEffectiveOnPipeline() {
        // PRIORITY 策略典型用法：effectiveOn 结果接 pickHighestPriority
        List<PriorityRange> ranges = Arrays.asList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100),
                new PriorityRange(2L, ld("2026-04-01"), ld("2026-12-31"), 50),
                new PriorityRange(3L, ld("2026-07-01"), ld("2026-12-31"), 10));
        List<PriorityRange> effective = ErpDateRanges.effectiveOn(ranges, ld("2026-05-15"));
        assertEquals(2, effective.size(), "5 月 15 日命中 r1 + r2");
        PriorityRange top = ErpDateRanges.pickHighestPriority(
                effective, Comparator.comparingInt(PriorityRange::getPriority));
        assertEquals(2L, top.getId(), "r2 priority=50 < r1 priority=100 → 取 r2");
    }

    // ---------- ErpDateRangeOverlapValidator.enforceStackableAware (STACKABLE 策略) ----------

    @Test
    public void enforceStackableAwareBothNonStackableOverlapRejects() {
        // 双方 stackable=false 且区间重叠 → 抛异常（MUTEX 语义）
        PriorityRange candidate = new PriorityRange(10L, ld("2026-03-01"), ld("2026-09-30"), 100, false);
        List<PriorityRange> existing = new ArrayList<>();
        existing.add(new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100, false));  // 重叠
        NopException ex = assertThrows(NopException.class,
                () -> ErpDateRangeOverlapValidator.enforceStackableAware(
                        candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER));
        assertEquals(ERR_TEST_STACKABLE_CONFLICT.getErrorCode(), ex.getErrorCode());
        assertEquals(1L, ex.getParam(ErpDateRangeOverlapValidator.ARG_CONFLICT_ID));
    }

    @Test
    public void enforceStackableAwareCandidateStackableAllowsOverlap() {
        // candidate stackable=true 与非 stackable 既有重叠 → 允许（可叠加）
        PriorityRange candidate = new PriorityRange(10L, ld("2026-03-01"), ld("2026-09-30"), 100, true);
        List<PriorityRange> existing = Collections.singletonList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100, false));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
        // 无异常即允许
    }

    @Test
    public void enforceStackableAwareOtherStackableAllowsOverlap() {
        // candidate 非 stackable 与 stackable 既有重叠 → 允许（可被叠加）
        PriorityRange candidate = new PriorityRange(10L, ld("2026-03-01"), ld("2026-09-30"), 100, false);
        List<PriorityRange> existing = Collections.singletonList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100, true));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
        // 无异常即允许
    }

    @Test
    public void enforceStackableAwareBothStackableAllowsOverlap() {
        // 双方 stackable=true 且重叠 → 允许（并行叠加）
        PriorityRange candidate = new PriorityRange(10L, ld("2026-03-01"), ld("2026-09-30"), 100, true);
        List<PriorityRange> existing = Collections.singletonList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100, true));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
        // 无异常即允许
    }

    @Test
    public void enforceStackableAwareExcludesSelfId() {
        // 更新场景：候选 id 与既有 id 相同 → 排除自身
        PriorityRange candidate = new PriorityRange(5L, ld("2026-01-01"), ld("2026-12-31"), 100, false);
        List<PriorityRange> existing = new ArrayList<>();
        existing.add(new PriorityRange(5L, ld("2026-01-01"), ld("2026-12-31"), 100, false));
        existing.add(new PriorityRange(6L, ld("2027-01-01"), ld("2027-12-31"), 100, false));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, 5L, STACKABLE_GETTER);
        // 无异常即通过
    }

    @Test
    public void enforceStackableAwareSkipPermanentRange() {
        // 候选永久无区间（from=to=null）→ 跳过
        PriorityRange candidate = new PriorityRange(10L, null, null, 100, false);
        List<PriorityRange> existing = Collections.singletonList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-12-31"), 100, false));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);

        // 反向：既有永久无区间也跳过
        PriorityRange candidate2 = new PriorityRange(11L, ld("2026-01-01"), ld("2026-12-31"), 100, false);
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate2, Collections.singletonList(new PriorityRange(1L, null, null, 100, false)),
                ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
    }

    @Test
    public void enforceStackableAwareEmptyAndNullExistingPasses() {
        PriorityRange candidate = new PriorityRange(10L, ld("2026-01-01"), ld("2026-12-31"), 100, false);
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, Collections.emptyList(), ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, null, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
        ErpDateRangeOverlapValidator.enforceStackableAware(
                null, Collections.singletonList(new PriorityRange(1L, null, null, 100, false)),
                ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
    }

    @Test
    public void enforceStackableAwareAdjacentNoOverlapPasses() {
        // 双方 stackable=false 但相邻日不重叠 → 通过
        PriorityRange candidate = new PriorityRange(10L, ld("2026-07-01"), ld("2026-12-31"), 100, false);
        List<PriorityRange> existing = Collections.singletonList(
                new PriorityRange(1L, ld("2026-01-01"), ld("2026-06-30"), 100, false));
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate, existing, ERR_TEST_STACKABLE_CONFLICT, null, STACKABLE_GETTER);
    }

    // ---------- helpers ----------

    private static LocalDate ld(String s) {
        return LocalDate.parse(s);
    }
}
