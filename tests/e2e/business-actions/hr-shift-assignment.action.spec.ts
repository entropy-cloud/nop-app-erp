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
 * hr ErpHrShiftAssignment 单/批量分配 + 周期复制 + ErpHrShift.calcAttendance 计算入口
 * 浏览器层 E2E（plan 2026-07-18-0100-1 Phase 1）。
 *
 * 验证 4 条 DIRECT 路径经 GraphQL /graphql 的全栈可达性：
 *
 * 单/批量分配（ErpHrShiftAssignmentBizModel，UC-HR-06）：
 *   assignSingle(employeeId, shiftId, assignmentDate) 单分配 + (employeeId,date) 唯一约束守卫
 *     ERR_SHIFT_DUPLICATE_ASSIGNMENT
 *   assignBatch(employeeIds:List<Long>, shiftId, startDate, endDate) 笛卡尔积批量
 *     返回 List<ErpHrShiftAssignment>；区间内重复 assignBatch 跳过已存在（返回 List 仅含新增行）
 *   copyFromPeriod(sourceStartDate, sourceEndDate, targetStartDate) 周期复制
 *
 * 考勤计算入口（ErpHrShiftBizModel.calcAttendance）：
 *   经打卡数据驱动迟到/早退写 Attendance + Assignment 双落点
 *   - shift.startTime + graceLateMinutes 比较 clockIn → lateMinutes > 0；准时打卡 lateMinutes = 0
 *   - assignment.actualStartTime/actualEndTime 回写；status=SCHEDULED→PRESENT；isAbsent=false
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 * 自包含 setup：建测试专用 employee + shift + 入口实体（避免污染种子基线）；finally 兜底 cleanup。
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
      code: uniq(`E2E-SA-EMP-${tag}`),
      firstName: '排',
      lastName: tag,
      fullName: `排班${tag}`,
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
  startTime = '09:00',
  endTime = '17:00',
  graceLateMinutes = 15,
): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpHrShift',
    {
      code,
      name: `E2E 班次 ${code}`,
      shiftType: 'FIXED',
      startTime,
      endTime,
      graceLateMinutes,
      graceEarlyLeaveMinutes: 15,
      requireClockIn: true,
      requireClockOut: true,
      orgId: 2,
    },
    'id code',
  );
}

async function createAttendance(
  page: import('@playwright/test').Page,
  employeeId: string | number,
  date: string,
  clockIn: string | null,
  clockOut: string | null = null,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrAttendance',
    {
      employeeId: Number(employeeId),
      date,
      clockIn,
      clockOut,
      isAbsent: false,
      lateMinutes: 0,
      earlyLeaveMinutes: 0,
      businessDate: date,
      orgId: 2,
    },
    'id',
  );
}

