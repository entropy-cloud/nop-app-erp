import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  findFirst,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * hr ErpHrLeaveRequest 休假审批 + ErpHrAttendance 考勤打卡浏览器层 E2E（plan 2026-07-14-0215-3 Phase 2）。
 *
 * 验证两条 DIRECT 路径经 GraphQL /graphql 的全栈可达性：
 *
 * 休假审批状态机（ErpHrLeaveRequestBizModel，UC-HR-02）：
 *   DRAFT→submit→SUBMITTED→approve→APPROVED→cancel→CANCELLED
 *   reject 路径：SUBMITTED→reject→REJECTED
 *   日期重叠守卫：同员工同类型重叠区间 submit 抛 ERR_LEAVE_DATE_OVERLAP
 *   （代码容忍 null LeaveBalance——if(balance==null) return;，故不建 LeaveBalance 亦可测 approve 状态翻转）
 *
 * 考勤打卡端点（ErpHrAttendanceBizModel，UC-HR-06）：
 *   clockIn(employeeId) → ErpHrAttendance 行创建（clockInTime 非空）
 *   clockOut(employeeId) → clockOutTime 非空 + workHours 派生
 *   (employeeId,date) 唯一约束守卫：重复 clockIn 抛 ERR_ALREADY_CLOCKED_IN
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 * 自包含 setup：建 ErpHrEmployee + ErpHrLeaveRequest(DRAFT 入口)。清理：删 leave + attendance + employee。
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
      code: uniq(`E2E-LA-EMP-${tag}`),
      firstName: '考',
      lastName: tag,
      fullName: `考勤${tag}`,
      gender: 'MALE',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
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
      code: uniq(`E2E-LV-${tag}`),
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

async function cleanupEmployee(page: import('@playwright/test').Page, employeeId: string | number): Promise<void> {
  await deleteByFilter(page, 'ErpHrLeaveRequest', eqFilter('employeeId', Number(employeeId)));
  await deleteByFilter(page, 'ErpHrAttendance', eqFilter('employeeId', Number(employeeId)));
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

test.describe('hr ErpHrLeaveRequest approval state machine', () => {
  test('happy path: DRAFT→submit→SUBMITTED→approve→APPROVED→cancel→CANCELLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');

    const emp = await createEmployee(page, 'hp');
    const leave = await createLeave(page, emp.id, 'hp', '2026-08-10', '2026-08-12');

    // submit: DRAFT → SUBMITTED
    await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id: leave.id }, 'id');
    let st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
    expect(st.status, 'after submit status=SUBMITTED').toBe('SUBMITTED');

    // approve: SUBMITTED → APPROVED（LeaveBalance null → checkLeaveBalance return，不阻断）
    await callMutationOk(page, 'ErpHrLeaveRequest', 'approve', { id: leave.id }, 'id');
    st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status approvedAt');
    expect(st.status, 'after approve status=APPROVED').toBe('APPROVED');
    expect(st.approvedAt, 'approve sets approvedAt').not.toBeNull();

    // cancel: APPROVED → CANCELLED
    await callMutationOk(page, 'ErpHrLeaveRequest', 'cancel', { id: leave.id }, 'id');
    st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
    expect(st.status, 'after cancel status=CANCELLED').toBe('CANCELLED');

    await cleanupEmployee(page, emp.id);
  });

  test('reject path: submit→SUBMITTED→reject→REJECTED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');

    const emp = await createEmployee(page, 'rj');
    const leave = await createLeave(page, emp.id, 'rj', '2026-09-01', '2026-09-02');

    await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id: leave.id }, 'id');
    await callMutationOk(page, 'ErpHrLeaveRequest', 'reject', { id: leave.id }, 'id');
    const st = await verifyState(page, 'ErpHrLeaveRequest', leave.id, 'status');
    expect(st.status, 'after reject status=REJECTED').toBe('REJECTED');

    await cleanupEmployee(page, emp.id);
  });

  test('date overlap guard: second overlapping submit rejected (ERR_LEAVE_DATE_OVERLAP)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrLeaveRequest-main');

    const emp = await createEmployee(page, 'ov');
    // 两条同员工同类型重叠区间休假
    const first = await createLeave(page, emp.id, 'ov1', '2026-10-01', '2026-10-05');
    const second = await createLeave(page, emp.id, 'ov2', '2026-10-03', '2026-10-07');

    // 第一条 submit 成功（无其他 SUBMITTED/APPROVED 重叠）
    await callMutationOk(page, 'ErpHrLeaveRequest', 'submit', { id: first.id }, 'id');

    // 第二条 submit：与第一条 SUBMITTED 重叠 → ERR_LEAVE_DATE_OVERLAP
    const rej = await callMutation(page, 'ErpHrLeaveRequest', 'submit', { id: second.id }, 'id');
    expect(rej.errors, 'overlapping submit should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry date-overlap token').toContain('重叠');

    // 第二条状态不变（仍 DRAFT）
    const st = await verifyState(page, 'ErpHrLeaveRequest', second.id, 'status');
    expect(st.status, 'second leave unchanged after overlap guard reject').toBe('DRAFT');

    await cleanupEmployee(page, emp.id);
  });
});

test.describe('hr ErpHrAttendance clock in/out endpoints', () => {
  test('clockIn creates attendance + clockOut sets workHours + repeat clockIn guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrAttendance-main');

    const emp = await createEmployee(page, 'ci');

    // clockIn：创建当日考勤记录
    const clockInResult = await callMutationOk(
      page,
      'ErpHrAttendance',
      'clockIn',
      { employeeId: Number(emp.id) },
      'id clockIn',
    );
    expect(clockInResult.id, 'clockIn creates attendance row').not.toBeNull();
    expect(clockInResult.clockIn, 'clockIn timestamp set').not.toBeNull();

    // 独立 __get 断言
    const verified = await verifyState(page, 'ErpHrAttendance', clockInResult.id, 'clockIn clockOut workHours');
    expect(verified.clockIn, '__get clockIn non-null').not.toBeNull();
    expect(verified.clockOut, '__get clockOut still null before clockOut').toBeNull();

    // clockOut：设置 clockOut + workHours
    await callMutationOk(page, 'ErpHrAttendance', 'clockOut', { employeeId: Number(emp.id) }, 'id');
    const afterOut = await verifyState(page, 'ErpHrAttendance', clockInResult.id, 'clockIn clockOut workHours');
    expect(afterOut.clockOut, 'clockOut timestamp set').not.toBeNull();
    expect(Number(afterOut.workHours), 'workHours derived >= 0').toBeGreaterThanOrEqual(0);

    // 重复 clockIn 守卫：(employeeId,date) 已有记录 + clockIn 非空 → ERR_ALREADY_CLOCKED_IN
    const rej = await callMutation(page, 'ErpHrAttendance', 'clockIn', { employeeId: Number(emp.id) }, 'id');
    expect(rej.errors, 'repeat clockIn should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry already-clocked-in token').toContain('已签到');

    await cleanupEmployee(page, emp.id);
  });
});
