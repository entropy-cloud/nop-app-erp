import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  findFirst,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * hr ErpHrLeaveRequest.approve/cancel → ErpHrShiftAssignment 跨实体钩子联动
 * 浏览器层 E2E（plan 2026-07-18-0347-2）。
 *
 * 验证 2 条 DIRECT 路径经 GraphQL /graphql 的全栈可达性：
 *
 *   LeaveRequest.approve → 内部委派 IErpHrShiftBiz.onLeaveApproved → 区间内
 *     ShiftAssignment 行字段翻转：isAbsent=true / absenceReason=LEAVE /
 *     leaveRequestId=leave.id / status=ABSENT
 *   LeaveRequest.cancel  → 内部委派 IErpHrShiftBiz.onLeaveCancelled → 仅解除
 *     由该 leaveRequestId 标记的行：isAbsent=false / absenceReason=null /
 *     leaveRequestId=null / status=SCHEDULED
 *
 * 区间范围准确性：onLeaveApproved 经 findAssignmentsByEmployeeRange 仅按 employeeId
 * + dateBetween(assignmentDate, startDate, endDate) 过滤（无 status 过滤——区间内
 * 行不论 SCHEDULED/CANCELLED/ABSENT/PRESENT 均被选中），区间外 assignment 行不被
 * 标记。本 spec setup 时显式将 3 个测试 assignment 全部置 SCHEDULED 态（不引入
 * ABSENT/CANCELLED 负路径）以避免被钩子的"全选"行为干扰主路径断言。
 *
 * GraphQL String/Long 类型桥：assignment.leaveRequestId GraphQL 返回 String，
 * 断言比较须 Number() coercion（同 hr-shift-assignment.action.spec.ts:147-148 范式）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 * 自包含 setup：建 employee + shift + 3 ShiftAssignment（区间内 2 + 区间外 1）+
 * LeaveRequest(DRAFT)；finally 兜底删 LeaveRequest + ShiftAssignment + Shift + Employee
 * （钩子仅写 ShiftAssignment，不写 Attendance——calcAttendance 为独立 mutation 不由
 * approve/cancel 链触发，无需 Attendance cleanup）。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createEmployee(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-LK-EMP-${tag}`),
      firstName: '联',
      lastName: tag,
      fullName: `联动${tag}`,
      gender: 'MALE',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      orgId: 2,
    },
    'id',
  );
}

async function createShift(
  page: import('@playwright/test').Page,
  code: string,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrShift',
    {
      code,
      name: `E2E 联动班次 ${code}`,
      shiftType: 'FIXED',
      startTime: '09:00',
      endTime: '17:00',
      graceLateMinutes: 15,
      graceEarlyLeaveMinutes: 15,
      requireClockIn: true,
      requireClockOut: true,
      orgId: 2,
    },
    'id',
  );
}

async function createLeave(
  page: import('@playwright/test').Page,
  employeeId: string | number,
  tag: string,
  startDate: string,
  endDate: string,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrLeaveRequest',
    {
      code: uniq(`E2E-LK-LV-${tag}`),
      employeeId,
      leaveType: 'ANNUAL',
      startDate,
      endDate,
      status: 'DRAFT',
      orgId: 2,
    },
    'id',
  );
}

async function assignSingle(
  page: import('@playwright/test').Page,
  employeeId: string | number,
  shiftId: string | number,
  assignmentDate: string,
): Promise<{ id: string }> {
  return callMutationOk(
    page,
    'ErpHrShiftAssignment',
    'assignSingle',
    {
      employeeId: Number(employeeId),
      shiftId: Number(shiftId),
      assignmentDate,
    },
    'id',
  );
}

async function findAssignment(
  page: import('@playwright/test').Page,
  employeeId: string | number,
  assignmentDate: string,
): Promise<any | null> {
  return findFirst(
    page,
    'ErpHrShiftAssignment',
    andFilter(
      eqFilter('employeeId', Number(employeeId)),
      eqFilter('assignmentDate', assignmentDate),
    ),
    'id isAbsent absenceReason leaveRequestId status',
  );
}

async function cleanupAll(
  page: import('@playwright/test').Page,
  employeeId: string | number,
  shiftId: string | number,
): Promise<void> {
  await deleteByFilter(page, 'ErpHrLeaveRequest', eqFilter('employeeId', Number(employeeId)));
  await deleteByFilter(page, 'ErpHrShiftAssignment', eqFilter('employeeId', Number(employeeId)));
  await deleteById(page, 'ErpHrShift', shiftId);
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

test.describe('hr ErpHrLeaveRequest → ErpHrShiftAssignment cross-entity hook (plan 0347-2)', () => {
  test('approve marks in-range assignments ABSENT + leaves out-of-range unchanged', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');

    const emp = await createEmployee(page, 'apv');
    const shift = await createShift(page, uniq('E2E-LK-SHIFT-apv'));

    try {
      // setup 3 SCHEDULED assignments: 2026-08-10 / 2026-08-11 (in-range) + 2026-08-13 (out-of-range)
      await assignSingle(page, emp.id, shift.id, '2026-08-10');
      await assignSingle(page, emp.id, shift.id, '2026-08-11');
      await assignSingle(page, emp.id, shift.id, '2026-08-13');

      // LeaveRequest 区间 2026-08-10 ~ 2026-08-12（区间内仅含 08-10/08-11；08-13 在区间外）
      const leave = await createLeave(page, emp.id, 'apv', '2026-08-10', '2026-08-12');

      // submit: DRAFT → SUBMITTED
      await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id: leave.id }, 'id');
      let st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
      expect(st.status, 'after submit status=SUBMITTED').toBe('SUBMITTED');

      // approve: SUBMITTED → APPROVED（内部委派 onLeaveApproved 钩子）
      await callMutationOk(page, 'ErpHrLeaveRequest', 'approve', { id: leave.id }, 'id');
      st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
      expect(st.status, 'after approve status=APPROVED').toBe('APPROVED');

      // 区间内 2026-08-10 行：4 字段翻转
      const a1 = await findAssignment(page, emp.id, '2026-08-10');
      expect(a1, 'in-range 08-10 assignment exists').not.toBeNull();
      expect(a1.isAbsent, '08-10 isAbsent=true').toBe(true);
      expect(a1.absenceReason, '08-10 absenceReason=LEAVE').toBe('LEAVE');
      expect(Number(a1.leaveRequestId), '08-10 leaveRequestId=leave.id').toBe(Number(leave.id));
      expect(a1.status, '08-10 status=ABSENT').toBe('ABSENT');

      // 区间内 2026-08-11 行：4 字段翻转
      const a2 = await findAssignment(page, emp.id, '2026-08-11');
      expect(a2, 'in-range 08-11 assignment exists').not.toBeNull();
      expect(a2.isAbsent, '08-11 isAbsent=true').toBe(true);
      expect(a2.absenceReason, '08-11 absenceReason=LEAVE').toBe('LEAVE');
      expect(Number(a2.leaveRequestId), '08-11 leaveRequestId=leave.id').toBe(Number(leave.id));
      expect(a2.status, '08-11 status=ABSENT').toBe('ABSENT');

      // 区间外 2026-08-13 行：字段不变
      const a3 = await findAssignment(page, emp.id, '2026-08-13');
      expect(a3, 'out-of-range 08-13 assignment exists').not.toBeNull();
      expect(a3.isAbsent, '08-13 isAbsent unchanged').toBe(false);
      expect(a3.absenceReason, '08-13 absenceReason unchanged').toBeNull();
      expect(a3.leaveRequestId, '08-13 leaveRequestId unchanged').toBeNull();
      expect(a3.status, '08-13 status=SCHEDULED').toBe('SCHEDULED');
    } finally {
      await cleanupAll(page, emp.id, shift.id);
    }
  });

  test('cancel restores in-range assignments marked by the leave', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');

    const emp = await createEmployee(page, 'cnl');
    const shift = await createShift(page, uniq('E2E-LK-SHIFT-cnl'));

    try {
      // setup 2 SCHEDULED in-range assignments
      await assignSingle(page, emp.id, shift.id, '2026-08-10');
      await assignSingle(page, emp.id, shift.id, '2026-08-11');

      const leave = await createLeave(page, emp.id, 'cnl', '2026-08-10', '2026-08-12');

      // approve 前置：先 submit + approve 触发 onLeaveApproved 标记
      await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id: leave.id }, 'id');
      await callMutationOk(page, 'ErpHrLeaveRequest', 'approve', { id: leave.id }, 'id');

      // sanity: 区间内 08-10 行已被钩子标记为 ABSENT
      const before = await findAssignment(page, emp.id, '2026-08-10');
      expect(before?.status, 'sanity 08-10 status=ABSENT before cancel').toBe('ABSENT');

      // cancel: APPROVED → CANCELLED（内部委派 onLeaveCancelled 钩子）
      await callMutationOk(page, 'ErpHrLeaveRequest', 'cancel', { id: leave.id }, 'id');
      const st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
      expect(st.status, 'after cancel status=CANCELLED').toBe('CANCELLED');

      // 区间内 08-10 行：4 字段还原
      const a1 = await findAssignment(page, emp.id, '2026-08-10');
      expect(a1, 'restored 08-10 assignment exists').not.toBeNull();
      expect(a1.isAbsent, '08-10 isAbsent restored to false').toBe(false);
      expect(a1.absenceReason, '08-10 absenceReason restored to null').toBeNull();
      expect(a1.leaveRequestId, '08-10 leaveRequestId restored to null').toBeNull();
      expect(a1.status, '08-10 status restored to SCHEDULED').toBe('SCHEDULED');

      // 区间内 08-11 行：4 字段还原
      const a2 = await findAssignment(page, emp.id, '2026-08-11');
      expect(a2, 'restored 08-11 assignment exists').not.toBeNull();
      expect(a2.isAbsent, '08-11 isAbsent restored to false').toBe(false);
      expect(a2.absenceReason, '08-11 absenceReason restored to null').toBeNull();
      expect(a2.leaveRequestId, '08-11 leaveRequestId restored to null').toBeNull();
      expect(a2.status, '08-11 status restored to SCHEDULED').toBe('SCHEDULED');
    } finally {
      await cleanupAll(page, emp.id, shift.id);
    }
  });
});
