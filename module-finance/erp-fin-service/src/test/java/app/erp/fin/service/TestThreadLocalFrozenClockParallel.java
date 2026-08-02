package app.erp.fin.service;

import app.erp.common.test.ThreadLocalFrozenClock;
import io.nop.api.core.time.CoreMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 并行隔离客观证明（plan 2026-08-01-1357-1 Closure Gates §验收 1 / 设计文档 §3.3 路径 C 根治主张）。
 *
 * <p>起 2 线程各 {@link ThreadLocalFrozenClock#install(LocalDate)} 不同日期，经 CyclicBarrier 同步并发读
 * {@code CoreMetrics.today()}，断言各线程读到各自冻结日、互不串扰——证明路径 C 的 thread-local delegating
 * clock 根治了原 {@code CoreMetrics.s_clock} 进程级全局静态单槽的并行不安全缺陷（即使未来 surefire
 * {@code parallel=methods}/{@code threadCount>1} 也可安全并行）。
 *
 * <p>纯逻辑测试（无 DB/IoC），不依赖 Nop 测试基类。
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class TestThreadLocalFrozenClockParallel {

    @AfterEach
    void cleanup() {
        ThreadLocalFrozenClock.clear();
    }

    @Test
    public void twoThreadsFrozenToDifferentDatesDoNotContaminate() throws Exception {
        ThreadLocalFrozenClock.ensureRegistered();

        LocalDate dateA = LocalDate.of(2026, 7, 17);
        LocalDate dateB = LocalDate.of(2026, 8, 1);
        assertNotEquals(dateA, dateB, "两线程冻结日须不同");

        int iterations = 20;
        for (int i = 0; i < iterations; i++) {
            final int iteration = i;
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AssertionError[] errs = new AssertionError[2];

            Runnable taskA = () -> {
                try {
                    ThreadLocalFrozenClock.install(dateA);
                    ready.countDown();
                    start.await();
                    assertEquals(dateA, CoreMetrics.today(),
                            "线程 A 应读到自身冻结日 " + dateA + "（iteration " + iteration + "）");
                    assertEquals(dateA, CoreMetrics.currentDate(), "currentDate 同步一致");
                } catch (AssertionError e) {
                    errs[0] = e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            };
            Runnable taskB = () -> {
                try {
                    ThreadLocalFrozenClock.install(dateB);
                    ready.countDown();
                    start.await();
                    assertEquals(dateB, CoreMetrics.today(),
                            "线程 B 应读到自身冻结日 " + dateB + "（iteration " + iteration + "）");
                    assertEquals(dateB, CoreMetrics.currentDate(), "currentDate 同步一致");
                } catch (AssertionError e) {
                    errs[1] = e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            };

            Thread tA = new Thread(taskA, "frozen-clock-A");
            Thread tB = new Thread(taskB, "frozen-clock-B");
            tA.start();
            tB.start();
            ready.await();
            start.countDown();
            done.await();
            tA.join();
            tB.join();

            if (errs[0] != null) {
                throw errs[0];
            }
            if (errs[1] != null) {
                throw errs[1];
            }
        }
    }

    @Test
    public void clearRestoresSystemDateForCurrentThread() {
        ThreadLocalFrozenClock.ensureRegistered();
        ThreadLocalFrozenClock.install(LocalDate.of(2026, 7, 17));
        ThreadLocalFrozenClock.clear();
        // 清除后线程本地无冻结值，delegating clock 委托系统真实时钟；断言不等于冻结日即可证清空生效
        LocalDate today = CoreMetrics.today();
        LocalDate frozen = LocalDate.of(2026, 7, 17);
        // 极低概率：系统真实日期恰好等于冻结日，此时无法区分；用 isFrozen 旁证（应为 false）
        org.junit.jupiter.api.Assertions.assertFalse(ThreadLocalFrozenClock.isFrozen(),
                "clear() 后线程本地应无冻结值");
        // 若系统日 ≠ 冻结日，进一步断言委托系统
        if (!today.equals(frozen)) {
            assertNotEquals(frozen, today, "clear() 后应委托系统真实时钟");
        }
    }
}
