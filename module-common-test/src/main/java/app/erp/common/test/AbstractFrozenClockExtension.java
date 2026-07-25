package app.erp.common.test;

import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.time.IClock;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冻结 {@link CoreMetrics} 时钟的测试扩展基类（plan 2026-07-24-2200-1 Phase 3）。
 *
 * <p>15 个域的 {@code *FrozenClockExtension} 共用此基类。子类仅需声明 {@link #REFERENCE_DATE}
 * 和提供构造函数传入日期。
 *
 * <p>仅冻结日期（{@code currentDate}/{@code currentDateTime}），保留 {@code currentTimeMillis}/
 * {@code nanoTime} 走真实系统时钟——使 {@code ContextProvider} 等依赖时间单调推进的设施不受影响。
 */
public abstract class AbstractFrozenClockExtension implements BeforeAllCallback, AfterAllCallback {

    protected final LocalDate referenceDate;

    protected AbstractFrozenClockExtension(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    private ICClock frozenClock;

    private static class ICClock implements IClock {
        private final IClock system = CoreMetrics.defaultClock();
        private final LocalDate referenceDate;

        ICClock(LocalDate referenceDate) {
            this.referenceDate = referenceDate;
        }

        @Override
        public long currentTimeMillis() {
            return system.currentTimeMillis();
        }

        @Override
        public long nanoTime() {
            return system.nanoTime();
        }

        @Override
        public LocalDate currentDate() {
            return referenceDate;
        }

        @Override
        public LocalDateTime currentDateTime() {
            return referenceDate.atStartOfDay();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        frozenClock = new ICClock(referenceDate);
        CoreMetrics.registerClock(frozenClock);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        CoreMetrics.registerClock(CoreMetrics.defaultClock());
    }
}
