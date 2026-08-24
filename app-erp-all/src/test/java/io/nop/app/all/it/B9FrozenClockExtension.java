package io.nop.app.all.it;

import app.erp.common.test.AbstractFrozenClockExtension;
import app.erp.common.test.ThreadLocalFrozenClock;
import java.time.LocalDate;

/**
 * 冻结 CoreMetrics 时钟到 B9 两用例（C18 contract / C19 b2b）共用参考日（镜像
 * {@code C15C16C17FrozenClockExtension} / C07/C13C14 先例，app-erp-all 侧本地扩展）。
 * 参考日 2026-07-17 ∈ 2026-07 OPEN 会计期间（seed period 1，org 2）。
 *
 * <p>B9 引入依据（plan 冻结时钟纪律条款「仅当日期敏感漂移时按先例引入」的执行期判定）：
 * ct 域 signDate/versionDate/accrualDate 为非 clock-tag 字面 CoreMetrics.today() 落库列
 * （RECORDING→CHECKING 跨日即漂移），b2b→pur 收货链 businessDate / log 到岸成本单
 * code 尾缀 currentTimeMillis / businessDate 同源——故 C18/C19 均冻结时钟取确定值。
 */
public final class B9FrozenClockExtension extends AbstractFrozenClockExtension {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    public B9FrozenClockExtension() {
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
