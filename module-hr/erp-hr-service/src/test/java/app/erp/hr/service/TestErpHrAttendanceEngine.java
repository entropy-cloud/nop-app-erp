package app.erp.hr.service;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmployee;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 考勤打卡端点端到端测试（use-cases.md UC-HR-06）。覆盖：
 * <ul>
 *   <li>场景1：clockIn → clockOut → workHours 正确计算。</li>
 *   <li>场景2：重复打卡拦截 ERR_ALREADY_CLOCKED_IN。</li>
 *   <li>场景3：未签到签退拦截 ERR_NOT_CLOCKED_IN。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrAttendanceEngine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrAttendanceBiz attendanceBiz;

    @Test
    public void testClockInClockOutComputesWorkHours() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-CLOCK"));

        ErpHrAttendance afterIn = attendanceBiz.clockIn(empId, CTX);
        assertNotNull(afterIn.getClockIn(), "签到后 clockIn 应有值");
        assertNull(afterIn.getClockOut(), "签到后 clockOut 应为空");

        ErpHrAttendance afterOut = attendanceBiz.clockOut(empId, CTX);
        assertNotNull(afterOut.getClockOut(), "签退后 clockOut 应有值");
        assertNotNull(afterOut.getWorkHours(), "签退后 workHours 应已计算");

        ErpHrAttendance today = attendanceBiz.getTodayAttendance(empId, CTX);
        assertNotNull(today);
        assertEquals(today.getClockIn(), afterIn.getClockIn());
    }

    @Test
    public void testDuplicateClockInBlocked() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-DUP"));

        attendanceBiz.clockIn(empId, CTX);

        NopException ex = assertThrows(NopException.class,
                () -> attendanceBiz.clockIn(empId, CTX));
        assertEquals(ErpHrErrors.ERR_ALREADY_CLOCKED_IN.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testClockOutWithoutClockInBlocked() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-NOIN"));

        NopException ex = assertThrows(NopException.class,
                () -> attendanceBiz.clockOut(empId, CTX));
        assertEquals(ErpHrErrors.ERR_NOT_CLOCKED_IN.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testGetTodayAttendanceReturnsNullWhenAbsent() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-NULL"));

        ErpHrAttendance result = attendanceBiz.getTodayAttendance(empId, CTX);
        assertNull(result, "无打卡记录应返回 null");
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
}
