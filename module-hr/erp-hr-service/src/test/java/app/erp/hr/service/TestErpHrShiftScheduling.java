package app.erp.hr.service;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.biz.IErpHrShiftRotationPatternBiz;
import app.erp.hr.biz.IErpHrShiftSwapRequestBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftRotationPattern;
import app.erp.hr.dao.entity.ErpHrShiftSwapRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排班管理端到端行为测试（shift-scheduling.md §2.2/§3.3/§4.1/§4.2/§5/§6）。覆盖：
 * <ul>
 *   <li>一人一天一排班唯一约束（ERR_SHIFT_DUPLICATE_ASSIGNMENT）。</li>
 *   <li>批量分配、复制上期、轮换生成（含 staggerDays 错峰）。</li>
 *   <li>迟到/早退/缺勤计算 + 跨天夜班 clockOut 次日基准。</li>
 *   <li>调换审批 APPROVED 双方 assignment 互换且可追溯。</li>
 *   <li>休假联动 APPROVED 标记缺席、CANCELLED 解除。</li>
 *   <li>轮换 pattern 非法序列抛 ERR_SHIFT_ROTATION_PATTERN_INVALID。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrShiftScheduling extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrShiftBiz shiftBiz;
    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;
    @Inject
    IErpHrShiftRotationPatternBiz rotationBiz;
    @Inject
    IErpHrShiftSwapRequestBiz swapBiz;

    @Test
    public void testAssignSingleEnforcesOneEmployeeOneDayOneShift() {
        Long[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-UNIQUE");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Long[]{empId, shiftId};
        });
        Long empId = ids[0];
        Long shiftId = ids[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        ErpHrShiftAssignment a1 = assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        assertNotNull(a1.getId());
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED, a1.getStatus());

        // 同员工同日再次分配 → 冲突
        NopException ex = assertThrows(NopException.class,
                () -> assignmentBiz.assignSingle(empId, shiftId, date, CTX));
        assertEquals(ErpHrErrors.ERR_SHIFT_DUPLICATE_ASSIGNMENT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testAssignBatchCreatesForEveryEmployeeEveryDay() {
        Long[] ids = ormTemplate.runInSession(session -> {
            Long empA = seedEmployee("EMP-BATCH-A");
            Long empB = seedEmployee("EMP-BATCH-B");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Long[]{empA, empB, shiftId};
        });
        Long empA = ids[0];
        Long empB = ids[1];
        Long shiftId = ids[2];

        List<ErpHrShiftAssignment> created = assignmentBiz.assignBatch(
                Arrays.asList(empA, empB), shiftId,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), CTX);
        // 2 员工 × 3 天 = 6 条
        assertEquals(6, created.size());
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empA, LocalDate.of(2026, 7, 2), CTX));
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empB, LocalDate.of(2026, 7, 3), CTX));
    }

    @Test
    public void testCopyFromPeriodAlignsByDayOffset() {
        Long[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-COPY");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Long[]{empId, shiftId};
        });
        Long empId = ids[0];
        Long shiftId = ids[1];

        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 6, 1), CTX);
        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 6, 2), CTX);

        List<ErpHrShiftAssignment> copied = assignmentBiz.copyFromPeriod(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 7, 1), CTX);
        assertEquals(2, copied.size());
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empId, LocalDate.of(2026, 7, 1), CTX));
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empId, LocalDate.of(2026, 7, 2), CTX));
    }

    @Test
    public void testRotationGenerateStaggerAndRegenerate() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empA = seedEmployee("EMP-ROT-A");
            Long empB = seedEmployee("EMP-ROT-B");
            Long morningId = seedShift("MORNING", "08:00", "17:00", true);
            Long afternoonId = seedShift("AFTERNOON", "14:00", "23:00", true);
            Long patternId = seedRotationPattern("ROT-2CYCLE",
                    "[\"MORNING\",\"AFTERNOON\"]");
            return new Object[]{empA, empB, morningId, afternoonId, patternId};
        });
        Long empA = (Long) seeded[0];
        Long empB = (Long) seeded[1];
        Long patternId = (Long) seeded[4];

        // 2 成员 × staggerDays=1 × 范围 2026-07-01~2026-07-04
        List<ErpHrShiftAssignment> generated = rotationBiz.generateRotation(
                patternId, Arrays.asList(empA, empB), 1,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4),
                false, CTX);
        assertFalse(generated.isEmpty(), "轮换应生成至少 1 条排班");
        // empA 起始日有班
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empA, LocalDate.of(2026, 7, 1), CTX));
        // empB 错峰 1 天 → 7-1 无班，7-2 有班
        assertNull(assignmentBiz.findByEmployeeAndDate(empB, LocalDate.of(2026, 7, 1), CTX));
        assertNotNull(assignmentBiz.findByEmployeeAndDate(empB, LocalDate.of(2026, 7, 2), CTX));

        // regenerate 同范围：旧的 SCHEDULELLED 应被取消，重新生成
        long countBefore = countAssignments(empA, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));
        List<ErpHrShiftAssignment> regenerated = rotationBiz.generateRotation(patternId, Arrays.asList(empA, empB), 1,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4),
                true, CTX);
        // regenerate 不增加有效排班数（同范围同成员）
        long countAfter = countAssignments(empA, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));
        assertEquals(countBefore, countAfter,
                "regenerate 应清旧重生成，有效排班数不变 (regenerated=" + regenerated.size()
                        + ", before=" + countBefore + ", after=" + countAfter + ")");
    }

    @Test
    public void testRotationInvalidPatternThrows() {
        Long empId = ormTemplate.runInSession(session ->
                seedEmployee("EMP-INVALID-ROT"));
        Long patternId = ormTemplate.runInSession(session ->
                seedRotationPattern("ROT-EMPTY", "[]"));
        NopException ex = assertThrows(NopException.class, () ->
                rotationBiz.generateRotation(patternId, Arrays.asList(empId), 0,
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2),
                        false, CTX));
        assertEquals(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCalcAttendanceOnTimeNoLateNoEarlyLeave() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-ONTIME");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        seedAttendance(empId, date,
                LocalDateTime.of(2026, 7, 1, 8, 5),
                LocalDateTime.of(2026, 7, 1, 17, 5));

        ErpHrAttendance result = shiftBiz.calcAttendance(empId, date, CTX);
        assertNotNull(result);
        assertEquals(0, result.getLateMinutes().intValue(), "8:05 在 grace 15 内不算迟到");
        assertEquals(0, result.getEarlyLeaveMinutes().intValue(), "17:05 晚于 17:00 不算早退");
        assertFalse(result.getIsAbsent());
    }

    @Test
    public void testCalcAttendanceLateAndEarlyLeave() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LATE");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        // 08:30 签到（迟到 30 分钟，超出 grace 15 → lateMinutes=30）
        // 16:00 签退（早退 60 分钟，超出 grace 15 → earlyLeaveMinutes=60）
        seedAttendance(empId, date,
                LocalDateTime.of(2026, 7, 1, 8, 30),
                LocalDateTime.of(2026, 7, 1, 16, 0));

        ErpHrAttendance result = shiftBiz.calcAttendance(empId, date, CTX);
        assertEquals(30, result.getLateMinutes().intValue(), "08:30 - 08:00 = 30 分钟迟到");
        assertEquals(60, result.getEarlyLeaveMinutes().intValue(), "17:00 - 16:00 = 60 分钟早退");
    }

    @Test
    public void testCalcAttendanceCrossDayNightShift() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-NIGHT");
            // 夜班 23:00→08:00 跨天
            Long shiftId = seedShift("NIGHT", "23:00", "08:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        // 签到 23:00 当日（准时）+ 签退 08:00 次日（准时）
        seedAttendance(empId, date,
                LocalDateTime.of(2026, 7, 1, 23, 0),
                LocalDateTime.of(2026, 7, 2, 8, 0));

        ErpHrAttendance result = shiftBiz.calcAttendance(empId, date, CTX);
        assertNotNull(result);
        assertEquals(0, result.getLateMinutes().intValue(), "23:00 准时");
        assertEquals(0, result.getEarlyLeaveMinutes().intValue(), "次日 08:00 准时（跨天基准）");
    }

    @Test
    public void testCalcAttendanceCrossDayNightShiftEarlyLeave() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-NIGHT-EARLY");
            Long shiftId = seedShift("NIGHT", "23:00", "08:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        // 签退 06:00 次日 → 早退 120 分钟（08:00 - 06:00，超 grace 15）
        seedAttendance(empId, date,
                LocalDateTime.of(2026, 7, 1, 23, 0),
                LocalDateTime.of(2026, 7, 2, 6, 0));

        ErpHrAttendance result = shiftBiz.calcAttendance(empId, date, CTX);
        assertEquals(120, result.getEarlyLeaveMinutes().intValue(), "次日 08:00 - 06:00 = 120 分钟早退");
    }

    @Test
    public void testCalcAttendanceAbsentByNoClockIn() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-ABSENT");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];
        LocalDate date = LocalDate.of(2026, 7, 1);

        ErpHrShiftAssignment assignment = assignmentBiz.assignSingle(empId, shiftId, date, CTX);
        // 无 attendance 记录 → 缺勤
        ErpHrAttendance result = shiftBiz.calcAttendance(empId, date, CTX);
        assertNotNull(result);
        assertTrue(result.getIsAbsent(), "requireClockIn=true 且无打卡 → 缺勤");

        // assignment 应已置为 ABSENT（calcAttendance 调 updateAssignmentStatus）
        ErpHrShiftAssignment updated = daoProvider.daoFor(ErpHrShiftAssignment.class)
                .getEntityById(assignment.getId());
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT, updated.getStatus());
        assertTrue(updated.getIsAbsent());
        assertEquals(ErpHrConstants.ABSENCE_REASON_LATE_NOT_CLOCKED, updated.getAbsenceReason());
    }

    @Test
    public void testSwapApproveSwapsShiftsBetweenAssignments() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empA = seedEmployee("EMP-SWAP-A");
            Long empB = seedEmployee("EMP-SWAP-B");
            Long morningId = seedShift("MORNING", "08:00", "17:00", true);
            Long afternoonId = seedShift("AFTERNOON", "14:00", "23:00", true);
            return new Object[]{empA, empB, morningId, afternoonId};
        });
        Long empA = (Long) seeded[0];
        Long empB = (Long) seeded[1];
        Long morningId = (Long) seeded[2];
        Long afternoonId = (Long) seeded[3];
        LocalDate date = LocalDate.of(2026, 7, 1);

        ErpHrShiftAssignment src = assignmentBiz.assignSingle(empA, morningId, date, CTX);
        ErpHrShiftAssignment tgt = assignmentBiz.assignSingle(empB, afternoonId, date, CTX);

        ErpHrShiftSwapRequest req = swapBiz.submit(src.getId(), tgt.getId(), "测试调换", CTX);
        assertEquals(ErpHrConstants.SWAP_STATUS_PENDING, req.getStatus());

        ErpHrShiftSwapRequest approved = swapBiz.approve(req.getId(), CTX);
        assertEquals(ErpHrConstants.SWAP_STATUS_APPROVED, approved.getStatus());

        // 双方班次应已互换
        ErpHrShiftAssignment srcAfter = daoProvider.daoFor(ErpHrShiftAssignment.class).getEntityById(src.getId());
        ErpHrShiftAssignment tgtAfter = daoProvider.daoFor(ErpHrShiftAssignment.class).getEntityById(tgt.getId());
        assertEquals(afternoonId, srcAfter.getShiftId(), "源 assignment 现在上中班");
        assertEquals(morningId, tgtAfter.getShiftId(), "目标 assignment 现在上早班");
        assertEquals(approved.getId(), srcAfter.getSwapRequestId());
        assertEquals(approved.getId(), tgtAfter.getSwapRequestId());
    }

    @Test
    public void testSwapIllegalTransitionRejects() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empA = seedEmployee("EMP-SWAP-ILLEGAL-A");
            Long empB = seedEmployee("EMP-SWAP-ILLEGAL-B");
            Long morningId = seedShift("MORNING-X", "08:00", "17:00", true);
            Long afternoonId = seedShift("AFTERNOON-X", "14:00", "23:00", true);
            return new Object[]{empA, empB, morningId, afternoonId};
        });
        Long empA = (Long) seeded[0];
        Long empB = (Long) seeded[1];
        Long morningId = (Long) seeded[2];
        Long afternoonId = (Long) seeded[3];
        LocalDate date = LocalDate.of(2026, 7, 1);

        ErpHrShiftAssignment src = assignmentBiz.assignSingle(empA, morningId, date, CTX);
        ErpHrShiftAssignment tgt = assignmentBiz.assignSingle(empB, afternoonId, date, CTX);
        ErpHrShiftSwapRequest req = swapBiz.submit(src.getId(), tgt.getId(), "测试", CTX);
        swapBiz.reject(req.getId(), CTX);
        // REJECTED 后再 approve → 非法
        NopException ex = assertThrows(NopException.class,
                () -> swapBiz.approve(req.getId(), CTX));
        assertEquals(ErpHrErrors.ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testLeaveApprovedMarksAbsentAndCancelReverts() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LEAVE");
            Long shiftId = seedShift("MORNING", "08:00", "17:00", true);
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];

        // 排班 7-1 ~ 7-3
        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 7, 1), CTX);
        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 7, 2), CTX);
        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 7, 3), CTX);
        // 休假 7-2 ~ 7-3
        Long leaveId = seedLeaveRequest(empId, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3));

        shiftBiz.onLeaveApproved(leaveId, CTX);

        // 7-1 仍 SCHEDULED，7-2/7-3 ABSENT
        ErpHrShiftAssignment a1 = daoProvider.daoFor(ErpHrShiftAssignment.class)
                .findAllByQuery(buildEmployeeDateQuery(empId, LocalDate.of(2026, 7, 1))).get(0);
        ErpHrShiftAssignment a2 = daoProvider.daoFor(ErpHrShiftAssignment.class)
                .findAllByQuery(buildEmployeeDateQuery(empId, LocalDate.of(2026, 7, 2))).get(0);
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED, a1.getStatus());
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT, a2.getStatus());
        assertTrue(a2.getIsAbsent());
        assertEquals(ErpHrConstants.ABSENCE_REASON_LEAVE, a2.getAbsenceReason());
        assertEquals(leaveId, a2.getLeaveRequestId());

        // 取消休假 → 解除标记
        shiftBiz.onLeaveCancelled(leaveId, CTX);
        ErpHrShiftAssignment a2After = daoProvider.daoFor(ErpHrShiftAssignment.class)
                .findAllByQuery(buildEmployeeDateQuery(empId, LocalDate.of(2026, 7, 2))).get(0);
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED, a2After.getStatus());
        assertFalse(a2After.getIsAbsent());
        assertNull(a2After.getLeaveRequestId());
    }

    // ---------- helpers ----------

    private io.nop.api.core.beans.query.QueryBean buildEmployeeDateQuery(Long empId, LocalDate date) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.and(
                io.nop.api.core.beans.FilterBeans.eq("employeeId", empId),
                io.nop.api.core.beans.FilterBeans.eq("assignmentDate", date)));
        return q;
    }

    private long countAssignments(Long empId, LocalDate from, LocalDate to) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.and(
                io.nop.api.core.beans.FilterBeans.eq("employeeId", empId),
                io.nop.api.core.beans.FilterBeans.dateBetween("assignmentDate", from, to),
                io.nop.api.core.beans.FilterBeans.in("status", Arrays.asList(
                        ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED,
                        ErpHrConstants.ASSIGNMENT_STATUS_PRESENT,
                        ErpHrConstants.ASSIGNMENT_STATUS_ABSENT))));
        return assignmentBiz.findCount(q, CTX);
    }

    private Long seedEmployee(String code) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName("测试员工");
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedShift(String code, String startTime, String endTime, boolean requireClockIn) {
        IEntityDao<ErpHrShift> dao = daoProvider.daoFor(ErpHrShift.class);
        ErpHrShift s = new ErpHrShift();
        s.setCode(code);
        s.setName(code);
        s.setShiftType("FIXED");
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setGraceLateMinutes(15);
        s.setGraceEarlyLeaveMinutes(15);
        s.setRequireClockIn(requireClockIn);
        s.setRequireClockOut(true);
        dao.saveEntity(s);
        return s.getId();
    }

    private Long seedRotationPattern(String code, String patternData) {
        IEntityDao<ErpHrShiftRotationPattern> dao = daoProvider.daoFor(ErpHrShiftRotationPattern.class);
        ErpHrShiftRotationPattern p = new ErpHrShiftRotationPattern();
        p.setCode(code);
        p.setName(code);
        p.setPatternType(ErpHrConstants.PATTERN_TYPE_CYCLE_DAYS);
        p.setPatternData(patternData);
        p.setRotateInterval(1);
        p.setStartDate(LocalDate.of(2026, 7, 1));
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedAttendance(Long employeeId, LocalDate date,
                                LocalDateTime clockIn, LocalDateTime clockOut) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        ErpHrAttendance a = new ErpHrAttendance();
        a.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        a.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        a.setEmployeeId(employeeId);
        a.setDate(date);
        a.setClockIn(clockIn);
        a.setClockOut(clockOut);
        a.setIsAbsent(false);
        a.setLateMinutes(0);
        a.setEarlyLeaveMinutes(0);
        dao.saveEntity(a);
    }

    private Long seedLeaveRequest(Long employeeId, LocalDate startDate, LocalDate endDate) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setCode("LV-" + employeeId + "-" + startDate.toString());
        l.setEmployeeId(employeeId);
        l.setLeaveType("ANNUAL");
        l.setStartDate(startDate);
        l.setEndDate(endDate);
        l.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        dao.saveEntity(l);
        return l.getId();
    }
}
