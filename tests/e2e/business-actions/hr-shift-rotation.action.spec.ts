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
  GraphQLClient,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * hr ErpHrShiftRotationPattern 轮换引擎 + 重生成幂等浏览器层 E2E
 * （plan 2026-07-18-0100-1 Phase 2）。
 *
 * 验证 2 条 DIRECT 路径经 GraphQL /graphql 的全栈可达性：
 *
 *   generateRotation(patternId, groupMemberIds:List<Long>, staggerDays, startDate, endDate, regenerate)
 *   算法（ErpHrShiftRotationPatternBizModel :56-94 实测）：
 *     - 逐成员 i：memberStart = startDate + staggerDays·i
 *     - 逐日 day 从 memberStart 到 endDate：取 sequence[dayIndex % cycleLength]（patternData JSON 数组）
 *       · "OFF" 跳过
 *       · 已有 SCHEDULED assignment (employeeId,day) 跳过
 *       · 否则按班次 code 建 SCHEDULED assignment
 *     - regenerate=true 先 CANCEL 区间内既有 SCHEDULED assignment 再重建
 *     - regenerate=false 静默跳过已存在（不 CANCEL、不抛守卫）
 *
 * 测试样本（确定性 count，无 OFF）：
 *   patternData = ["MORNING","AFTERNOON","NIGHT"]（cycleLength=3）
 *   3 成员 × staggerDays=1 × startDate=2026-08-01 × endDate=2026-08-03
 *     member 0: 08-01,08-02,08-03 → 3 行
 *     member 1: 08-02,08-03 → 2 行
 *     member 2: 08-03 → 1 行
 *     total = 6 行
 *
 * 自包含 setup：建测试专用 employee + 班次模板（codes = MORNING/AFTERNOON/NIGHT）+ pattern；
 * finally 兜底 cleanup（删 assignment + pattern + shifts + employees）。
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
      code: uniq(`E2E-RO-EMP-${tag}`),
      firstName: '轮',
      lastName: tag,
      fullName: `轮换${tag}`,
      gender: 'MALE',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      orgId: 2,
    },
    'id',
  );
}

async function createShiftByCode(
  page: import('@playwright/test').Page,
  code: string,
): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpHrShift',
    {
      code,
      name: `E2E 班次 ${code}`,
      shiftType: 'FIXED',
      startTime: '08:00',
      endTime: '16:00',
      graceLateMinutes: 15,
      graceEarlyLeaveMinutes: 15,
      requireClockIn: false,
      requireClockOut: false,
      orgId: 2,
    },
    'id code',
  );
}

async function createPattern(
  page: import('@playwright/test').Page,
  patternData: string,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrShiftRotationPattern',
    {
      code: uniq('E2E-RO-PAT'),
      name: 'E2E 轮换模板',
      patternType: 'CYCLE_DAYS',
      patternData,
      orgId: 2,
    },
    'id',
  );
}

