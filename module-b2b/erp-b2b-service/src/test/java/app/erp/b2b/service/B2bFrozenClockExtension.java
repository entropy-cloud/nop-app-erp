package app.erp.b2b.service;

import app.erp.common.test.AbstractFrozenClockExtension;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.time.IClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 b2b 域测试参考日。
 * 基类 AbstractFrozenClockExtension 提供冻结/恢复机制；本类仅声明参考日期。
 */
public final class B2bFrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public B2bFrozenClockExtension() {
        super(REFERENCE_DATE);
    }

    public static void installFrozenClock() {
        CoreMetrics.registerClock(new io.nop.api.core.time.IClock() {
            private final IClock system = CoreMetrics.defaultClock();
            @Override public long currentTimeMillis() { return system.currentTimeMillis(); }
            @Override public long nanoTime() { return system.nanoTime(); }
            @Override public LocalDate currentDate() { return REFERENCE_DATE; }
            @Override public java.time.LocalDateTime currentDateTime() { return REFERENCE_DATE.atStartOfDay(); }
        });
    }

    public static void restoreSystemClock() {
        CoreMetrics.registerClock(CoreMetrics.defaultClock());
    }
}
