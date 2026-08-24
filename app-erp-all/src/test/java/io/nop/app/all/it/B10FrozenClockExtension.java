package io.nop.app.all.it;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 B10 两用例（C20a drp 净需求释放 / C20b drp 仿真提升）共用参考日（镜像
 * {@code B9FrozenClockExtension} / C15C16C17/C13C14 先例，app-erp-all 侧本地扩展）。
 * 参考日 2026-07-17 ∈ 2026-07 OPEN 会计期间（seed period 1，org 2）。
 *
 * <p>B10 引入依据（plan 冻结时钟纪律条款「若 RECORDING 发现日期敏感漂移即启用」的执行期判定）：
 * drp 释放链 TransferOrder/PurchaseOrder businessDate = CoreMetrics.today() 落库列、
 * 计划 runAt = currentTimestamp()（clock-tag 列 @var 掩码，但跨日漂移先例已证）——跨日漂移按
 * drp 域 {@code DrpFrozenClockExtension}（REFERENCE_DATE 2026-07-17）先例冻结取确定值。
 */
public final class B10FrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public B10FrozenClockExtension() {
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
