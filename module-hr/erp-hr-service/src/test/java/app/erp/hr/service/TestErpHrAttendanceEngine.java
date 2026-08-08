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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考勤打卡端点端到端测试（use-cases.md UC-HR-06）。覆盖：
 * <ul>
 *   <li>场景1：clockIn → clockOut → workHours 正确计算。</li>
 *   <li>场景2：重复打卡以最后一次为准（last-wins 覆盖 clockIn + clockOut 已存在时重算 workHours）。</li>
 *   <li>场景3：未签到签退拦截 ERR_NOT_CLOCKED_IN。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrAttendanceEngine extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

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

        ErpHrAttendance afterIn = ormTemplate.runInSession(session -> attendanceBiz.clockIn(empId, CTX));
        assertNotNull(afterIn.getClockIn(), "签到后 clockIn 应有值");
        assertNull(afterIn.getClockOut(), "签到后 clockOut 应为空");

        ErpHrAttendance afterOut = ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX));
        assertNotNull(afterOut.getClockOut(), "签退后 clockOut 应有值");
        assertNotNull(afterOut.getWorkHours(), "签退后 workHours 应已计算");

        ErpHrAttendance today = ormTemplate.runInSession(session -> attendanceBiz.getTodayAttendance(empId, CTX));
        assertNotNull(today);
        assertEquals(today.getClockIn(), afterIn.getClockIn());
    }

    @Test
    public void testDuplicateClockInLastWins() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-DUP"));

        ErpHrAttendance first = ormTemplate.runInSession(session -> attendanceBiz.clockIn(empId, CTX));
        ErpHrAttendance second = ormTemplate.runInSession(session -> attendanceBiz.clockIn(empId, CTX));

        assertNotNull(second.getClockIn(), "重复签到后 clockIn 应有值（last-wins 覆盖，不抛异常）");
        assertTrue(second.getClockIn().after(first.getClockIn()), "last-wins 应覆盖为后值（后值 > 前值）");

        ErpHrAttendance today = ormTemplate.runInSession(session -> attendanceBiz.getTodayAttendance(empId, CTX));
        assertNotNull(today, "当日考勤记录应存在");
        assertEquals(today.getClockIn(), second.getClockIn(), "持久化 clockIn 应为第二次签到值");
    }

    @Test
    public void testDuplicateClockInRecomputesWorkHours() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-WH"));

        // 确定性构造：DAO 直接 seed 旧 clockIn（过去 4 小时）+ 显式未来 clockOut（不经 clockIn/clockOut
        // mutation）；镜像公式断言与实现 computeWorkHours 逐值一致（Duration.toMinutes 截断 + /60 HALF_UP），
        // 避免「同分钟毫秒截断下两次 workHours 同值」的竞态（frozen clock 无时间推进 API）
        LocalDateTime oldClockIn = LocalDateTime.now().minusHours(4);
        LocalDateTime seededClockOut = LocalDateTime.now().plusHours(3);
        ormTemplate.runInSession(session -> seedAttendance(empId, oldClockIn, seededClockOut));

        ErpHrAttendance after = ormTemplate.runInSession(session -> attendanceBiz.clockIn(empId, CTX));

        assertNotNull(after.getClockIn(), "last-wins 覆盖后 clockIn 应有值");
        assertNotNull(after.getClockOut(), "既有 clockOut 应保留");
        assertNotNull(after.getWorkHours(), "重复签到且 clockOut 已存在时应重算 workHours");

        BigDecimal expected = BigDecimal.valueOf(
                        Duration.between(after.getClockIn().toLocalDateTime(), seededClockOut).toMinutes())
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        assertEquals(expected, after.getWorkHours(), "workHours 应按新 clockIn 与 seed clockOut 镜像重算");

        BigDecimal preValue = BigDecimal.valueOf(
                        Duration.between(oldClockIn, seededClockOut).toMinutes())
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        assertNotEquals(preValue, after.getWorkHours(), "覆盖后 workHours 应与覆盖前（旧 clockIn 计算值）不同");
    }

    @Test
    public void testClockOutWithoutClockInBlocked() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-NOIN"));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX)));
        assertEquals(ErpHrErrors.ERR_NOT_CLOCKED_IN.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testGetTodayAttendanceReturnsNullWhenAbsent() {
        Long empId = ormTemplate.runInSession(session -> seedEmployee("EMP-NULL"));

        ErpHrAttendance result = ormTemplate.runInSession(session -> attendanceBiz.getTodayAttendance(empId, CTX));
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

    private ErpHrAttendance seedAttendance(Long employeeId, LocalDateTime clockIn, LocalDateTime clockOut) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        ErpHrAttendance row = new ErpHrAttendance();
        row.setBusinessDate(HrFrozenClockExtension.REFERENCE_DATE);
        row.setEmployeeId(employeeId);
        row.setDate(HrFrozenClockExtension.REFERENCE_DATE);
        row.setSource(ErpHrConstants.ATTENDANCE_SOURCE_CARD);
        row.setIsAbsent(false);
        row.setLateMinutes(0);
        row.setEarlyLeaveMinutes(0);
        row.setClockIn(Timestamp.valueOf(clockIn));
        row.setClockOut(Timestamp.valueOf(clockOut));
        dao.saveEntity(row);
        return row;
    }
}
