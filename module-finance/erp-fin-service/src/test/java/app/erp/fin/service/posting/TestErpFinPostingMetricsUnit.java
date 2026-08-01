package app.erp.fin.service.posting;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.nop.commons.metrics.GlobalMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ErpFinPostingMetrics} SPI 迁移单测（observability.md §7.1 步骤 4 + §7.2 criterion 5）。
 *
 * <p>断言：(a) Counter/Timer meter 经 {@link MeterRegistry} 注册成功（业务事件后可查询）；
 * (b) 成功/失败 Counter 分别按 {@code result} tag 分桶计数；
 * (c) Timer 在多次 {@code recordLatency} 后 {@code sampleCount()} 反映累积样本数；
 * (d) {@code p99LatencyMillis()} 返回非零毫秒值（直方图估计，{@code publishPercentiles(0.99)} 启用）。
 *
 * <p>不依赖 IoC 容器——直接 {@code new ErpFinPostingMetrics(SimpleMeterRegistry)} 验证 SPI 接入语义。
 */
public class TestErpFinPostingMetricsUnit {

    @Test
    public void countersRegisteredAndIncrementedByResultTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinPostingMetrics metrics = new ErpFinPostingMetrics(registry);

        metrics.recordResult("AP_INVOICE", true);
        metrics.recordResult("AP_INVOICE", true);
        metrics.recordResult("AP_INVOICE", false);
        metrics.recordResult("SAL_SHP", true);

