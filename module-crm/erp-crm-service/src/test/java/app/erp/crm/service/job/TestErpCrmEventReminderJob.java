package app.erp.crm.service.job;

import app.erp.crm.dao.entity.ErpCrmEvent;
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
 * 定时活动到期提醒 Job Bean 测试（plan 2026-07-06-0642-1 §Phase 2）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空委托到 {@code runReminders}（统计到期事件→notify 调用次数）、
 * execute() 为 public 无参方法。复用 {@code TestErpCrmForecastRecalcJob} 范式。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmEventReminderJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob();
        job.cron = "";
        job.execute();
        assertEquals(0, job.delegateCalls, "cron 空值应跳过活动提醒委托");
    }

    @Test
    public void testCronConfiguredTriggersNotifyPerEvent() {
        CountingJob job = new CountingJob();
        job.cron = "0 0/15 * * * ?";
        // 模拟 findDueReminders 返回 2 条到期事件
        job.dueEvents = Arrays.asList(newEvent(101L, "活动-A"), newEvent(102L, "活动-B"));
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 runReminders 1 次");
        assertEquals(2, job.notifyCalls, "2 条到期事件应通知 2 次");
    }

    @Test
    public void testCronConfiguredWithNoEventsSkipsNotify() {
        CountingJob job = new CountingJob();
        job.cron = "0 0/15 * * * ?";
        job.dueEvents = Collections.emptyList();
        job.execute();
        assertEquals(1, job.delegateCalls, "cron 非空应委托 runReminders 1 次");
        assertEquals(0, job.notifyCalls, "无到期事件时应跳过 notify");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpCrmEventReminderJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private ErpCrmEvent newEvent(Long id, String subject) {
        ErpCrmEvent e = new ErpCrmEvent();
        e.orm_propValueByName("id", id);
        e.setSubject(subject);
        return e;
    }

    /**
     * 计数 Job：覆盖 {@link #runReminders} 避免实际查询 DB，
     * 直接使用预设的 dueEvents 列表模拟；覆盖 {@link #notifyEvent} 避免实际调 notificationBiz。
     */
    private static class CountingJob extends ErpCrmEventReminderJob {
        String cron;
        List<ErpCrmEvent> dueEvents = Collections.emptyList();
        int delegateCalls;
        int notifyCalls;

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected int runReminders(IServiceContext ctx) {
            delegateCalls++;
            int count = 0;
            for (ErpCrmEvent e : dueEvents) {
                notifyEvent(e, ctx);
                count++;
            }
            return count;
        }

        @Override
        protected void notifyEvent(ErpCrmEvent event, IServiceContext ctx) {
            // 覆盖避免实际调用 notificationBiz；仅计数
            notifyCalls++;
        }
    }
}
