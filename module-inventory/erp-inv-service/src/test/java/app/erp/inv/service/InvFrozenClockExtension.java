package app.erp.inv.service;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 inv 域测试参考日。
 * 基类 AbstractFrozenClockExtension 经 ThreadLocalFrozenClock 提供冻结/恢复机制；本类仅声明参考日期。
 */
public final class InvFrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public InvFrozenClockExtension() {
        super(REFERENCE_DATE);
    }

    public static void installFrozenClock() {
        ThreadLocalFrozenClock.ensureRegistered();
        ThreadLocalFrozenClock.install(REFERENCE_DATE);
    }

    public static void restoreSystemClock() {
        ThreadLocalFrozenClock.clear();
    }
}