        Counter successAp = registry.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_RESULT, ErpFinPostingMetrics.RESULT_SUCCESS)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, "AP_INVOICE")
                .counter();
        assertNotNull(successAp, "success AP_INVOICE counter must be registered");
        assertEquals(2.0, successAp.count(), "AP_INVOICE success counter = 2");

        Counter failureAp = registry.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_RESULT, ErpFinPostingMetrics.RESULT_FAILURE)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, "AP_INVOICE")
                .counter();
        assertNotNull(failureAp, "failure AP_INVOICE counter must be registered");
        assertEquals(1.0, failureAp.count(), "AP_INVOICE failure counter = 1");

        Counter successSal = registry.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_RESULT, ErpFinPostingMetrics.RESULT_SUCCESS)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, "SAL_SHP")
                .counter();
        assertNotNull(successSal, "success SAL_SHP counter must be registered");
        assertEquals(1.0, successSal.count(), "SAL_SHP success counter = 1");
    }

    @Test
    public void timerRecordsLatencyAndExposesAggregateCountAndP99() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinPostingMetrics metrics = new ErpFinPostingMetrics(registry);

        // 3 笔 AP_INVOICE 过账（5ms / 8ms / 12ms）+ 1 笔 SAL_SHP 过账（20ms）
        metrics.recordLatency("AP_INVOICE", TimeUnit.MILLISECONDS.toNanos(5));
        metrics.recordLatency("AP_INVOICE", TimeUnit.MILLISECONDS.toNanos(8));
        metrics.recordLatency("AP_INVOICE", TimeUnit.MILLISECONDS.toNanos(12));
        metrics.recordLatency("SAL_SHP", TimeUnit.MILLISECONDS.toNanos(20));

        // 业务事件后 timer 注册成功（按 biz_type）
        Timer apTimer = registry.find(ErpFinPostingMetrics.METRIC_POSTING_DURATION)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, "AP_INVOICE")
                .timer();
        assertNotNull(apTimer, "AP_INVOICE duration timer must be registered");
        assertEquals(3L, apTimer.count(), "AP_INVOICE timer count = 3");

        Timer salTimer = registry.find(ErpFinPostingMetrics.METRIC_POSTING_DURATION)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, "SAL_SHP")
                .timer();
        assertNotNull(salTimer, "SAL_SHP duration timer must be registered");
        assertEquals(1L, salTimer.count(), "SAL_SHP timer count = 1");

        // 聚合 sampleCount（跨 biz_type 求和）= 3 + 1 = 4
        assertEquals(4, metrics.sampleCount(), "aggregate sample count = 4");

        // 聚合 p99LatencyMillis（跨 biz_type 取最大 P99）≥ AP_INVOICE 的 P99（≈12ms 估计）
        // 与 ring-buffer 迁移前数值语义偏移已评估（observability.md §4.2 P99 偏移注记）：
        // 直方图估计值非精确窗口 P99，但用于 30s 阈值门控仍能识别慢过账。
        long p99 = metrics.p99LatencyMillis();
        assertTrue(p99 > 0, "aggregate P99 must be non-zero after samples recorded");
        assertTrue(p99 < 30_000, "aggregate P99 < 30s threshold (test samples are sub-second)");
    }

    @Test
    public void emptyRegistryReturnsZeroSnapshot() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinPostingMetrics metrics = new ErpFinPostingMetrics(registry);

        assertEquals(0, metrics.sampleCount(), "no samples → count = 0");
        assertEquals(0L, metrics.p99LatencyMillis(), "no samples → p99 = 0");
    }

    @Test
    public void nullBizTypeNormalizedToUnknownTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinPostingMetrics metrics = new ErpFinPostingMetrics(registry);

        metrics.recordResult(null, true);
        metrics.recordLatency(null, 1_000_000L);

        Counter unknownCounter = registry.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, ErpFinPostingMetrics.BIZ_TYPE_UNKNOWN)
                .counter();
        assertNotNull(unknownCounter, "null biz_type normalized to 'unknown' tag");
        assertEquals(1.0, unknownCounter.count());

        Timer unknownTimer = registry.find(ErpFinPostingMetrics.METRIC_POSTING_DURATION)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, ErpFinPostingMetrics.BIZ_TYPE_UNKNOWN)
                .timer();
        assertNotNull(unknownTimer, "null biz_type normalized to 'unknown' tag (Timer)");
        assertEquals(1L, unknownTimer.count());
    }

    /**
     * 生产路径接入验证（observability.md §7.1 步骤 4 + §7.2 criterion 5）：
     * 无参构造器 {@code new ErpFinPostingMetrics()} → {@link GlobalMeterRegistry#instance()}（经
     * {@code QuarkusIntegration.start():48-51} 桥接流入 {@code /q/metrics}）。断言 meter 经全局 registry
     * 注册可查（{@code find().meter()} 非空）+ 业务事件后计数/计时正确。
     *
     * <p>确定性：surefire 单 fork 顺序执行；使用本测试独有的 {@code biz_type=GLOBAL_Q7_POSTING} tag 组合，
     * 与其他测试零碰撞 → delta 精确（before=0，after=N）。
     */
    @Test
    public void noArgConstructorBindsToGlobalMeterRegistry() {
        MeterRegistry global = GlobalMeterRegistry.instance();
        final String bizType = "GLOBAL_Q7_POSTING";

        ErpFinPostingMetrics metrics = new ErpFinPostingMetrics();

        metrics.recordResult(bizType, true);
        metrics.recordResult(bizType, true);
        metrics.recordResult(bizType, false);
        metrics.recordLatency(bizType, TimeUnit.MILLISECONDS.toNanos(5));

        assertNotNull(global.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                        .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, bizType).meter(),
                "erp_fin_posting_total meter registered in GlobalMeterRegistry via no-arg constructor");
        assertNotNull(global.find(ErpFinPostingMetrics.METRIC_POSTING_DURATION)
                        .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, bizType).meter(),
                "erp_fin_posting_duration_seconds meter registered in GlobalMeterRegistry");

        Counter success = global.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_RESULT, ErpFinPostingMetrics.RESULT_SUCCESS)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, bizType).counter();
        Counter failure = global.find(ErpFinPostingMetrics.METRIC_POSTING_TOTAL)
                .tag(ErpFinPostingMetrics.TAG_RESULT, ErpFinPostingMetrics.RESULT_FAILURE)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, bizType).counter();
        Timer duration = global.find(ErpFinPostingMetrics.METRIC_POSTING_DURATION)
                .tag(ErpFinPostingMetrics.TAG_BIZ_TYPE, bizType).timer();

        assertNotNull(success, "success counter in global registry");
        assertNotNull(failure, "failure counter in global registry");
        assertNotNull(duration, "duration timer in global registry");
        assertEquals(2.0, success.count(), "success count = 2 (unique biz_type isolates delta)");
        assertEquals(1.0, failure.count(), "failure count = 1");
        assertEquals(1L, duration.count(), "duration sample count = 1");
        assertTrue(duration.totalTime(TimeUnit.MILLISECONDS) >= 5.0,
                "recorded duration >= 5ms");
    }
}
