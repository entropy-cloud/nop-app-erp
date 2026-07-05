package app.erp.cs.service.job;

import app.erp.cs.dao.entity.ErpCsSurvey;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时 CSAT 调查到期提醒 Job Bean 测试（plan 2026-07-06-0642-1 §Phase 2）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空委托到 {@code runReminders}（统计未响应/过期调查→notify 调用次数）、
 * execute() 为 public 无参方法。复用 {@code TestErpCsSlaScanJob} 范式。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsCsatReminderJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob();
        job.cron = "";
        job.execute();
        assertEquals(0, job.notifyCalls, "cron 空值应跳过 CSAT 提醒委托");
    }

    @Test
    public void testCronConfiguredTriggersNotifyPerSurvey() {
        CountingJob job = new CountingJob();
        job.cron = "0 0 2 * * ?";
        // 模拟 findSurveyReminders 返回 1 条 + findExpiredSurveys 返回 1 条
        job.reminders = Collections.singletonList(newSurvey(201L));
        job.expired = Collections.singletonList(newSurvey(202L));
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 runReminders 1 次");
        assertEquals(2, job.notifyCalls, "1 提醒 + 1 过期应通知 2 次");
    }

    @Test
    public void testCronConfiguredWithNoSurveysSkipsNotify() {
        CountingJob job = new CountingJob();
        job.cron = "0 0 2 * * ?";
        job.reminders = Collections.emptyList();
        job.expired = Collections.emptyList();
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 runReminders 1 次");
        assertEquals(0, job.notifyCalls, "无未响应/过期调查时应跳过 notify");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpCsCsatReminderJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private ErpCsSurvey newSurvey(Long id) {
        ErpCsSurvey s = new ErpCsSurvey();
        s.orm_propValueByName("id", id);
        return s;
    }

    /**
     * 计数 Job：覆盖 {@link #runReminders} 模拟数据；覆盖 {@link #notifySurvey} 避免实际调 notificationBiz。
     */
    public static class CountingJob extends ErpCsCsatReminderJob {
        String cron;
        List<ErpCsSurvey> reminders = Collections.emptyList();
        List<ErpCsSurvey> expired = Collections.emptyList();
        int delegateCalls;
        int notifyCalls;

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected int runReminders(IServiceContext ctx) {
            delegateCalls++;
            int n = 0;
            n += notifyAll(reminders, "REMINDER", ctx);
            n += notifyAll(expired, "EXPIRED", ctx);
            return n;
        }

        @Override
        protected void notifySurvey(ErpCsSurvey survey, String state, IServiceContext ctx) {
            notifyCalls++;
        }
    }
}
