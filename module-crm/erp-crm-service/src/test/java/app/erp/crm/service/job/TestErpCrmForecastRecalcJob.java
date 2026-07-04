package app.erp.crm.service.job;

import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
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
 * 定时销售预测重算 Job Bean 测试（plan 2026-07-05-0306-1 Phase 3）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空且存在 OPEN 期间时委托到 refreshForecast、
 * cron 非空但无 OPEN 期间时跳过（不委托）、execute() 为 public 无参方法。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmForecastRecalcJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob(true);
        job.cron = "";
        job.execute();
        assertEquals(0, job.delegateCalls, "cron 空值应跳过预测重算委托");
    }

    @Test
    public void testCronConfiguredWithOpenPeriodTriggersDelegation() {
        CountingJob job = new CountingJob(true);
        job.cron = "0 3 * * *";
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空且存在 OPEN 期间应委托 refreshForecast 1 次");
    }

    @Test
    public void testCronConfiguredWithoutOpenPeriodSkipsDelegation() {
        CountingJob job = new CountingJob(false);
        job.cron = "0 3 * * *";
        job.execute();
        assertEquals(0, job.delegateCalls, "无 OPEN 期间覆盖今天时应跳过委托");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpCrmForecastRecalcJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private static class CountingJob extends ErpCrmForecastRecalcJob {
        String cron;
        private final boolean hasOpenPeriod;
        int delegateCalls;

        CountingJob(boolean hasOpenPeriod) {
            this.hasOpenPeriod = hasOpenPeriod;
        }

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected ErpCrmForecastPeriod findOpenPeriod(LocalDate today, IServiceContext ctx) {
            return hasOpenPeriod ? new ErpCrmForecastPeriod() : null;
        }

        @Override
        protected void runRefreshForecast(Long periodId, IServiceContext ctx) {
            delegateCalls++;
        }
    }
}
