package app.erp.crm.service.job;

import app.erp.crm.dao.entity.ErpCrmLead;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时线索评分批量重算 Job Bean 测试（plan 2026-07-05-0306-1 Phase 3）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空迭代 active 线索逐条委托 recalculateScore、
 * 单线索失败隔离不阻断后续线索、execute() 为 public 无参方法。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmLeadScoringRecalcJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob();
        job.cron = "";
        job.activeLeads = Collections.emptyList();
        job.execute();
        assertEquals(0, job.delegateCalls, "cron 空值应跳过评分重算委托");
    }

    @Test
    public void testCronConfiguredIteratesAllActiveLeads() {
        CountingJob job = new CountingJob();
        job.cron = "0 2 * * *";
        job.activeLeads = Arrays.asList(newLead(101L), newLead(102L), newLead(103L));
        job.execute();
        assertEquals(3, job.delegateCalls, "cron 非空应迭代全部 active 线索逐条委托");
    }

    @Test
    public void testSingleLeadFailureIsolationDoesNotBlockOthers() {
        CountingJob job = new CountingJob();
        job.cron = "0 2 * * *";
        job.activeLeads = Arrays.asList(newLead(201L), newLead(202L), newLead(203L));
        job.failingLeadId = 202L;
        job.execute();
        assertEquals(3, job.delegateCalls, "全部 3 条线索应都被尝试委托");
        assertEquals(2, job.successCount, "单线索失败应隔离，其余 2 条仍成功");
        assertEquals(1, job.delegateCalls - job.successCount, "应记录 1 条失败");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpCrmLeadScoringRecalcJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    private static ErpCrmLead newLead(Long id) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        return lead;
    }

    private static class CountingJob extends ErpCrmLeadScoringRecalcJob {
        String cron;
        java.util.List<ErpCrmLead> activeLeads = Collections.emptyList();
        Long failingLeadId;
        int delegateCalls;
        int successCount;

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected java.util.List<ErpCrmLead> findActiveLeads(IServiceContext ctx) {
            return activeLeads;
        }

        @Override
        protected void runRecalculateScore(Long leadId, IServiceContext ctx) {
            delegateCalls++;
            if (failingLeadId != null && failingLeadId.equals(leadId)) {
                throw new RuntimeException("simulated lead scoring failure for " + leadId);
            }
            successCount++;
        }
    }
}
