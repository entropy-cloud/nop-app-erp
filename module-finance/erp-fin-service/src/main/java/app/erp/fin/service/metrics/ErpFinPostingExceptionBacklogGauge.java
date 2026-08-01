package app.erp.fin.service.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.nop.commons.metrics.GlobalMeterRegistry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标 5（{@code erp_fin_posting_exception_backlog} Gauge）注册助手（observability.md §5.1 指标 5）。
 *
 * <p>静态工具类（非 IoC bean）——避免编辑 {@code _vfs/.../app-service.beans.xml}（受 `_vfs` 目录写入约束）。
 * Gauge 经 {@link GlobalMeterRegistry#instance()} 注册（生产）或测试传入的 {@link MeterRegistry}（单测），
 * 由 {@code ErpFinPostingExceptionBizModel.initObservability()} 触发。
 *
 * <p>Gauge 模型：值由 {@link AtomicLong} 持有（5 分钟后台刷新任务更新），Prometheus scrape 时 Gauge
 * 读取此缓存值。后台刷新避免 scrape 时才查 DB 引入抓取延迟（observability.md §5.1 指标 5 校正）。
 */
public final class ErpFinPostingExceptionBacklogGauge {

    public static final String METRIC_POSTING_EXCEPTION_BACKLOG = "erp_fin_posting_exception_backlog";
    public static final String TAG_BIZ_TYPE = "biz_type";
    public static final String BIZ_TYPE_ALL = "all";

    private ErpFinPostingExceptionBacklogGauge() {
    }

    /**
     * 注册 Gauge 到指定 registry，绑定到给定的 {@link AtomicLong} 缓存值。
     * 调用方负责维护缓存值的刷新（如经 {@code GlobalExecutors.globalTimer().scheduleAtFixedRate}）。
     *
     * @param registry    目标 registry；null 走 {@link GlobalMeterRegistry#instance()}
     * @param backlogCache Gauge 读取的缓存值持有者
     */
    public static void register(MeterRegistry registry, AtomicLong backlogCache) {
        Gauge.builder(METRIC_POSTING_EXCEPTION_BACKLOG, backlogCache, AtomicLong::doubleValue)
                .tag(TAG_BIZ_TYPE, BIZ_TYPE_ALL)
                .register(registry != null ? registry : GlobalMeterRegistry.instance());
    }
}
