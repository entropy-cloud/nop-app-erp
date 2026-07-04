package app.erp.fin.service.posting;

import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;

import java.util.Arrays;

/**
 * 业财过账运行监控——进程内时延采样器（{@code posting-log.md §裁决3}）。
 *
 * <p>凭证生成时延 P99 不可由 SQL 衍生（事件触发时间未入库），故用环形缓冲收集最近 N 次过账的
 * 总耗时（复用 {@code ErpFinPostingProcessor} 各阶段 {@code nanoTimeDiff} 求和），P99 = 排序后第 99 百分位。
 *
 * <p>线程安全：所有写路径经 {@code synchronized}（过账为业务关键路径但 QPS 有限，锁竞争可接受；
 * 若未来高吞吐可换为 lock-free 环形缓冲）。
 *
 * <p>残留风险（见 owner doc §裁决3）：进程重启采样清零、无历史趋势——生产趋势须接 Micrometer + 时序库（Follow-up）。
 */
public class ErpFinPostingMetrics {

    private volatile long[] samples;
    private volatile int size;
    private volatile int nextIndex;

    public ErpFinPostingMetrics() {
        this.samples = new long[ErpFinConstants.DEFAULT_METRIC_LATENCY_SAMPLE_WINDOW];
        this.size = 0;
        this.nextIndex = 0;
    }

    /** 采样窗口大小（覆盖默认值，由部署侧配置；仅在首次调用时生效以避免并发重建数组）。 */
    public void init() {
        int window = resolveSampleWindow();
        if (window > 0 && window != samples.length) {
            synchronized (this) {
                if (window != samples.length) {
                    samples = new long[window];
                    size = 0;
                    nextIndex = 0;
                }
            }
        }
    }

    /** 记录单次过账总耗时（纳秒）。由 {@code ErpFinPostingProcessor} 在编排方法成功路径调用。 */
    public synchronized void recordLatency(long durationNanos) {
        if (durationNanos < 0) {
            return;
        }
        samples[nextIndex] = durationNanos;
        nextIndex = (nextIndex + 1) % samples.length;
        if (size < samples.length) {
            size++;
        }
    }

    /** 当前窗口内的 P99 时延（毫秒）；无样本返回 0。 */
    public synchronized long p99LatencyMillis() {
        if (size == 0) {
            return 0L;
        }
        long[] copy = Arrays.copyOf(samples, size);
        Arrays.sort(copy);
        int idx = (int) Math.ceil(copy.length * 0.99) - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= copy.length) {
            idx = copy.length - 1;
        }
        return CoreMetrics.nanoToMillis(copy[idx]);
    }

    /** 当前窗口样本数（供呈现接口暴露观测基数）。 */
    public synchronized int sampleCount() {
        return size;
    }

    private static int resolveSampleWindow() {
        return AppConfig.var(ErpFinConstants.CONFIG_METRIC_LATENCY_SAMPLE_WINDOW,
                ErpFinConstants.DEFAULT_METRIC_LATENCY_SAMPLE_WINDOW);
    }
}
