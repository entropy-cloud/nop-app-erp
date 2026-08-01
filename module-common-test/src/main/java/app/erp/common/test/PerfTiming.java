package app.erp.common.test;

import io.nop.api.core.time.CoreMetrics;

import java.util.Arrays;

/**
 * 性能测量辅助工具（plan 2026-08-02-1121-2 Phase 1 / 设计文档 §5.1）。
 *
 * <p>统一性能测量协议（设计文档 §4 统一约定）：
 * <ol>
 *   <li>K 轮 <b>untimed warmup</b>（不计入测量；使 JIT 稳定 + DB/ORM 缓存预热，缓解系统性
 *       首 invocation bias，设计文档 §3.4）。</li>
 *   <li>N 轮 <b>timed 测量</b>（{@link CoreMetrics#nanoTime()} 包裹；Q6 {@link ThreadLocalFrozenClock}
 *       已使被测路径的日期/期间数据确定性成立，计时随机噪声经 N 轮 + 中位数 + 方差比收敛）。</li>
 * </ol>
 *
 * <p>度量定义（设计文档 §4 统一约定）：
 * <ul>
 *   <li>{@code varianceRatio = (max − min) / median}——极差/中位数比，直观稳健、无正态假设。</li>
 *   <li>验收阈值：凭证过账 / 报表渲染 &lt; 15%；期间结账 / reclose &lt; 20%（设计文档 §4.x）。</li>
 * </ul>
 *
 * <p>{@link Measurement} 对齐设计文档 §5.3 baseline JSON 字段（{@code timedRounds} + {@code median} +
 * {@code p95} + {@code varianceRatio}），便于 perf 测试类直接写入基线 JSON。
 *
 * <p><b>计时窗口纪律（硬约束）</b>：调用方须保证传入的 {@code timed} 仅包裹被测业务链路本身，
 * seed 数据生成 / 夹具构造必须在 {@code PerfTiming.measure(...)} 调用<b>之前</b>完成。否则 seed-gen
 * 成本会污染被测墙钟（设计文档 §4 统一约定 + §5.2 重申）。每轮若需消费唯一输入（如幂等过账须每轮
 * 唯一 bill code），亦须在 timing 窗口外预先构造好 batch 数组，timing 窗口内仅做消费循环。
 */
public final class PerfTiming {

    private PerfTiming() {
    }

    public static Measurement measure(Runnable timed, int warmupK, int timedN) {
        for (int i = 0; i < warmupK; i++) {
            timed.run();
        }
        long[] nanos = new long[timedN];
        for (int i = 0; i < timedN; i++) {
            long start = CoreMetrics.nanoTime();
            timed.run();
            nanos[i] = CoreMetrics.nanoTimeDiff(start);
        }
        return compute(nanos);
    }

    public static Measurement compute(long[] nanosRaw) {
        long[] sorted = Arrays.copyOf(nanosRaw, nanosRaw.length);
        Arrays.sort(sorted);
        int n = sorted.length;
        long min = sorted[0];
        long max = sorted[n - 1];
        long median = (n % 2 == 1) ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2L;
        double rank = 0.95 * (n - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        double frac = rank - lo;
        long p95 = (lo == hi) ? sorted[lo] : (long) (sorted[lo] + frac * (sorted[hi] - sorted[lo]));
        double varianceRatio = median == 0.0 ? 0.0 : ((double) (max - min)) / ((double) median);
        return new Measurement(nanosRaw, median, p95, varianceRatio, min, max);
    }

    public static final class Measurement {
        public final long[] timedRounds;
        public final long median;
        public final long p95;
        public final double varianceRatio;
        public final long min;
        public final long max;

        public Measurement(long[] timedRounds, long median, long p95, double varianceRatio, long min, long max) {
            this.timedRounds = timedRounds;
            this.median = median;
            this.p95 = p95;
            this.varianceRatio = varianceRatio;
            this.min = min;
            this.max = max;
        }

        public double varianceRatioPercent() {
            return varianceRatio * 100.0;
        }

        public double medianMillis() {
            return median / 1_000_000.0;
        }

        public double p95Millis() {
            return p95 / 1_000_000.0;
        }

        public boolean withinThreshold(double varianceThresholdPercent) {
            return varianceRatioPercent() < varianceThresholdPercent;
        }
    }
}
