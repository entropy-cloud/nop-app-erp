package app.erp.fin.service.posting;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.nop.commons.metrics.GlobalMeterRegistry;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 业财过账运行监控——Micrometer SPI 接入（{@code posting-log.md §裁决3} + observability.md §4.2 SPI 迁移）。
 *
 * <p>本类对齐平台 {@code DaoMetricsImpl} 范式：构造器注入 {@link MeterRegistry}（IoC）或无参默认
 * {@link GlobalMeterRegistry#instance()}，经 {@link Counter} / {@link Timer} 原生 API 创建业务指标，
 * 经 {@code QuarkusIntegration.start():48-51} 桥接后自动流入 {@code /q/metrics} 端点。
 *
 * <p><b>契约保留</b>（observability.md §4.2 + §9.1 R3）：{@link #p99LatencyMillis()} /
 * {@link #sampleCount()} 签名保留并转发 {@link Timer#takeSnapshot()}，支撑公开 {@code @BizQuery} 跨层契约
 * {@code IErpFinPostingExceptionBiz.getRuntimeMetrics} → {@code ErpFinPostingMetricsSnapshot}
 * （含阈值门控 {@code erp-fin.metric.latency-p99-threshold-millis}）+ {@code TestErpFinPostingMetrics} 断言。
 *
 * <p><b>P99 语义偏移</b>（observability.md §4.2 注记 + §9.1 R3 + 本计划 Phase 2 Decision 裁决）：
 * 迁移前 {@code p99LatencyMillis()} 返回进程内 ring-buffer 窗口的<b>精确</b> P99（排序后 {@code ceil(n*0.99)-1} 位）；
 * 迁移后转发 {@link Timer.Snapshot#percentile(double)} 返回 Micrometer 直方图<b>估计值</b>（client-side 插值）。
 * 裁决：偏移可接受——阈值门控（默认 30s）用于告警而非精确度量，直方图估计值同样可识别慢过账；
 * 经 {@code Timer.Builder.publishPercentiles(0.99)} 显式启用 P99 客户端计算，避免默认未启用时
 * {@code percentile(0.99)=0} 的盲区。聚合策略：snapshot 跨所有 {@code biz_type} 变体取
 * 「最大 P99 + 总样本数」（worst-case 聚合，对齐原 ring-buffer 全局窗口观测意图）。
 *
 * <p>本类删除了原 {@code volatile long[]} ring-buffer（observability.md §7.2 criterion 3 验收）。
 * 进程重启语义：Micrometer registry 内置累积计数器，进程内有效；持久化趋势由部署侧 Prometheus successor 负责。
 */
public class ErpFinPostingMetrics {

    static final String METRIC_POSTING_TOTAL = "erp_fin_posting_total";
    static final String METRIC_POSTING_DURATION = "erp_fin_posting_duration_seconds";
    static final String TAG_RESULT = "result";
    static final String TAG_BIZ_TYPE = "biz_type";
    static final String RESULT_SUCCESS = "success";
    static final String RESULT_FAILURE = "failure";
    static final String BIZ_TYPE_UNKNOWN = "unknown";
    static final double PERCENTILE_P99 = 0.99;

    private final MeterRegistry registry;

    public ErpFinPostingMetrics() {
        this(GlobalMeterRegistry.instance());
    }

    public ErpFinPostingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 指标 1（{@code erp_fin_posting_total} Counter）—— 过账成功/失败分桶计数。
     * 由 {@code ErpFinPostingProcessor} 在编排方法 success/failure 路径调用。
     *
     * @param bizType 业务单据类型（如 {@code AP_INVOICE}）；null 归一化为 {@code unknown}
     * @param success true=成功（{@code result=success}），false=失败（{@code result=failure}）
     */
    public void recordResult(String bizType, boolean success) {
        Counter.builder(METRIC_POSTING_TOTAL)
                .tag(TAG_RESULT, success ? RESULT_SUCCESS : RESULT_FAILURE)
                .tag(TAG_BIZ_TYPE, normalizeBizType(bizType))
                .register(registry)
                .increment();
    }

    /**
     * 指标 2（{@code erp_fin_posting_duration_seconds} Timer）—— 单次过账端到端耗时。
     * 替代原 ring-buffer 的 {@code recordLatency(long)}（observability.md §4.2 裁决）。
     * Timer tag 仅 {@code biz_type}（不含 {@code result}，对齐设计文档 §5.1 指标 2 spec）。
     *
     * @param bizType       业务单据类型；null 归一化为 {@code unknown}
     * @param durationNanos 单次过账端到端耗时（纳秒）；负值忽略
     */
    public void recordLatency(String bizType, long durationNanos) {
        if (durationNanos < 0) {
            return;
        }
        postingDurationTimer(bizType).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 当前所有 {@code biz_type} 变体的聚合 P99 时延（毫秒）；无样本返回 0。
     *
     * <p>聚合策略：跨所有 {@code erp_fin_posting_duration_seconds{biz_type=*}} Timer 取最大 P99
     * （worst-case 观测，对齐原 ring-buffer 全局窗口意图）。P99 由 {@code publishPercentiles(0.99)}
     * 客户端计算（非 PromQL {@code histogram_quantile}）。
     */
    public long p99LatencyMillis() {
        double maxP99Millis = 0.0;
        for (Timer timer : findPostingDurationTimers()) {
            double p99 = extractP99Millis(timer.takeSnapshot());
            if (p99 > maxP99Millis) {
                maxP99Millis = p99;
            }
        }
        return (long) maxP99Millis;
    }

    /**
     * 当前所有 {@code biz_type} 变体的聚合样本数（供呈现接口暴露观测基数）。
     *
     * <p>聚合策略：跨所有 {@code erp_fin_posting_duration_seconds{biz_type=*}} Timer 求和 count。
     */
    public int sampleCount() {
        long total = 0;
        for (Timer timer : findPostingDurationTimers()) {
            total += timer.takeSnapshot().count();
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    private Timer postingDurationTimer(String bizType) {
        return Timer.builder(METRIC_POSTING_DURATION)
                .tag(TAG_BIZ_TYPE, normalizeBizType(bizType))
                .publishPercentiles(PERCENTILE_P99)
                .register(registry);
    }

    private Collection<Timer> findPostingDurationTimers() {
        return registry.find(METRIC_POSTING_DURATION).timers();
    }

    /**
     * 从 {@link HistogramSnapshot} 提取 P99 毫秒值（micrometer 1.16.5 API：{@code percentileValues()}
     * 返回 {@link ValueAtPercentile} 数组，每个元素含 {@code percentile()} 系数 + {@code value(TimeUnit)} 单位化值）。
     * 未启用 {@code publishPercentiles(0.99)} 或无样本时返回 0（{@code ValueAtPercentile} 数组为空）。
     *
     * <p>单位注记：{@code ValueAtPercentile.value()}（无参）返回 Timer 内部原始记录单位（纳秒），
     * 与 {@code Timer.getId().getBaseUnit()} 标签（seconds）不一致——须用 {@code value(TimeUnit)} 显式单位化。
     */
    private static double extractP99Millis(HistogramSnapshot snapshot) {
        ValueAtPercentile[] pcts = snapshot.percentileValues();
        if (pcts == null) {
            return 0.0;
        }
        double maxMatched = 0.0;
        for (ValueAtPercentile v : pcts) {
            if (v != null && v.percentile() >= PERCENTILE_P99) {
                double millis = v.value(TimeUnit.MILLISECONDS);
                if (millis > maxMatched) {
                    maxMatched = millis;
                }
            }
        }
        return maxMatched;
    }

    private static String normalizeBizType(String bizType) {
        return bizType == null ? BIZ_TYPE_UNKNOWN : bizType;
    }
}
