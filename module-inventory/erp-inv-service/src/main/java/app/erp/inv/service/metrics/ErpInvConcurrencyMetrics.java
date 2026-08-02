package app.erp.inv.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nop.commons.metrics.GlobalMeterRegistry;

/**
 * inventory 域业务指标静态埋点工具（observability.md §5.1 指标 4 / 6）。
 *
 * <p>静态工具类（非 IoC bean）——避免编辑 {@code _vfs/.../app-service.beans.xml}（受 `_vfs` 目录写入约束）。
 * 所有方法经 {@link GlobalMeterRegistry#instance()} 取 registry（observability.md §4.2 裁决允许的
 * 「无参默认 GlobalMeterRegistry.instance()」范式），经 {@code QuarkusIntegration.start():48-51}
 * 桥接后流入 {@code /q/metrics} 端点。
 *
 * <p>覆盖指标：
 * <ul>
 *   <li>指标 4 {@code erp_concurrency_optimistic_lock_failure_total}（Counter，tag=domain,operation）——
 *       repo-wide grep 定位：app-erp 全仓仅 {@code StockMoveBookkeeper.updateBalanceWithRetry} 一处
 *       tryLock+retry 范式（其他 retry/tryLock 命中均为 b2b 实体 retryCount 字段或 finance 异常工作台
 *       deferred retry，非乐观锁并发冲突范式），由该处调用 {@link #recordOptimisticLockFailure}。</li>
 *   <li>指标 6 path=costing_reclose：{@code ErpInvCostingBizModel.reclosePeriodCosts} 入口
 *       调用 {@link #recordCostingReclosePathThroughput}（对齐 Q5 §4 四路径定义）。</li>
 * </ul>
 */
public final class ErpInvConcurrencyMetrics {

    public static final String METRIC_OPTIMISTIC_LOCK_FAILURE = "erp_concurrency_optimistic_lock_failure_total";
    public static final String METRIC_BUSINESS_PATH_THROUGHPUT = "erp_business_path_throughput_total";

    public static final String TAG_DOMAIN = "domain";
    public static final String TAG_OPERATION = "operation";
    public static final String TAG_PATH = "path";

    public static final String DOMAIN_INVENTORY = "inventory";
    public static final String OPERATION_STOCK_BALANCE_UPDATE = "stock_balance_update";
    public static final String OPERATION_STOCK_BALANCE_UPDATE_EXHAUSTED = "stock_balance_update_retry_exhausted";
    public static final String PATH_COSTING_RECLOSE = "costing_reclose";

    private ErpInvConcurrencyMetrics() {
    }

    /**
     * 指标 4：乐观锁冲突失败计数（每次 tryLock 冲突 +1）。
     * 由 {@code StockMoveBookkeeper.updateBalanceWithRetry} 冲突路径调用。
     *
     * @param registry 目标 registry；null 走 {@link GlobalMeterRegistry#instance()}
     */
    public static void recordOptimisticLockFailure(MeterRegistry registry) {
        optimisticLockCounter(registry, OPERATION_STOCK_BALANCE_UPDATE).increment();
    }

    /**
     * 指标 4 补充：乐观锁重试耗尽事件计数（区分冲突频次 vs 最终放弃事件）。
     * 由 {@code StockMoveBookkeeper.updateBalanceWithRetry} 在 {@code attempts > maxRetry} 分支调用。
     */
    public static void recordOptimisticLockFailureExhausted(MeterRegistry registry) {
        optimisticLockCounter(registry, OPERATION_STOCK_BALANCE_UPDATE_EXHAUSTED).increment();
    }

    /** 指标 6 path=costing_reclose：期末成本兜底重算关键路径吞吐。 */
    public static void recordCostingReclosePathThroughput(MeterRegistry registry) {
        Counter.builder(METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(TAG_PATH, PATH_COSTING_RECLOSE)
                .register(resolveRegistry(registry))
                .increment();
    }

    private static Counter optimisticLockCounter(MeterRegistry registry, String operation) {
        return Counter.builder(METRIC_OPTIMISTIC_LOCK_FAILURE)
                .tag(TAG_DOMAIN, DOMAIN_INVENTORY)
                .tag(TAG_OPERATION, operation)
                .register(resolveRegistry(registry));
    }

    private static MeterRegistry resolveRegistry(MeterRegistry registry) {
        return registry != null ? registry : GlobalMeterRegistry.instance();
    }
}
