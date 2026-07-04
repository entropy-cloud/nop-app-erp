package app.erp.ast.service.job;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时批量折旧 Job Bean 测试（plan 2026-07-05-0306-1 Phase 3）。
 *
 * <p>覆盖：cron 空值跳过（不委托 BizModel）、cron 非空委托到 executeBatchDepreciation、
 * execute() 为 public 无参方法（BeanMethodJobInvoker 反射兼容）。
 *
 * <p>沿用 finance 参考实现范式：可重写 {@code resolveCronConfig()} 的匿名子类 + 计数 runBatchDepreciation
 * 装饰器验证门控语义，不依赖 Mockito。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstDepreciationJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob("");
        job.execute();
        assertEquals(0, job.delegateCalls, "cron 空值应跳过折旧委托");
    }

    @Test
    public void testCronConfiguredTriggersDelegation() {
        CountingJob job = new CountingJob("0 0 2 1 * ?");
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 executeBatchDepreciation 1 次");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpAstDepreciationJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private static class CountingJob extends ErpAstDepreciationJob {
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
        protected int runBatchDepreciation(String period, IServiceContext ctx) {
            delegateCalls++;
            return 0;
        }
    }
}
