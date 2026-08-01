package app.erp.inv.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.nop.commons.metrics.GlobalMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * inventory 域业务指标单测（observability.md §7.1 步骤 4 + §7.2 criterion 5）。
 *
 * <p>覆盖指标 4（{@code erp_concurrency_optimistic_lock_failure_total} Counter）+
 * 指标 6 inventory costing_reclose 路径。
 *
 * <p>不依赖 IoC 容器——直接传 {@link SimpleMeterRegistry} 验证 SPI 接入语义。
 */
public class TestErpInvConcurrencyMetricsUnit {

    @Test
    public void optimisticLockFailureCounterRegisteredPerOperation() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(registry);
        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(registry);
        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(registry);
        ErpInvConcurrencyMetrics.recordOptimisticLockFailureExhausted(registry);

        Counter conflictCounter = registry.find(ErpInvConcurrencyMetrics.METRIC_OPTIMISTIC_LOCK_FAILURE)
                .tag(ErpInvConcurrencyMetrics.TAG_DOMAIN, ErpInvConcurrencyMetrics.DOMAIN_INVENTORY)
                .tag(ErpInvConcurrencyMetrics.TAG_OPERATION,
                        ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE)
                .counter();
        assertNotNull(conflictCounter, "optimistic lock failure counter (per-conflict) must be registered");
        assertEquals(3.0, conflictCounter.count(), "3 conflicts recorded");

        Counter exhaustedCounter = registry.find(ErpInvConcurrencyMetrics.METRIC_OPTIMISTIC_LOCK_FAILURE)
                .tag(ErpInvConcurrencyMetrics.TAG_DOMAIN, ErpInvConcurrencyMetrics.DOMAIN_INVENTORY)
                .tag(ErpInvConcurrencyMetrics.TAG_OPERATION,
                        ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE_EXHAUSTED)
                .counter();
        assertNotNull(exhaustedCounter, "retry-exhausted counter must be registered");
        assertEquals(1.0, exhaustedCounter.count(), "1 exhaustion event recorded");
    }

    @Test
    public void costingReclosePathThroughputCounterRegistered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        ErpInvConcurrencyMetrics.recordCostingReclosePathThroughput(registry);
        ErpInvConcurrencyMetrics.recordCostingReclosePathThroughput(registry);

        Counter c = registry.find(ErpInvConcurrencyMetrics.METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(ErpInvConcurrencyMetrics.TAG_PATH, ErpInvConcurrencyMetrics.PATH_COSTING_RECLOSE)
                .counter();
        assertNotNull(c, "costing_reclose path counter must be registered");
        assertEquals(2.0, c.count(), "2 costing reclose events recorded");
    }

    /**
     * 生产路径接入验证（observability.md §7.1 步骤 4 + §7.2 criterion 5）：
     * 静态助手 {@code registry=null} → {@link GlobalMeterRegistry#instance()}（经
     * {@code QuarkusIntegration.start():48-51} 桥接流入 {@code /q/metrics}）。覆盖指标 4
     * （optimistic_lock_failure Counter）+ 指标 6 costing_reclose 路径（throughput Counter）。
     *
     * <p>确定性：surefire 单 fork 顺序执行；固定枚举 tag 使用 before/after delta capture（同 fork 内可能已有
     * 集成测试经 StockMoveBookkeeper 写入全局 optimistic_lock_failure counter）。
     */
    @Test
    public void globalMeterRegistryPathRegistersInventoryBusinessMetrics() {
        MeterRegistry global = GlobalMeterRegistry.instance();

        double beforeConflict = optimisticLockCountInGlobal(global,
                ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE);
        double beforeExhausted = optimisticLockCountInGlobal(global,
                ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE_EXHAUSTED);
        double beforeCosting = pathCounterInGlobal(global);

        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(null);
        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(null);
        ErpInvConcurrencyMetrics.recordOptimisticLockFailureExhausted(null);
        ErpInvConcurrencyMetrics.recordCostingReclosePathThroughput(null);

        assertNotNull(global.find(ErpInvConcurrencyMetrics.METRIC_OPTIMISTIC_LOCK_FAILURE).meter(),
                "erp_concurrency_optimistic_lock_failure_total registered in GlobalMeterRegistry");
        assertNotNull(global.find(ErpInvConcurrencyMetrics.METRIC_BUSINESS_PATH_THROUGHPUT).meter(),
                "erp_business_path_throughput_total registered in GlobalMeterRegistry");

        assertEquals(beforeConflict + 2, optimisticLockCountInGlobal(global,
                ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE), 1e-9,
                "optimistic lock conflict delta = 2");
        assertEquals(beforeExhausted + 1, optimisticLockCountInGlobal(global,
                ErpInvConcurrencyMetrics.OPERATION_STOCK_BALANCE_UPDATE_EXHAUSTED), 1e-9,
                "optimistic lock exhausted delta = 1");
        assertEquals(beforeCosting + 1, pathCounterInGlobal(global), 1e-9,
                "costing_reclose path delta = 1");
    }

    private static double optimisticLockCountInGlobal(MeterRegistry registry, String operation) {
        Counter c = registry.find(ErpInvConcurrencyMetrics.METRIC_OPTIMISTIC_LOCK_FAILURE)
                .tag(ErpInvConcurrencyMetrics.TAG_DOMAIN, ErpInvConcurrencyMetrics.DOMAIN_INVENTORY)
                .tag(ErpInvConcurrencyMetrics.TAG_OPERATION, operation)
                .counter();
        return c == null ? 0.0 : c.count();
    }

    private static double pathCounterInGlobal(MeterRegistry registry) {
        Counter c = registry.find(ErpInvConcurrencyMetrics.METRIC_BUSINESS_PATH_THROUGHPUT)
                .tag(ErpInvConcurrencyMetrics.TAG_PATH, ErpInvConcurrencyMetrics.PATH_COSTING_RECLOSE)
                .counter();
        return c == null ? 0.0 : c.count();
    }
}
