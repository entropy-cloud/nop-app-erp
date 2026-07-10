package app.erp.hr.service;

import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrLeaveBalance;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 休假审批引擎端到端测试（use-cases.md UC-HR-02）。覆盖：
 * <ul>
 *   <li>场景1：完整审批流程 DRAFT→submit→APPROVED + 排班联动 isAbsent=true。</li>
 *   <li>场景2：余额不足拦截 ERR_LEAVE_BALANCE_INSUFFICIENT。</li>
 *   <li>场景3：日期重叠拦截 ERR_LEAVE_DATE_OVERLAP。</li>
 *   <li>场景4：cancel 回退排班标记 + 余额恢复。</li>
 *   <li>场景5：durationDays 自动计算（startDate~endDate 含首尾）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrLeaveEngine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;
    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;
    @Inject
    IErpHrShiftBiz shiftBiz;

    @Test
    public void testFullApprovalFlowActivatesShiftLinkage() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-FULL");
            Long shiftId = seedShift("MORNING");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("10"));
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];

        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 7, 2), CTX);
        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), ErpHrConstants.LEAVE_STATUS_DRAFT);

        leaveRequestBiz.submit(String.valueOf(leaveId), CTX);
        leaveRequestBiz.approve(String.valueOf(leaveId), CTX);

        ErpHrLeaveRequest refreshed = daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
        assertEquals(ErpHrConstants.LEAVE_STATUS_APPROVED, refreshed.getStatus());

        ErpHrShiftAssignment assignment = findAssignment(empId, LocalDate.of(2026, 7, 2));
        assertNotNull(assignment);
        assertTrue(assignment.getIsAbsent(), "排班应标记为缺席");
        assertEquals(ErpHrConstants.ABSENCE_REASON_LEAVE, assignment.getAbsenceReason());
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT, assignment.getStatus());
    }

    @Test
    public void testInsufficientBalanceBlocksSubmit() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-BALANCE");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("5"));
            // 已用 3 天（7-1~7-3）
            seedLeaveRequest(empId, "ANNUAL", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
                    ErpHrConstants.LEAVE_STATUS_APPROVED);
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];

        // 新申请 4 天（余额仅剩 2 天）
        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 13), ErpHrConstants.LEAVE_STATUS_DRAFT);

        NopException ex = assertThrows(NopException.class,
                () -> leaveRequestBiz.submit(String.valueOf(leaveId), CTX));
        assertEquals(ErpHrErrors.ERR_LEAVE_BALANCE_INSUFFICIENT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testDateOverlapBlocksSubmit() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-OVERLAP");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("30"));
            // 已有 APPROVED 1/1~1/3
            seedLeaveRequest(empId, "ANNUAL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3),
                    ErpHrConstants.LEAVE_STATUS_APPROVED);
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];

        // 新申请 1/2~1/4 与已有重叠
        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 4), ErpHrConstants.LEAVE_STATUS_DRAFT);

        NopException ex = assertThrows(NopException.class,
                () -> leaveRequestBiz.submit(String.valueOf(leaveId), CTX));
        assertEquals(ErpHrErrors.ERR_LEAVE_DATE_OVERLAP.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCancelRevertsShiftLinkage() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CANCEL");
            Long shiftId = seedShift("MORNING");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("10"));
            return new Object[]{empId, shiftId};
        });
        Long empId = (Long) seeded[0];
        Long shiftId = (Long) seeded[1];

        assignmentBiz.assignSingle(empId, shiftId, LocalDate.of(2026, 7, 5), CTX);
        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5), ErpHrConstants.LEAVE_STATUS_DRAFT);

        leaveRequestBiz.submit(String.valueOf(leaveId), CTX);
        leaveRequestBiz.approve(String.valueOf(leaveId), CTX);

        ErpHrShiftAssignment afterApprove = findAssignment(empId, LocalDate.of(2026, 7, 5));
        assertTrue(afterApprove.getIsAbsent(), "审批后应标记缺席");

        leaveRequestBiz.cancel(String.valueOf(leaveId), CTX);

        ErpHrLeaveRequest refreshed = daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
        assertEquals(ErpHrConstants.LEAVE_STATUS_CANCELLED, refreshed.getStatus());

        ErpHrShiftAssignment afterCancel = findAssignment(empId, LocalDate.of(2026, 7, 5));
        assertFalse(afterCancel.getIsAbsent(), "取消后应解除缺席标记");
        assertEquals(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED, afterCancel.getStatus());

        // 余额恢复——getBalance 应回到全额
        BigDecimal remaining = leaveRequestBiz.getBalance(empId, "ANNUAL", 2026, CTX);
        assertEquals(0, remaining.compareTo(new BigDecimal("10")), "取消后余额应恢复为 10");
    }

    @Test
    public void testDurationDaysAutoCalc() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-DURATION");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("30"));
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];

        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), ErpHrConstants.LEAVE_STATUS_DRAFT);

        leaveRequestBiz.submit(String.valueOf(leaveId), CTX);

        ErpHrLeaveRequest refreshed = daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
        assertEquals(0, refreshed.getDurationDays().compareTo(new BigDecimal("5")),
                "1/1~1/5 含首尾应为 5 天");
    }

    @Test
    public void testRejectFromSubmitted() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-REJECT");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("10"));
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];

        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), ErpHrConstants.LEAVE_STATUS_DRAFT);
        leaveRequestBiz.submit(String.valueOf(leaveId), CTX);
        leaveRequestBiz.reject(String.valueOf(leaveId), CTX);

        ErpHrLeaveRequest refreshed = daoProvider.daoFor(ErpHrLeaveRequest.class).getEntityById(leaveId);
        assertEquals(ErpHrConstants.LEAVE_STATUS_REJECTED, refreshed.getStatus());
    }

    @Test
    public void testIllegalTransitionApprovedToSubmit() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-ILLEGAL");
            seedBalance(empId, "ANNUAL", 2026, new BigDecimal("10"));
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];

        Long leaveId = seedLeaveRequest(empId, "ANNUAL",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), ErpHrConstants.LEAVE_STATUS_APPROVED);

        NopException ex = assertThrows(NopException.class,
                () -> leaveRequestBiz.submit(String.valueOf(leaveId), CTX));
        assertEquals(ErpHrErrors.ERR_LEAVE_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    // ---------- helpers ----------

    private ErpHrShiftAssignment findAssignment(Long empId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", empId), eq("assignmentDate", date)));
        q.setLimit(1);
        List<ErpHrShiftAssignment> list = daoProvider.daoFor(ErpHrShiftAssignment.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

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

    private Long seedShift(String code) {
        IEntityDao<ErpHrShift> dao = daoProvider.daoFor(ErpHrShift.class);
        ErpHrShift s = new ErpHrShift();
        s.setCode(code);
        s.setName(code);
        s.setShiftType("FIXED");
        s.setStartTime("08:00");
        s.setEndTime("17:00");
        s.setGraceLateMinutes(15);
        s.setGraceEarlyLeaveMinutes(15);
        s.setRequireClockIn(true);
        s.setRequireClockOut(true);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedBalance(Long employeeId, String leaveType, int fiscalYear, BigDecimal entitledDays) {
        IEntityDao<ErpHrLeaveBalance> dao = daoProvider.daoFor(ErpHrLeaveBalance.class);
        ErpHrLeaveBalance b = new ErpHrLeaveBalance();
        b.setBusinessDate(LocalDate.of(2026, 1, 1));
        b.setEmployeeId(employeeId);
        b.setLeaveType(leaveType);
        b.setFiscalYear(fiscalYear);
        b.setEntitledDays(entitledDays);
        b.setCarriedForwardDays(BigDecimal.ZERO);
        dao.saveEntity(b);
    }

    private Long seedLeaveRequest(Long employeeId, String leaveType,
                                  LocalDate startDate, LocalDate endDate, String status) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(LocalDate.of(2026, 1, 1));
        l.setCode("LV-" + employeeId + "-" + startDate.toString() + "-" + System.nanoTime());
        l.setEmployeeId(employeeId);
        l.setLeaveType(leaveType);
        l.setStartDate(startDate);
        l.setEndDate(endDate);
        l.setStatus(status);
        if (ErpHrConstants.LEAVE_STATUS_APPROVED.equals(status)) {
            l.setDurationDays(BigDecimal.valueOf(
                    java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1));
        }
        dao.saveEntity(l);
        return l.getId();
    }
}
