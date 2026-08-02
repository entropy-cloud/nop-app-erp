package app.erp.common.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.LocalDate;

/**
 * 冻结时钟的测试扩展基类（plan 2026-08-01-1357-1 Phase 1 / 设计文档 §3.3 路径 C）。
 *
 * <p>15 个域的 {@code *FrozenClockExtension} 共用此基类。子类仅需声明 {@link #referenceDate}
 * 和提供构造函数传入日期。
 *
 * <p>实现委托 {@link ThreadLocalFrozenClock}：{@code beforeAll} 安装线程本地冻结值，
 * {@code afterAll} 清除线程本地值——不再整体替换 {@code CoreMetrics} 全局静态槽（根治并行不安全）。
 * 仅冻结日期（{@code currentDate}/{@code currentDateTime}），保留 {@code currentTimeMillis}/
 * {@code nanoTime} 走真实系统时钟。
 */
public abstract class AbstractFrozenClockExtension implements BeforeAllCallback, AfterAllCallback {

    protected final LocalDate referenceDate;

    protected AbstractFrozenClockExtension(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ThreadLocalFrozenClock.ensureRegistered();
        ThreadLocalFrozenClock.install(referenceDate);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        ThreadLocalFrozenClock.clear();
    }
}
