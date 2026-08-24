package io.nop.app.all.it;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 B8 三用例（C15 CRM / C16 CS / C17 HR）共用参考日（镜像
 * {@code HrFrozenClockExtension} / C07/C13C14 先例，app-erp-all 侧本地扩展）。参考日
 * 2026-07-17 ∈ 2026-07 OPEN 会计期间（seed period 1，org 2）：C16 SLA deadline/达标判断、
 * C17 薪资 businessDate/凭证日期（2026-07-15 ∈ 期间）与 C15 单据日期均取确定值。
 */
public final class C15C16C17FrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public C15C16C17FrozenClockExtension() {
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