async function cleanupEmployee(page: import('@playwright/test').Page, employeeId: string | number): Promise<void> {
  await deleteByFilter(page, 'ErpHrShiftSwapRequest', eqFilter('requesterId', Number(employeeId)));
  await deleteByFilter(page, 'ErpHrShiftAssignment', eqFilter('employeeId', Number(employeeId)));
  await deleteByFilter(page, 'ErpHrAttendance', eqFilter('employeeId', Number(employeeId)));
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

async function cleanupShift(page: import('@playwright/test').Page, shiftId: string | number): Promise<void> {
  await deleteById(page, 'ErpHrShift', shiftId);
}

test.describe('hr ErpHrShiftAssignment single/batch/period-copy', () => {
  test('assignSingle creates SCHEDULED row + duplicate (employeeId,date) guard ERR_SHIFT_DUPLICATE_ASSIGNMENT', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftAssignment-main');

    const emp = await createEmployee(page, 'single');
    const shift = await createShift(page, uniq('E2E-SA-SHIFT-single'));

    try {
      // assignSingle: 创建 SCHEDULED 排班
      const assignment = await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignSingle',
        {
          employeeId: Number(emp.id),
          shiftId: Number(shift.id),
          assignmentDate: '2026-08-10',
        },
        'id status employeeId shiftId assignmentDate',
      );
      expect(assignment.id, 'assignment created').toBeTruthy();
      expect(assignment.status, 'status=SCHEDULED').toBe('SCHEDULED');
      expect(Number(assignment.employeeId), 'employeeId echo').toBe(Number(emp.id));
      expect(Number(assignment.shiftId), 'shiftId echo').toBe(Number(shift.id));
      expect(assignment.assignmentDate, 'assignmentDate echo').toContain('2026-08-10');

      // 独立 __get 断言
      const verified = await verifyState(
        page,
        'ErpHrShiftAssignment',
        assignment.id,
        'status employeeId shiftId assignmentDate',
      );
      expect(verified.status, '__get status=SCHEDULED').toBe('SCHEDULED');

      // 重复同 (employeeId,date) assignSingle → ERR_SHIFT_DUPLICATE_ASSIGNMENT
      const rej = await callMutation(
        page,
        'ErpHrShiftAssignment',
        'assignSingle',
        {
          employeeId: Number(emp.id),
          shiftId: Number(shift.id),
          assignmentDate: '2026-08-10',
        },
        'id',
      );
      expect(rej.errors, 'duplicate assignSingle should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry duplicate token').toContain('已存在');
    } finally {
      await cleanupEmployee(page, emp.id);
      await cleanupShift(page, shift.id);
    }
  });

  test('assignBatch cartesian product 3 employees x 3 days → 9 rows + repeat skips existing', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftAssignment-main');

    const empA = await createEmployee(page, 'bat-a');
    const empB = await createEmployee(page, 'bat-b');
    const empC = await createEmployee(page, 'bat-c');
    const shift = await createShift(page, uniq('E2E-SA-SHIFT-batch'));

    try {
      // assignBatch: 3 employee × 3 day 笛卡尔积 → 9 行
      const employeeIds = [Number(empA.id), Number(empB.id), Number(empC.id)];
      const result = await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignBatch',
        {
          employeeIds,
          shiftId: Number(shift.id),
          startDate: '2026-09-01',
          endDate: '2026-09-03',
        },
        'id status employeeId shiftId assignmentDate',
      );
      expect(Array.isArray(result), 'assignBatch returns List').toBe(true);
      expect(result.length, 'cartesian 3x3 = 9 rows').toBe(9);
      for (const row of result) {
        expect(row.status, 'each row status=SCHEDULED').toBe('SCHEDULED');
        expect(employeeIds, 'each row employeeId is one of the input').toContain(Number(row.employeeId));
      }

      // 区间内重复 assignBatch：跳过已存在 → 返回 List 仅含新增行（应为 0）
      const repeatResult = await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignBatch',
        {
          employeeIds,
          shiftId: Number(shift.id),
          startDate: '2026-09-01',
          endDate: '2026-09-03',
        },
        'id',
      );
      expect(Array.isArray(repeatResult), 'repeat assignBatch returns List').toBe(true);
      expect(repeatResult.length, 'repeat assignBatch skips existing → 0 new').toBe(0);
    } finally {
      await cleanupEmployee(page, empA.id);
      await cleanupEmployee(page, empB.id);
      await cleanupEmployee(page, empC.id);
      await cleanupShift(page, shift.id);
    }
  });

  test('copyFromPeriod copies source range to target range with same day-offset', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftAssignment-main');

    const emp = await createEmployee(page, 'copy');
    const shift = await createShift(page, uniq('E2E-SA-SHIFT-copy'));

    try {
      // 先 assignBatch 建源周期 3 天 (2026-10-01 .. 2026-10-03)
      await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignBatch',
        {
          employeeIds: [Number(emp.id)],
          shiftId: Number(shift.id),
          startDate: '2026-10-01',
          endDate: '2026-10-03',
        },
        'id',
      );

      // copyFromPeriod 复制到目标周期：targetStartDate = 2026-11-01（offset +31 days）
      const copied = await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'copyFromPeriod',
        {
          sourceStartDate: '2026-10-01',
          sourceEndDate: '2026-10-03',
          targetStartDate: '2026-11-01',
        },
        'id status employeeId shiftId assignmentDate',
      );
      expect(Array.isArray(copied), 'copyFromPeriod returns List').toBe(true);
      expect(copied.length, 'copied 3 rows from source 3-day range').toBe(3);
      const dates = copied.map((r: any) => r.assignmentDate).sort();
      expect(dates[0], 'first target day = targetStart').toContain('2026-11-01');
      expect(dates[2], 'last target day = targetStart + 2').toContain('2026-11-03');
      for (const row of copied) {
        expect(Number(row.employeeId), 'copied row employeeId echoes').toBe(Number(emp.id));
        expect(Number(row.shiftId), 'copied row shiftId echoes').toBe(Number(shift.id));
        expect(row.status, 'copied row status=SCHEDULED').toBe('SCHEDULED');
      }
    } finally {
      await cleanupEmployee(page, emp.id);
      await cleanupShift(page, shift.id);
    }
  });
});

