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
 * hr ErpHrShiftSwapRequest 4 动作状态机 + approve 双向班次交换副作用
 * 浏览器层 E2E（plan 2026-07-18-0100-1 Phase 3）。
 *
 * 验证 DIRECT 路径经 GraphQL /graphql 的全栈可达性 + 状态机迁移 + 双向副作用：
 *   submit(sourceAssignmentId, targetAssignmentId, reason) → PENDING
 *     无前置状态守卫、无重复守卫（每次 submit 经 nanoTime code 建新 PENDING 行）。
 *   approve(swapRequestId) PENDING→APPROVED + **核心副作用**：
 *     (a) source assignment shiftId 翻转 = 原 target shiftId
 *     (b) target assignment shiftId 翻转 = 原 source shiftId
 *     (c) 双 assignment swapRequestId = swap id
 *     (d) 双 assignment replacedByAssignmentId 双向回链（source.replacedBy=target.id, target.replacedBy=source.id）
 *     (e) 双 assignment status=SCHEDULED（重新置位）
 *   reject(swapRequestId) PENDING→REJECTED + 双 assignment 不变（无 shiftId 交换）
 *   cancel(swapRequestId) PENDING→CANCELLED + 双 assignment 不变
 *   非法状态迁移守卫：APPROVED/REJECTED/CANCELLED 状态的 swapRequestId 调 approve/reject/cancel
 *     抛 ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION（description 含「不允许」语义 token）。
 *
 * 自包含 setup：建 2 employee × 2 shift × 2 assignment（同日）作为交换基底；finally 兜底 cleanup。
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
      code: uniq(`E2E-SW-EMP-${tag}`),
      firstName: '换',
      lastName: tag,
      fullName: `调换${tag}`,
      gender: 'MALE',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      orgId: 2,
    },
    'id',
  );
}

async function createShift(page: import('@playwright/test').Page, code: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpHrShift',
    {
      code,
      name: `E2E 班次 ${code}`,
      shiftType: 'FIXED',
      startTime: '09:00',
      endTime: '17:00',
      graceLateMinutes: 15,
      graceEarlyLeaveMinutes: 15,
      requireClockIn: false,
      requireClockOut: false,
      orgId: 2,
    },
    'id',
  );
}

/** 建 1 对 (source, target) assignment：A 员工 shiftA + B 员工 shiftB 同日 */
async function buildSwapPair(
  page: import('@playwright/test').Page,
  empA: { id: string },
  empB: { id: string },
  shiftA: { id: string },
  shiftB: { id: string },
  date: string,
): Promise<{ source: { id: string }; target: { id: string } }> {
  const source = await callMutationOk(
    page,
    'ErpHrShiftAssignment',
    'assignSingle',
    { employeeId: Number(empA.id), shiftId: Number(shiftA.id), assignmentDate: date },
    'id',
  );
  const target = await callMutationOk(
    page,
    'ErpHrShiftAssignment',
    'assignSingle',
    { employeeId: Number(empB.id), shiftId: Number(shiftB.id), assignmentDate: date },
    'id',
  );
  return { source, target };
}

