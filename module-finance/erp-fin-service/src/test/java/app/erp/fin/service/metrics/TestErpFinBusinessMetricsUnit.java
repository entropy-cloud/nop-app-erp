package app.erp.fin.service.metrics;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.nop.commons.metrics.GlobalMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * finance 域业务指标单测（observability.md §7.1 步骤 4 + §7.2 criterion 5）。
 *
 * <p>覆盖指标 3（{@code erp_fin_period_close_duration_seconds} Timer）+ 指标 5（Gauge 注册助手）+
 * 指标 6 finance 三路径（posting/period_close/report_render）。
 *
 * <p>不依赖 IoC 容器——直接传 {@link SimpleMeterRegistry} 验证 SPI 接入语义。
 */
public class TestErpFinBusinessMetricsUnit {

    @Test
    public void periodCloseDurationTimerRegisteredWithTagValuesFromPeriod() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setYear(2026);
        period.setMonth(7);

        ErpFinBusinessMetrics.recordPeriodCloseDuration(registry, period,
                TimeUnit.MILLISECONDS.toNanos(1500));

        Timer timer = registry.find(ErpFinBusinessMetrics.METRIC_PERIOD_CLOSE_DURATION)
                .tag(ErpFinBusinessMetrics.TAG_FISCAL_YEAR, "2026")
                .tag(ErpFinBusinessMetrics.TAG_PERIOD_NO, "7")
                .timer();
        assertNotNull(timer, "period_close_duration timer must be registered with year/month tags");
        assertEquals(1L, timer.count(), "single recording → count = 1");
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 1000.0,
                "recorded duration >= 1000ms (recorded 1500ms)");
    }

    @Test
    public void periodCloseDurationIgnoresNullOrNegativeInputs() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ErpFinBusinessMetrics.recordPeriodCloseDuration(registry, null, 1_000_000L);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setYear(2026);
        ErpFinBusinessMetrics.recordPeriodCloseDuration(registry, period, -1L);
        assertEquals(0, registry.find(ErpFinBusinessMetrics.METRIC_PERIOD_CLOSE_DURATION).timers().size(),
                "null period + negative duration ignored → no timer registered");
    }

    @Test
    public void businessPathThroughputCounterRegisteredPerPath() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        ErpFinBusinessMetrics.recordPostingPathThroughput(registry);
        ErpFinBusinessMetrics.recordPostingPathThroughput(registry);
        ErpFinBusinessMetrics.recordPeriodClosePathThroughput(registry);
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(registry);
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(registry);
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(registry);

        assertEquals(2.0, pathCounter(registry, ErpFinBusinessMetrics.PATH_POSTING).count(),
                "posting path counter = 2");
        assertEquals(1.0, pathCounter(registry, ErpFinBusinessMetrics.PATH_PERIOD_CLOSE).count(),
                "period_close path counter = 1");
        assertEquals(3.0, pathCounter(registry, ErpFinBusinessMetrics.PATH_REPORT_RENDER).count(),
                "report_render path counter = 3");
    }

    @Test
    public void postingExceptionBacklogGaugeReflectsAtomicLongValue() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        java.util.concurrent.atomic.AtomicLong backlog = new java.util.concurrent.atomic.AtomicLong(42L);

        ErpFinPostingExceptionBacklogGauge.register(registry, backlog);

        assertEquals(42.0, gaugeValue(registry), 0.001,
                "Gauge reflects initial AtomicLong value 42");

        backlog.set(99L);
        assertEquals(99.0, gaugeValue(registry), 0.001,
                "Gauge reflects updated AtomicLong value 99");
    }

    private static Counter pathCounter(MeterRegistry registry, String path) {
        Counter c = registry.find(ErpFinBusinessMetrics.METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(ErpFinBusinessMetrics.TAG_PATH, path)
                .counter();
        assertNotNull(c, "path=" + path + " counter must be registered");
        return c;
    }

    private static double gaugeValue(MeterRegistry registry) {
        return registry.find(ErpFinPostingExceptionBacklogGauge.METRIC_POSTING_EXCEPTION_BACKLOG)
                .tag(ErpFinPostingExceptionBacklogGauge.TAG_BIZ_TYPE,
                        ErpFinPostingExceptionBacklogGauge.BIZ_TYPE_ALL)
                .gauge()
                .value();
    }

    /**
     * 生产路径接入验证（observability.md §7.1 步骤 4 + §7.2 criterion 5）：
     * 静态助手 {@code registry=null} → {@link GlobalMeterRegistry#instance()}（经
     * {@code QuarkusIntegration.start():48-51} 桥接流入 {@code /q/metrics}）。覆盖指标 3（period_close_duration
     * Timer）+ 指标 5（backlog Gauge）+ 指标 6 finance 三路径（throughput Counter）。
     *
     * <p>确定性：surefire 单 fork 顺序执行。指标 3 使用独有 tag 组合 {@code fiscal_year=2099,period_no=13}
     * 零碰撞 → delta 精确；指标 6 固定枚举 path tag 使用 before/after delta capture（同 fork 内可能已有
     * 集成测试经 Processor 写入全局 throughput counter）；指标 5 Gauge {@code register()} 幂等返回既有或新注册。
     */
    @Test
    public void globalMeterRegistryPathRegistersFinanceBusinessMetrics() {
        MeterRegistry global = GlobalMeterRegistry.instance();

        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setYear(2099);
        period.setMonth(13);

        ErpFinBusinessMetrics.recordPeriodCloseDuration(null, period, TimeUnit.MILLISECONDS.toNanos(500));

        double beforePosting = pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_POSTING);
        double beforePeriodClose = pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_PERIOD_CLOSE);
        double beforeReportRender = pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_REPORT_RENDER);

        ErpFinBusinessMetrics.recordPostingPathThroughput(null);
        ErpFinBusinessMetrics.recordPeriodClosePathThroughput(null);
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(null);
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(null);

        AtomicLong backlog = new AtomicLong(7L);
        ErpFinPostingExceptionBacklogGauge.register(global, backlog);

        assertNotNull(global.find(ErpFinBusinessMetrics.METRIC_PERIOD_CLOSE_DURATION)
                        .tag(ErpFinBusinessMetrics.TAG_FISCAL_YEAR, "2099")
                        .tag(ErpFinBusinessMetrics.TAG_PERIOD_NO, "13").meter(),
                "erp_fin_period_close_duration_seconds registered in GlobalMeterRegistry");
        assertNotNull(global.find(ErpFinBusinessMetrics.METRIC_BUSINESS_PATH_THROUGHPUT).meter(),
                "erp_business_path_throughput_total registered in GlobalMeterRegistry");
        Gauge backlogGauge = global.find(ErpFinPostingExceptionBacklogGauge.METRIC_POSTING_EXCEPTION_BACKLOG).gauge();
        assertNotNull(backlogGauge, "erp_fin_posting_exception_backlog gauge registered in GlobalMeterRegistry");

        Timer periodTimer = global.find(ErpFinBusinessMetrics.METRIC_PERIOD_CLOSE_DURATION)
                .tag(ErpFinBusinessMetrics.TAG_FISCAL_YEAR, "2099")
                .tag(ErpFinBusinessMetrics.TAG_PERIOD_NO, "13").timer();
        assertNotNull(periodTimer, "period close timer (unique tag combo)");
        assertEquals(1L, periodTimer.count(), "period_close count = 1 (unique tag combo isolates delta)");
        assertTrue(periodTimer.totalTime(TimeUnit.MILLISECONDS) >= 500.0, "recorded >= 500ms");

        assertEquals(beforePosting + 1, pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_POSTING), 1e-9,
                "posting path delta = 1");
        assertEquals(beforePeriodClose + 1, pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_PERIOD_CLOSE), 1e-9,
                "period_close path delta = 1");
        assertEquals(beforeReportRender + 2, pathCounterInGlobal(global, ErpFinBusinessMetrics.PATH_REPORT_RENDER), 1e-9,
                "report_render path delta = 2");
    }

    private static double pathCounterInGlobal(MeterRegistry registry, String path) {
        Counter c = registry.find(ErpFinBusinessMetrics.METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(ErpFinBusinessMetrics.TAG_PATH, path)
                .counter();
        return c == null ? 0.0 : c.count();
    }
}