test.describe('hr ErpHrShift.calcAttendance late/on-time entry', () => {
  test('calcAttendance writes lateMinutes>0 + assignment.actualStartTime on late clockIn', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShift-main');

    const emp = await createEmployee(page, 'late');
    // graceLateMinutes=15, startTime=09:00 → clockIn 09:30 → 30 > 15 → lateMinutes=30
    const shift = await createShift(page, uniq('E2E-SA-SHIFT-late'), '09:00', '17:00', 15);
    const date = '2026-11-15';

    try {
      // 前置：建 assignment (assignSingle) + 晚打卡 attendance
      const assignment = await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignSingle',
        {
          employeeId: Number(emp.id),
          shiftId: Number(shift.id),
          assignmentDate: date,
        },
        'id status',
      );
      await createAttendance(page, emp.id, date, `${date}T09:30:00`, `${date}T18:00:00`);

      // calcAttendance：触发迟到计算
      const attendance = await callMutationOk(
        page,
        'ErpHrShift',
        'calcAttendance',
        { employeeId: Number(emp.id), assignmentDate: date },
        'id lateMinutes earlyLeaveMinutes isAbsent',
      );
      expect(attendance.id, 'calcAttendance returns attendance').toBeTruthy();
      expect(Number(attendance.lateMinutes), 'late clockIn → lateMinutes=30').toBeGreaterThan(0);

      // 双落点断言：assignment 回写 actualStartTime + status=PRESENT + isAbsent=false
      const assignmentAfter = await verifyState(
        page,
        'ErpHrShiftAssignment',
        assignment.id,
        'status isAbsent actualStartTime actualEndTime',
      );
      expect(assignmentAfter.status, 'assignment status flipped to PRESENT').toBe('PRESENT');
      expect(assignmentAfter.isAbsent, 'assignment.isAbsent=false after present clockIn').toBe(false);
      expect(assignmentAfter.actualStartTime, 'actualStartTime written from attendance.clockIn').not.toBeNull();
    } finally {
      await cleanupEmployee(page, emp.id);
      await cleanupShift(page, shift.id);
    }
  });

  test('calcAttendance on-time clockIn → lateMinutes=0', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShift-main');

    const emp = await createEmployee(page, 'onTime');
    const shift = await createShift(page, uniq('E2E-SA-SHIFT-ontime'), '09:00', '17:00', 15);
    const date = '2026-11-16';

    try {
      await callMutationOk(
        page,
        'ErpHrShiftAssignment',
        'assignSingle',
        {
          employeeId: Number(emp.id),
          shiftId: Number(shift.id),
          assignmentDate: date,
        },
        'id',
      );
      // 准时打卡 09:05 → 5 <= 15 → lateMinutes=0
      await createAttendance(page, emp.id, date, `${date}T09:05:00`, `${date}T17:00:00`);

      const attendance = await callMutationOk(
        page,
        'ErpHrShift',
        'calcAttendance',
        { employeeId: Number(emp.id), assignmentDate: date },
        'id lateMinutes earlyLeaveMinutes isAbsent',
      );
      expect(Number(attendance.lateMinutes), 'on-time clockIn → lateMinutes=0').toBe(0);
    } finally {
      await cleanupEmployee(page, emp.id);
      await cleanupShift(page, shift.id);
    }
  });
});
