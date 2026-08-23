package io.nop.app.all.it;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 fin 域测试参考日（镜像 erp-fin-service 测试 {@code FinFrozenClockExtension}，
 * app-erp-all 侧 app-erp-common-test 主依赖可访问）。参考日 2026-07-17 ∈ 2026-07 OPEN 会计期间
 * （seed period 1，org 2），C13/C14 两用例共用（C07/C11/C12 先例复用）。
 */
public final class C13C14FrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public C13C14FrozenClockExtension() {
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