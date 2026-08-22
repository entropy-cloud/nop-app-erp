package app.erp.hr.service;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 夜班跨天 clockOut 回退测试（use-cases.md UC-HR-06⑭ + shift-scheduling.md §4.2/§九.6，RC-R1.6 P1-RC-013）。
 * 覆盖：
 * <ul>
 *   <li>① 跨天命中：今日无记录 + 昨日跨天排班 + 昨日记录已签到 → 回退对昨日记录执行 clockOut。</li>
 *   <li>② 今日记录优先：今日已签到 → 签退今日记录，不回退不触碰昨日记录。</li>
 *   <li>③ 昨日无跨天排班：昨日记录已签到但无排班 → 抛 ERR_NOT_CLOCKED_IN。</li>
 *   <li>④ 昨日记录未签到：跨天排班存在但昨日 clockIn 空 → 抛 ERR_NOT_CLOCKED_IN。</li>
 *   <li>⑤ 常规路径回归：无昨日数据时行为不变（今日签退成功 / 无任何记录仍拒绝）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrAttendanceCrossDayClockOut extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrAttendanceBiz attendanceBiz;
    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    @Test
    public void testCrossDayClockOutHitsYesterdayNightShift() {
        LocalDate yesterday = HrFrozenClockExtension.REFERENCE_DATE.minusDays(1);
        String empId = ormTemplate.runInSession(session -> {
            String emp = seedEmployee("EMP-CROSS-HIT");
            String shiftId = seedNightShift();
            assignmentBiz.assignSingle(emp, shiftId, yesterday, CTX);
            seedAttendance(emp, yesterday, LocalDateTime.now().minusHours(9), null);
            return emp;
        });

        ErpHrAttendance after = ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX));

        assertEquals(yesterday, after.getDate(), "跨天签退应作用于昨日记录（date=yesterday）");
        assertNotNull(after.getClockOut(), "昨日记录 clockOut 应落值");
        // 冻结时钟仅冻结日期（CoreMetrics.today），clockOut 写真实系统时刻（CoreMetrics.currentTimestamp）：
        // clockIn=now-9h（seed 时刻）→ clockOut=now（clockOut 时刻，恒晚于 seed 时刻）→ Duration ≥ 9h → workHours ≥ 9.00 数学恒成立。
        // 精确值断言用实现公式镜像（toMinutes 截断 + /60 HALF_UP），鲁棒不依赖具体时刻。
        assertTrue(after.getWorkHours().compareTo(new BigDecimal("9.00")) >= 0,
                "clockIn=now-9h 下 workHours 应 ≥ 9.00，实际=" + after.getWorkHours());
        BigDecimal mirror = BigDecimal.valueOf(
                        Duration.between(after.getClockIn().toLocalDateTime(), after.getClockOut().toLocalDateTime()).toMinutes())
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        assertEquals(mirror, after.getWorkHours(), "workHours 应等于实现公式镜像值（clockIn→clockOut 分钟/60 HALF_UP）");
    }

    @Test
    public void testTodayRecordTakesPriorityOverYesterdayFallback() {
        LocalDate today = HrFrozenClockExtension.REFERENCE_DATE;
        LocalDate yesterday = today.minusDays(1);
        String empId = ormTemplate.runInSession(session -> {
            String emp = seedEmployee("EMP-CROSS-PRIO");
            String shiftId = seedNightShift();
            assignmentBiz.assignSingle(emp, shiftId, yesterday, CTX);
            seedAttendance(emp, yesterday, LocalDateTime.now().minusHours(20), null);
            seedAttendance(emp, today, LocalDateTime.now().minusHours(3), null);
            return emp;
        });

        ErpHrAttendance after = ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX));

        assertEquals(today, after.getDate(), "今日记录已签到应签退今日记录（回退仅在 today 无可用记录时触发）");
        assertNotNull(after.getClockOut(), "今日记录 clockOut 应落值");
        ErpHrAttendance y = findAttendanceByDate(empId, yesterday);
        assertNull(y.getClockOut(), "今日记录优先时不应触碰昨日记录");
    }

    @Test
    public void testCrossDayClockOutRejectedWithoutYesterdayShiftAssignment() {
        LocalDate yesterday = HrFrozenClockExtension.REFERENCE_DATE.minusDays(1);
        String empId = ormTemplate.runInSession(session -> {
            String emp = seedEmployee("EMP-CROSS-NOSHIFT");
            seedAttendance(emp, yesterday, LocalDateTime.now().minusHours(9), null);
            return emp;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX)));
        assertEquals(ErpHrErrors.ERR_NOT_CLOCKED_IN.getErrorCode(), ex.getErrorCode(),
                "昨日记录无跨天排班 → 回退不命中，维持 ERR_NOT_CLOCKED_IN");
    }

    @Test
    public void testCrossDayClockOutRejectedWhenYesterdayClockInNull() {
        LocalDate yesterday = HrFrozenClockExtension.REFERENCE_DATE.minusDays(1);
        String empId = ormTemplate.runInSession(session -> {
            String emp = seedEmployee("EMP-CROSS-NOIN");
            String shiftId = seedNightShift();
            assignmentBiz.assignSingle(emp, shiftId, yesterday, CTX);
            seedAttendance(emp, yesterday, null, null);
            return emp;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX)));
        assertEquals(ErpHrErrors.ERR_NOT_CLOCKED_IN.getErrorCode(), ex.getErrorCode(),
                "昨日记录 clockIn 为空 → 回退不命中，维持 ERR_NOT_CLOCKED_IN");
    }

    @Test
    public void testNormalClockOutUnchangedWithoutCrossDayData() {
        LocalDate today = HrFrozenClockExtension.REFERENCE_DATE;
        String empId = ormTemplate.runInSession(session -> {
            String emp = seedEmployee("EMP-CROSS-NORMAL");
            seedAttendance(emp, today, LocalDateTime.now().minusHours(2), null);
            return emp;
        });

        ErpHrAttendance after = ormTemplate.runInSession(session -> attendanceBiz.clockOut(empId, CTX));
        assertEquals(today, after.getDate(), "常规路径：签退今日记录");
        assertNotNull(after.getClockOut(), "常规路径：clockOut 落值");

        // 无任何记录（今日 + 昨日均无）→ 行为不变：拒绝
        String freshEmp = ormTemplate.runInSession(session -> seedEmployee("EMP-CROSS-EMPTY"));
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> attendanceBiz.clockOut(freshEmp, CTX)));
        assertEquals(ErpHrErrors.ERR_NOT_CLOCKED_IN.getErrorCode(), ex.getErrorCode());
    }

    // ---------- helpers ----------

    private String seedNightShift() {
        IEntityDao<ErpHrShift> dao = daoProvider.daoFor(ErpHrShift.class);
        ErpHrShift s = new ErpHrShift();
        s.setCode("NIGHT");
        s.setName("NIGHT");
        s.setShiftType("FIXED");
        s.setStartTime("23:00");
        s.setEndTime("08:00");
        s.setGraceLateMinutes(15);
        s.setGraceEarlyLeaveMinutes(15);
        s.setRequireClockIn(true);
        s.setRequireClockOut(true);
        dao.saveEntity(s);
        return s.getId();
    }

    private String seedEmployee(String code) {
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

    private void seedAttendance(String employeeId, LocalDate date, LocalDateTime clockIn, LocalDateTime clockOut) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        ErpHrAttendance row = new ErpHrAttendance();
        row.setBusinessDate(HrFrozenClockExtension.REFERENCE_DATE);
        row.setEmployeeId(employeeId);
        row.setDate(date);
        row.setSource(ErpHrConstants.ATTENDANCE_SOURCE_CARD);
        row.setIsAbsent(false);
        row.setLateMinutes(0);
        row.setEarlyLeaveMinutes(0);
        row.setClockIn(clockIn != null ? Timestamp.valueOf(clockIn) : null);
        row.setClockOut(clockOut != null ? Timestamp.valueOf(clockOut) : null);
        dao.saveEntity(row);
    }

    private ErpHrAttendance findAttendanceByDate(String employeeId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        List<ErpHrAttendance> list = daoProvider.daoFor(ErpHrAttendance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
