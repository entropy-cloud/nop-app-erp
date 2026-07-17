package app.erp.qa.service;

import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.time.IClock;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冻结 {@link CoreMetrics} 时钟到 quality 日期敏感型 auto-test 快照的参考日 {@link #REFERENCE_DATE}，
 * 使依赖 {@code CoreMetrics.today()}/{@code CoreMetrics.currentDate()} 派生的快照（如 inspection inspectionDate、
 * non_conformance ncrDate、action verificationDate/dueDate、spc_capability periodFrom/periodTo、risk_register riskDate）
 * 跨日稳定，不再随系统日历翻页而漂移。
 *
 * <p>与 {@code HrFrozenClockExtension}/{@code FinFrozenClockExtension} 同型：仅冻结日期
 * （{@code currentDate}/{@code currentDateTime}），保留 {@code currentTimeMillis}/{@code nanoTime} 走真实系统时钟——
 * 这样 {@code ContextProvider} 等依赖时间单调推进的设施（上下文生命周期、审计拦截器）不受影响；
 * 快照中的时间戳列已被 auto-test 框架通配（{@code *}），日期列均由 {@code CoreMetrics.today()} 派生，
 * 故只需钉住日期。</p>
 *
 * <p>作用域为单个测试类：beforeAll 注册冻结时钟，afterAll 恢复系统默认时钟，不影响其它测试类。</p>
 */
public final class QaFrozenClockExtension implements BeforeAllCallback, AfterAllCallback {

    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 17);

    private static final IClock FROZEN_CLOCK = new IClock() {
        private final IClock system = CoreMetrics.defaultClock();

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
            return REFERENCE_DATE;
        }

        @Override
        public LocalDateTime currentDateTime() {
            return REFERENCE_DATE.atStartOfDay();
        }
    };

    @Override
    public void beforeAll(ExtensionContext context) {
        installFrozenClock();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        restoreSystemClock();
    }

    public static void installFrozenClock() {
        CoreMetrics.registerClock(FROZEN_CLOCK);
    }

    public static void restoreSystemClock() {
        CoreMetrics.registerClock(CoreMetrics.defaultClock());
    }
}
