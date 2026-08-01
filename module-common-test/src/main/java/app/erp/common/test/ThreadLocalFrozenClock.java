package app.erp.common.test;

import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.time.IClock;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应用层 thread-local delegating clock（plan 2026-08-01-1357-1 Phase 1 / 设计文档 §3.3 路径 C）。
 *
 * <p>根治 {@link CoreMetrics#getClock() CoreMetrics 全局静态时钟} 的进程级单槽并行不安全缺陷：
 * 本类经 per-fork 一次性 {@link CoreMetrics#registerClock(IClock) registerClock} 挂到全局静态槽，
 * 此后所有日期读取委托到 <b>线程本地</b> 冻结值；不同线程互不串扰，使未来 surefire
 * {@code parallel=methods}/{@code threadCount>1} 也可安全并行。
 *
 * <ul>
 *   <li>{@link #currentTimeMillis()} / {@link #nanoTime()} 始终委托 {@link CoreMetrics#defaultClock()}，
 *       保留单调时间真实，不破坏 {@code ContextProvider} 等依赖时间推进的设施。</li>
 *   <li>{@link #currentDate()} / {@link #currentDateTime()} 当线程本地已 {@link #install(LocalDate)} 冻结值时
 *       返回冻结日期，否则委托 {@link CoreMetrics#defaultClock()}（系统真实日期）。</li>
 * </ul>
 *
 * <p><b>注册时序</b>：本类 {@code static {}} 静态初始化块在类加载时触发 {@link #ensureRegistered()}；
     又因平台 {@code NopJunitExtension.afterAll} 每个测试类结束后重置全局时钟为系统时钟（见
     {@link #ensureRegistered()} javadoc），各冻结扩展 {@code beforeAll} 须再次调 {@link #ensureRegistered()}
     重新挂载 delegating clock，随后 {@link #install(LocalDate)} 设线程本地冻结值。
 */
public class ThreadLocalFrozenClock implements IClock {

    private static final ThreadLocal<LocalDate> REF_DATE = new ThreadLocal<>();

    static {
        ensureRegistered();
    }

    /**
     * 将 delegating clock 挂到 {@link CoreMetrics} 全局静态槽。
     *
     * <p><b>每次调用均重新注册</b>（非幂等跳过）：平台 {@code NopJunitExtension.afterAll} 在每个测试类结束后
     * 执行 {@code CoreMetrics.registerClock(CoreMetrics.defaultClock())} 将全局槽重置为系统时钟（已核验
     * {@code ../nop-entropy/nop-autotest/nop-autotest-junit/.../NopJunitExtension.java:66}），故设计文档 §3.3
     * 假设的「per-fork 一次性注册持久」不成立。此处每次 beforeAll 重新挂载 delegating clock 以保证当前类
     * 冻结生效。平台无 {@code s_clock} 内省 API（private 无 getter），无法判断是否已被重置，故无条件注册。
     * delegating clock 在 REF_DATE 未 set 时委托 {@link CoreMetrics#defaultClock()}（系统真实时钟），对非冻结
     * 测试无行为影响。
     */
    public static void ensureRegistered() {
        CoreMetrics.registerClock(new ThreadLocalFrozenClock());
    }

    public static void install(LocalDate referenceDate) {
        REF_DATE.set(referenceDate);
    }

    public static void clear() {
        REF_DATE.remove();
    }

    public static boolean isFrozen() {
        return REF_DATE.get() != null;
    }

    @Override
    public long currentTimeMillis() {
        return CoreMetrics.defaultClock().currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return CoreMetrics.defaultClock().nanoTime();
    }

    @Override
    public LocalDate currentDate() {
        LocalDate frozen = REF_DATE.get();
        return frozen != null ? frozen : CoreMetrics.defaultClock().currentDate();
    }

    @Override
    public LocalDateTime currentDateTime() {
        LocalDate frozen = REF_DATE.get();
        return frozen != null ? frozen.atStartOfDay() : CoreMetrics.defaultClock().currentDateTime();
    }
}
