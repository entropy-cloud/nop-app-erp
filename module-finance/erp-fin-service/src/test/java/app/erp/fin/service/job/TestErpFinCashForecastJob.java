package app.erp.fin.service.job;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时现金预测刷新 Job Bean 测试（plan 2026-07-05-0306-1 Phase 3）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空委托到 refreshForecast、execute() 为 public 无参方法。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinCashForecastJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob("");
        job.execute();
        assertEquals(0, job.delegateCalls, "cron 空值应跳过现金预测刷新委托");
    }

    @Test
    public void testCronConfiguredTriggersDelegation() {
        CountingJob job = new CountingJob("0 0 1 * * ?");
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 refreshForecast 1 次");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpFinCashForecastJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private static class CountingJob extends ErpFinCashForecastJob {
        private final String cron;
        int delegateCalls;

        CountingJob(String cron) {
            this.cron = cron;
        }

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected int runRefreshForecast(LocalDate fromDate, LocalDate toDate, IServiceContext ctx) {
            delegateCalls++;
            return 0;
        }
    }
}
