package app.erp.ast.service;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 ast 域测试参考日（plan 2026-08-01-1357-1 Phase 3 / 设计文档 §4.1 step 4）。
 * 基类 AbstractFrozenClockExtension 经 ThreadLocalFrozenClock 提供冻结/恢复机制；本类仅声明参考日期。
 */
public final class AstFrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public AstFrozenClockExtension() {
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