async function cleanupEmployee(page: import('@playwright/test').Page, employeeId: string | number): Promise<void> {
  // 按 employeeId 反查 assignment.id，再删 swap (sourceAssignmentId/targetAssignmentId 引用)
  await deleteByFilter(page, 'ErpHrShiftSwapRequest', eqFilter('requesterId', Number(employeeId)));
  await deleteByFilter(page, 'ErpHrShiftAssignment', eqFilter('employeeId', Number(employeeId)));
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

async function cleanupShift(page: import('@playwright/test').Page, shiftId: string | number): Promise<void> {
  await deleteById(page, 'ErpHrShift', shiftId);
}

test.describe('hr ErpHrShiftSwapRequest state machine + approve bilateral shift exchange', () => {
  test('happy path: submit→PENDING→approve→APPROVED with bilateral shiftId swap + swapRequestId/replacedByAssignmentId cross-link + status reset', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftSwapRequest-main');

    const empA = await createEmployee(page, 'sw-a');
    const empB = await createEmployee(page, 'sw-b');
    const shiftA = await createShift(page, uniq('E2E-SW-A'));
    const shiftB = await createShift(page, uniq('E2E-SW-B'));

    try {
      const pair = await buildSwapPair(page, empA, empB, shiftA, shiftB, '2026-12-01');

      // submit → PENDING
      const swap = await callMutationOk(
        page,
        'ErpHrShiftSwapRequest',
        'submit',
        {
          sourceAssignmentId: Number(pair.source.id),
          targetAssignmentId: Number(pair.target.id),
          reason: 'E2E bilateral swap test',
        },
        'id status sourceAssignmentId targetAssignmentId',
      );
      expect(swap.id, 'swap created').toBeTruthy();
      expect(swap.status, 'after submit status=PENDING').toBe('PENDING');

      // approve → APPROVED
      const approved = await callMutationOk(
        page,
        'ErpHrShiftSwapRequest',
        'approve',
        { swapRequestId: Number(swap.id) },
        'id status',
      );
      expect(approved.status, 'after approve status=APPROVED').toBe('APPROVED');

      // 双向副作用断言经 verifyState __get：
      // (a)(b) source/target shiftId 翻转互换
      const srcAfter = await verifyState(
        page,
        'ErpHrShiftAssignment',
        pair.source.id,
        'shiftId swapRequestId replacedByAssignmentId status',
      );
      const tgtAfter = await verifyState(
        page,
        'ErpHrShiftAssignment',
        pair.target.id,
        'shiftId swapRequestId replacedByAssignmentId status',
      );
      expect(Number(srcAfter.shiftId), 'source.shiftId flipped to original target.shiftId').toBe(Number(shiftB.id));
      expect(Number(tgtAfter.shiftId), 'target.shiftId flipped to original source.shiftId').toBe(Number(shiftA.id));
      // (c) 双 assignment swapRequestId = swap id
      expect(Number(srcAfter.swapRequestId), 'source.swapRequestId = swap.id').toBe(Number(swap.id));
      expect(Number(tgtAfter.swapRequestId), 'target.swapRequestId = swap.id').toBe(Number(swap.id));
      // (d) 双 assignment replacedByAssignmentId 双向回链
      expect(Number(srcAfter.replacedByAssignmentId), 'source.replacedBy = target.id').toBe(Number(pair.target.id));
      expect(Number(tgtAfter.replacedByAssignmentId), 'target.replacedBy = source.id').toBe(Number(pair.source.id));
      // (e) 双 assignment status=SCHEDULED（重置）
      expect(srcAfter.status, 'source status reset SCHEDULED').toBe('SCHEDULED');
      expect(tgtAfter.status, 'target status reset SCHEDULED').toBe('SCHEDULED');
    } finally {
      await cleanupEmployee(page, empA.id);
      await cleanupEmployee(page, empB.id);
      await cleanupShift(page, shiftA.id);
      await cleanupShift(page, shiftB.id);
    }
  });

  test('reject path: submit→PENDING→reject→REJECTED + no shiftId exchange', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftSwapRequest-main');

    const empA = await createEmployee(page, 'rj-a');
    const empB = await createEmployee(page, 'rj-b');
    const shiftA = await createShift(page, uniq('E2E-SW-RJ-A'));
    const shiftB = await createShift(page, uniq('E2E-SW-RJ-B'));

    try {
      const pair = await buildSwapPair(page, empA, empB, shiftA, shiftB, '2026-12-08');

      const swap = await callMutationOk(
        page,
        'ErpHrShiftSwapRequest',
        'submit',
        {
          sourceAssignmentId: Number(pair.source.id),
          targetAssignmentId: Number(pair.target.id),
          reason: 'reject path',
        },
        'id',
      );

      await callMutationOk(page, 'ErpHrShiftSwapRequest', 'reject', { swapRequestId: Number(swap.id) }, 'id status');
      const st = await verifyState(page, 'ErpHrShiftSwapRequest', swap.id, 'status');
      expect(st.status, 'after reject status=REJECTED').toBe('REJECTED');

      // 双 assignment 不变：source.shiftId 仍 = shiftA.id，target.shiftId 仍 = shiftB.id
      const srcAfter = await verifyState(page, 'ErpHrShiftAssignment', pair.source.id, 'shiftId');
      const tgtAfter = await verifyState(page, 'ErpHrShiftAssignment', pair.target.id, 'shiftId');
      expect(Number(srcAfter.shiftId), 'source.shiftId unchanged after reject').toBe(Number(shiftA.id));
      expect(Number(tgtAfter.shiftId), 'target.shiftId unchanged after reject').toBe(Number(shiftB.id));
    } finally {
      await cleanupEmployee(page, empA.id);
      await cleanupEmployee(page, empB.id);
      await cleanupShift(page, shiftA.id);
      await cleanupShift(page, shiftB.id);
    }
  });

  test('cancel path: submit→PENDING→cancel→CANCELLED + no shiftId exchange', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftSwapRequest-main');

    const empA = await createEmployee(page, 'cc-a');
    const empB = await createEmployee(page, 'cc-b');
    const shiftA = await createShift(page, uniq('E2E-SW-CC-A'));
    const shiftB = await createShift(page, uniq('E2E-SW-CC-B'));

    try {
      const pair = await buildSwapPair(page, empA, empB, shiftA, shiftB, '2026-12-15');

      const swap = await callMutationOk(
        page,
        'ErpHrShiftSwapRequest',
        'submit',
        {
          sourceAssignmentId: Number(pair.source.id),
          targetAssignmentId: Number(pair.target.id),
          reason: 'cancel path',
        },
        'id',
      );

      await callMutationOk(page, 'ErpHrShiftSwapRequest', 'cancel', { swapRequestId: Number(swap.id) }, 'id status');
      const st = await verifyState(page, 'ErpHrShiftSwapRequest', swap.id, 'status');
      expect(st.status, 'after cancel status=CANCELLED').toBe('CANCELLED');

      // 双 assignment 不变
      const srcAfter = await verifyState(page, 'ErpHrShiftAssignment', pair.source.id, 'shiftId');
      const tgtAfter = await verifyState(page, 'ErpHrShiftAssignment', pair.target.id, 'shiftId');
      expect(Number(srcAfter.shiftId), 'source.shiftId unchanged after cancel').toBe(Number(shiftA.id));
      expect(Number(tgtAfter.shiftId), 'target.shiftId unchanged after cancel').toBe(Number(shiftB.id));
    } finally {
      await cleanupEmployee(page, empA.id);
      await cleanupEmployee(page, empB.id);
      await cleanupShift(page, shiftA.id);
      await cleanupShift(page, shiftB.id);
    }
  });

  test('illegal status transition guard: APPROVED→approve rejected with ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrShiftSwapRequest-main');

    const empA = await createEmployee(page, 'ig-a');
    const empB = await createEmployee(page, 'ig-b');
    const shiftA = await createShift(page, uniq('E2E-SW-IG-A'));
    const shiftB = await createShift(page, uniq('E2E-SW-IG-B'));

    try {
      const pair = await buildSwapPair(page, empA, empB, shiftA, shiftB, '2026-12-20');
      const swap = await callMutationOk(
        page,
        'ErpHrShiftSwapRequest',
        'submit',
        {
          sourceAssignmentId: Number(pair.source.id),
          targetAssignmentId: Number(pair.target.id),
          reason: 'illegal transition',
        },
        'id',
      );
      // 先 approve 走完正常路径到 APPROVED
      await callMutationOk(page, 'ErpHrShiftSwapRequest', 'approve', { swapRequestId: Number(swap.id) }, 'id');

      // 再次 approve（APPROVED → approve 期望 PENDING）→ ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION
      const rej = await callMutation(
        page,
        'ErpHrShiftSwapRequest',
        'approve',
        { swapRequestId: Number(swap.id) },
        'id',
      );
      expect(rej.errors, 'illegal transition should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许');

      // reject 同样在 APPROVED 状态下应被守卫拒绝
      const rej2 = await callMutation(
        page,
        'ErpHrShiftSwapRequest',
        'reject',
        { swapRequestId: Number(swap.id) },
        'id',
      );
      expect(rej2.errors, 'reject from APPROVED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej2.errors), 'reject should carry illegal-transition token').toContain('不允许');
    } finally {
      await cleanupEmployee(page, empA.id);
      await cleanupEmployee(page, empB.id);
      await cleanupShift(page, shiftA.id);
      await cleanupShift(page, shiftB.id);
    }
  });
});
