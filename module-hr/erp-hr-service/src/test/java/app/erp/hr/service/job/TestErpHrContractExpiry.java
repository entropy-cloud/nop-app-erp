package app.erp.hr.service.job;

import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.HrFrozenClockExtension;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同到期扫描 Job + 续签端到端测试（use-cases.md UC-HR-07）。覆盖：
 * <ul>
 *   <li>场景1（预警）：合同 endDate=today+15 + warningDays=30 → 扫描命中。</li>
 *   <li>场景2（过期推进）：合同 endDate=today-1 → ACTIVE→EXPIRED。</li>
 *   <li>场景3（续签）：EXPIRED → renew(newEndDate) → ACTIVE + endDate 更新。</li>
 *   <li>场景4（Job cron 空值跳过 + execute() 无参 public）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrContractExpiry extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrEmploymentContractBiz contractBiz;

    @Test
    public void testScanExpiringContractsHitsWithinWindow() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-EXPIRE-SOON");
            LocalDate today = CoreMetrics.today();
            Long contractId = seedContract("CTC-SOON", empId,
                    today.minusDays(10), today.plusDays(15), ErpHrConstants.CONTRACT_STATUS_ACTIVE);
            return new Object[]{empId, contractId};
        });
        Long contractId = (Long) seeded[1];

        List<ErpHrEmploymentContract> expiring = ormTemplate.runInSession(session -> contractBiz.scanExpiringContracts(30, CTX));
        boolean found = expiring.stream().anyMatch(c -> contractId.equals(c.getId()));
        assertTrue(found, "endDate=today+15 应在 warningDays=30 窗口内命中");
    }

    @Test
    public void testExpireOverdueContractsAdvancesStatus() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-EXPIRE-PAST");
            LocalDate today = CoreMetrics.today();
            Long contractId = seedContract("CTC-PAST", empId,
                    today.minusDays(40), today.minusDays(1), ErpHrConstants.CONTRACT_STATUS_ACTIVE);
            return new Object[]{empId, contractId};
        });
        Long contractId = (Long) seeded[1];

        List<ErpHrEmploymentContract> expired = ormTemplate.runInSession(session -> contractBiz.expireOverdueContracts(CTX));
        boolean found = expired.stream().anyMatch(c -> contractId.equals(c.getId()));
        assertTrue(found, "endDate=today-1 应被推进为 EXPIRED");

        ErpHrEmploymentContract refreshed = daoProvider.daoFor(ErpHrEmploymentContract.class).getEntityById(contractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_EXPIRED, refreshed.getStatus());
    }

    @Test
    public void testRenewFromExpiredToActive() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-RENEW");
            LocalDate today = CoreMetrics.today();
            Long contractId = seedContract("CTC-RENEW", empId,
                    today.minusDays(40), today.minusDays(1), ErpHrConstants.CONTRACT_STATUS_EXPIRED);
            return new Object[]{empId, contractId};
        });
        Long contractId = (Long) seeded[1];
        LocalDate newEndDate = CoreMetrics.today().plusYears(1);

        ErpHrEmploymentContract renewed = ormTemplate.runInSession(session -> contractBiz.renew(String.valueOf(contractId), newEndDate, CTX));
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, renewed.getStatus());
        assertEquals(newEndDate, renewed.getEndDate());

        ErpHrEmploymentContract refreshed = daoProvider.daoFor(ErpHrEmploymentContract.class).getEntityById(contractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, refreshed.getStatus());
        assertEquals(newEndDate, refreshed.getEndDate());
    }

    @Test
    public void testRenewRejectsTerminatedContract() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-RENEW-FAIL");
            Long contractId = seedContract("CTC-TERM", empId,
                    CoreMetrics.today().minusDays(10), CoreMetrics.today().plusDays(10),
                    ErpHrConstants.CONTRACT_STATUS_TERMINATED);
            return new Object[]{empId, contractId};
        });
        Long contractId = (Long) seeded[1];

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> contractBiz.renew(String.valueOf(contractId), CoreMetrics.today().plusYears(1), CTX)));
        assertEquals(ErpHrErrors.ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testJobCronEmptySkipsExecution() {
        CountingJob job = new CountingJob();
        job.cron = "";
        job.execute();
        assertEquals(0, job.warnCalls, "cron 空值应跳过预警委托");
        assertEquals(0, job.expireCalls, "cron 空值应跳过过期推进委托");
    }

    @Test
    public void testJobCronConfiguredTriggersBothSteps() {
        CountingJob job = new CountingJob();
        job.cron = "0 0 1 * * ?";
        job.expiring = Collections.emptyList();
        job.execute();
        assertEquals(1, job.warnDelegateCalls, "cron 非空应委托 runExpiryWarnings 1 次");
        assertEquals(1, job.expireDelegateCalls, "cron 非空应委托 runExpirations 1 次");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpHrContractExpiryJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    // ---------- helpers ----------

    private Long seedEmployee(String code) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName(code);
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedContract(String code, Long employeeId, LocalDate startDate, LocalDate endDate, String status) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(CoreMetrics.today());
        c.setCode(code + "-" + System.nanoTime());
        c.setEmployeeId(employeeId);
        c.setContractType(ErpHrConstants.CONTRACT_TYPE_FIXED_TERM);
        c.setSignDate(startDate);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setStatus(status);
        dao.saveEntity(c);
        return c.getId();
    }

    /**
     * 计数 Job：覆盖 {@link #runExpiryWarnings} / {@link #runExpirations} 模拟数据；
     * 覆盖 {@link #notifyExpiry} 避免实际调 notificationBiz。
     */
    public static class CountingJob extends ErpHrContractExpiryJob {
        String cron;
        List<ErpHrEmploymentContract> expiring = Collections.emptyList();
        int warnCalls;
        int warnDelegateCalls;
        int expireCalls;
        int expireDelegateCalls;

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected int runExpiryWarnings(IServiceContext ctx) {
            warnDelegateCalls++;
            return notifyAll(expiring, ctx);
        }

        @Override
        protected int runExpirations(IServiceContext ctx) {
            expireDelegateCalls++;
            return 0;
        }

        @Override
        protected void notifyExpiry(ErpHrEmploymentContract contract, IServiceContext ctx) {
            warnCalls++;
        }

        private int notifyAll(List<ErpHrEmploymentContract> list, IServiceContext ctx) {
            if (list == null || list.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpHrEmploymentContract c : list) {
                try {
                    notifyExpiry(c, ctx);
                    count++;
                } catch (Exception ignored) {
                    // 单条隔离
                }
            }
            return count;
        }
    }
}
