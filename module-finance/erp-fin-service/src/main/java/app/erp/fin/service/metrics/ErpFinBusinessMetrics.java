package app.erp.fin.service.metrics;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.nop.commons.metrics.GlobalMeterRegistry;

import java.util.concurrent.TimeUnit;

/**
 * finance 域业务指标静态埋点工具（observability.md §5.1 指标 3 / 6）。
 *
 * <p>本类是静态工具类（非 IoC bean）——避免编辑 {@code _vfs/.../app-service.beans.xml}（受
 * `_vfs` 目录写入约束）。所有方法经 {@link GlobalMeterRegistry#instance()} 取 registry（对齐
 * observability.md §4.2 裁决允许的「无参默认 GlobalMeterRegistry.instance()」范式），经
 * {@code QuarkusIntegration.start():48-51} 桥接后自动流入 {@code /q/metrics} 端点。
 *
 * <p>覆盖指标：
 * <ul>
 *   <li>指标 3 {@code erp_fin_period_close_duration_seconds}（Timer，tag=fiscal_year,period_no）——
 *       由 {@code ErpFinAccountingPeriodClosePeriodProcessor.closePeriod} 调用
 *       {@link #recordPeriodCloseDuration}。</li>
 *   <li>指标 6 {@code erp_business_path_throughput_total}（Counter，tag=path）—— finance 三路径：
 *       posting（{@code ErpFinVoucherBizModel.post}）/ period_close
 *       （{@code ErpFinAccountingPeriodClosePeriodProcessor.closePeriod}）/
 *       report_render（{@code ErpFinReportBizModel.renderHtml|download}）。</li>
 * </ul>
 *
 * <p>指标 5（{@code erp_fin_posting_exception_backlog} Gauge + 5 分钟后台刷新）需生命周期管理，
 * 由 {@code ErpFinPostingExceptionBizModel} 的 {@code @PostConstruct} 注册（复用 {@code countUnresolved}
 * 既有语义，observability.md §5.1 指标 5 校正），不在本静态类内。
 */
public final class ErpFinBusinessMetrics {

    public static final String METRIC_PERIOD_CLOSE_DURATION = "erp_fin_period_close_duration_seconds";
    public static final String METRIC_BUSINESS_PATH_THROUGHPUT = "erp_business_path_throughput_total";

    public static final String TAG_FISCAL_YEAR = "fiscal_year";
    public static final String TAG_PERIOD_NO = "period_no";
    public static final String TAG_PATH = "path";

    public static final String PATH_POSTING = "posting";
    public static final String PATH_PERIOD_CLOSE = "period_close";
    public static final String PATH_REPORT_RENDER = "report_render";

    private ErpFinBusinessMetrics() {
    }

    /**
     * 指标 3：单期间结账端到端耗时（{@code erp_fin_period_close_duration_seconds} Timer）。
     * tag：{@code fiscal_year}, {@code period_no}（来源 {@link ErpFinAccountingPeriod#getYear()} / {@code getMonth()}）。
     *
     * @param registry       目标 registry（IoC 注入或测试用 SimpleMeterRegistry）；null 走 GlobalMeterRegistry
     * @param period         期间实体（提供 tag 值）；null 忽略
     * @param durationNanos  耗时（纳秒）；负值忽略
     */
    public static void recordPeriodCloseDuration(MeterRegistry registry, ErpFinAccountingPeriod period,
                                                  long durationNanos) {
        if (period == null || durationNanos < 0) {
            return;
        }
        Timer.builder(METRIC_PERIOD_CLOSE_DURATION)
                .tag(TAG_FISCAL_YEAR, period.getYear() == null ? "unknown" : String.valueOf(period.getYear()))
                .tag(TAG_PERIOD_NO, period.getMonth() == null ? "unknown" : String.valueOf(period.getMonth()))
                .register(resolveRegistry(registry))
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** 指标 6 path=posting：凭证过账关键路径吞吐。 */
    public static void recordPostingPathThroughput(MeterRegistry registry) {
        businessPathCounter(registry, PATH_POSTING).increment();
    }

    /** 指标 6 path=period_close：期间结账关键路径吞吐。 */
    public static void recordPeriodClosePathThroughput(MeterRegistry registry) {
        businessPathCounter(registry, PATH_PERIOD_CLOSE).increment();
    }

    /** 指标 6 path=report_render：报表渲染关键路径吞吐（app 层入口，不触及 nop-entropy 源码）。 */
    public static void recordReportRenderPathThroughput(MeterRegistry registry) {
        businessPathCounter(registry, PATH_REPORT_RENDER).increment();
    }

    private static Counter businessPathCounter(MeterRegistry registry, String path) {
        return Counter.builder(METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(TAG_PATH, path)
                .register(resolveRegistry(registry));
    }

    private static MeterRegistry resolveRegistry(MeterRegistry registry) {
        return registry != null ? registry : GlobalMeterRegistry.instance();
    }
}