async function cleanupEmployee(page: import('@playwright/test').Page, employeeId: string | number): Promise<void> {
  await deleteByFilter(page, 'ErpHrShiftAssignment', eqFilter('employeeId', Number(employeeId)));
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

async function cleanupShift(page: import('@playwright/test').Page, shiftId: string | number): Promise<void> {
  await deleteById(page, 'ErpHrShift', shiftId);
}

test.describe('hr ErpHrShiftRotationPattern.generateRotation first run + regenerate idempotent', () => {
  test('first run generates 6 assignments for 3 members × staggerDays=1 × 3-day range (cycleLength=3)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftRotationPattern-main');

    const e1 = await createEmployee(page, 'ro-1');
    const e2 = await createEmployee(page, 'ro-2');
    const e3 = await createEmployee(page, 'ro-3');
    const morning = await createShiftByCode(page, uniq('MORNING'));
    const afternoon = await createShiftByCode(page, uniq('AFTERNOON'));
    const night = await createShiftByCode(page, uniq('NIGHT'));

    // patternData 引用班次 code，须与所建 shift.code 完全一致
    const patternData = JSON.stringify([morning.code, afternoon.code, night.code]);
    const pattern = await createPattern(page, patternData);

    try {
      const memberIds = [Number(e1.id), Number(e2.id), Number(e3.id)];
      // 首次生成 regenerate=false
      const result = await callMutationOk(
        page,
        'ErpHrShiftRotationPattern',
        'generateRotation',
        {
          patternId: Number(pattern.id),
          groupMemberIds: memberIds,
          staggerDays: 1,
          startDate: '2026-08-01',
          endDate: '2026-08-03',
          regenerate: false,
        },
        'id status employeeId shiftId assignmentDate',
      );
      expect(Array.isArray(result), 'generateRotation returns List').toBe(true);
      expect(result.length, 'expected 6 = 3+2+1 staggered rows').toBe(6);
      for (const row of result) {
        expect(row.status, 'each row status=SCHEDULED').toBe('SCHEDULED');
      }

      // member 0 (e1) 错峰起始日 = 08-01 → 覆盖 08-01/02/03 三行
      const e1Rows = result.filter((r: any) => Number(r.employeeId) === Number(e1.id));
      expect(e1Rows.length, 'member 0 covers 3 days').toBe(3);
      // member 2 (e3) 错峰起始日 = 08-03 → 仅 1 行
      const e3Rows = result.filter((r: any) => Number(r.employeeId) === Number(e3.id));
      expect(e3Rows.length, 'member 2 covers 1 day (staggered past end)').toBe(1);
    } finally {
      await cleanupEmployee(page, e1.id);
      await cleanupEmployee(page, e2.id);
      await cleanupEmployee(page, e3.id);
      await cleanupShift(page, morning.id);
      await cleanupShift(page, afternoon.id);
      await cleanupShift(page, night.id);
      await deleteById(page, 'ErpHrShiftRotationPattern', pattern.id);
    }
  });

  test('regenerate=true cancels old SCHEDULED rows and rebuilds same count; regenerate=false silently skips', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftRotationPattern-main');

    const e1 = await createEmployee(page, 're-1');
    const e2 = await createEmployee(page, 're-2');
    const e3 = await createEmployee(page, 're-3');
    const morning = await createShiftByCode(page, uniq('MORNING'));
    const afternoon = await createShiftByCode(page, uniq('AFTERNOON'));
    const night = await createShiftByCode(page, uniq('NIGHT'));
    const patternData = JSON.stringify([morning.code, afternoon.code, night.code]);
    const pattern = await createPattern(page, patternData);

    try {
      const memberIds = [Number(e1.id), Number(e2.id), Number(e3.id)];
      const args = {
        patternId: Number(pattern.id),
        groupMemberIds: memberIds,
        staggerDays: 1,
        startDate: '2026-08-10',
        endDate: '2026-08-12',
        regenerate: false,
      };

      // 首次生成：6 行 SCHEDULED
      const first = await callMutationOk(
        page,
        'ErpHrShiftRotationPattern',
        'generateRotation',
        args,
        'id',
      );
      expect(first.length, 'first run 6 rows').toBe(6);

      // regenerate=true 重生成：CANCEL 旧 6 行 SCHEDULED + 重建 6 行
      const regen = await callMutationOk(
        page,
        'ErpHrShiftRotationPattern',
        'generateRotation',
        { ...args, regenerate: true },
        'id',
      );
      expect(regen.length, 'regenerated rows = first run count').toBe(6);

      // 反查：CANCELLED 旧行存在（按 e1 范围内 status=CANCELLED）
      const cancelled = await new GraphQLClient(page).findItems<any>(
        'ErpHrShiftAssignment',
        {
          $type: 'and',
          $body: [
            { $type: 'eq', name: 'employeeId', value: Number(e1.id) },
            { $type: 'eq', name: 'status', value: 'CANCELLED' },
          ],
        },
        'id status',
      );
      expect(cancelled.length, 'regenerate CANCELs old SCHEDULED rows').toBeGreaterThan(0);

      // 当前 SCHEDULED 行数 = 6（仅重建后的新行）
      const scheduled = await new GraphQLClient(page).findItems<any>(
        'ErpHrShiftAssignment',
        {
          $type: 'and',
          $body: [
            { $type: 'in', name: 'employeeId', value: memberIds },
            { $type: 'eq', name: 'status', value: 'SCHEDULED' },
          ],
        },
        'id',
        50,
      );
      expect(scheduled.length, 'regenerate rebuilds exactly 6 SCHEDULED').toBe(6);

      // 再调 regenerate=false：所有 (employeeId,day) 已有 SCHEDULED → 静默跳过，0 新增
      const skip = await callMutationOk(
        page,
        'ErpHrShiftRotationPattern',
        'generateRotation',
        { ...args, regenerate: false },
        'id',
      );
      expect(Array.isArray(skip) ? skip.length : 0, 'regenerate=false silently skips existing').toBe(0);
    } finally {
      await cleanupEmployee(page, e1.id);
      await cleanupEmployee(page, e2.id);
      await cleanupEmployee(page, e3.id);
      await cleanupShift(page, morning.id);
      await cleanupShift(page, afternoon.id);
      await cleanupShift(page, night.id);
      await deleteById(page, 'ErpHrShiftRotationPattern', pattern.id);
    }
  });
});
