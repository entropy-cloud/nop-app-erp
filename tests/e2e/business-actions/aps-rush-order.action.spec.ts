import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';

/**
 * aps ErpApsOperationOrder insertRushOrder 插单区间重排业务动作浏览器层 E2E（plan 2026-07-14-0941-2 Phase 2）。
 *
 * 验证插单编排 DIRECT @BizMutation（insertRushOrder）经 GraphQL /graphql 的全栈可达性 + SchedulingResult 结构
 * 断言 + 急单 PLANNED 翻转 + 区间重排效果（背景工序 plannedStart 被推移）。
 *
 * 权威编排（ErpApsSchedulingProcessor.insertRushOrder，对齐 docs/design/aps/scheduling.md §六）：
 *   insertRushOrder(operationOrderId)：检测急单 [earliestStartDateT, latestEndDateT+buffer] 时间窗口，
 *     窗口内同工作中心、优先级低于新单（priority 数字更大）的 PLANNED 工序回退 DRAFT；IN_PROGRESS 工序
 *     永不回退（抛 ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE）。随后仅对窗口内 DRAFT 工序（含新单 + 回退者）
 *     重排，保留的 PLANNED 工序作为已占用区间。返回 SchedulingResult（feasible/scheduledOperationIds/conflicts）。
 *
 * Phase 2 Explore Decision（前置 + 区间重排效果裁定）：
 *   - insertRushOrder 不检查 schedule 状态（仅 requireOperationOrder），急单 operationOrderId 须存在。
 *     急单 status 非 DRAFT 时方法内置置 DRAFT 统一处理（无状态守卫拒绝）。
 *   - SchedulingResult 仅三字段：feasible(boolean) + scheduledOperationIds(List<Long>) + conflicts
 *     (List<ConflictReport>)。plannedStart/plannedEnd 在 ErpApsOperationOrder 实体上（非返回值），须排产后
 *     __get 独立断言。
 *   - 区间重排效果验证：先 scheduleForward 排背景工序（priority=50）→ PLANNED + plannedStart 落 08:00；
 *     再建急单（priority=10，同 machineId，earliestStartDateT 与背景工序窗口重叠）→ insertRushOrder
 *     将背景工序（priority 50 > rush 10）回退 DRAFT 后重排 → 背景工序 plannedStart 被推移至急单之后
 *     （区间重排可观测）。
 *   - 排程不可行路径（feasible=false）：insertRushOrder 内部调 engine.scheduleForward（非 scheduleBackward），
 *     正向排产总是 feasible=true（顺序填充），feasible=false 不可达（同 0508-1 scheduleForward 裁定）。
 *   - 非 DRAFT 工序 insertRushOrder 拒绝：后端无状态守卫（方法内置置 DRAFT），故不测负路径。
 *
 * 自包含 setup：建 Schedule（DRAFT + horizon 区间）+ 背景工序（DRAFT, machineId=100, priority=50）→
 *   scheduleForward → PLANNED（plannedStart 落 08:00）。再建急单工序（DRAFT, machineId=100, priority=10,
 *   earliestStartDateT 与背景工序重叠, latestEndDateT 兜底窗口）→ insertRushOrder。
 * 清理：删 急单工序 + 背景工序 + Schedule（无下游产物）。
 */

const HORIZON_START = '2026-07-10T00:00:00';
const HORIZON_END = '2026-07-20T00:00:00';
const MACHINE_ID = 100;
const WORK_ORDER_ID = 1;

async function seedSchedule(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpApsSchedule',
    {
      code: `E2E-APS-RUSH-SCH-${tag}-${Date.now()}`,
      name: `E2E Rush Schedule ${tag}`,
      scheduleDate: '2026-07-10',
      schedulingMode: 'FORWARD',
      horizonStart: HORIZON_START,
      horizonEnd: HORIZON_END,
      status: 'DRAFT',
    },
    'id',
  );
}

async function seedOpOrder(
  page: import('@playwright/test').Page,
  tag: string,
  opts: { priority: number; earliestStart?: string; latestEnd?: string },
): Promise<{ id: string; status: string }> {
  const data: Record<string, unknown> = {
    code: `E2E-APS-RUSH-OP-${tag}-${Date.now()}`,
    workOrderId: WORK_ORDER_ID,
    operationName: `E2E Rush Op ${tag}`,
    sequence: 10,
    machineId: MACHINE_ID,
    priority: opts.priority,
    setupTime: 0,
    runtimePerUnit: 10,
    qty: 3, // duration = 0 + 10×3 = 30 分钟
    earliestStartDateT: opts.earliestStart ?? '2026-07-10T08:00:00',
    status: 'DRAFT',
  };
  if (opts.latestEnd) {
    data.latestEndDateT = opts.latestEnd;
  }
  return createViaSave(page, 'ErpApsOperationOrder', data, 'id status');
}

test.describe('aps ErpApsOperationOrder insertRushOrder (rush order interval rescheduling)', () => {
  test('insertRushOrder: background PLANNED + rush DRAFT → SchedulingResult feasible + rush PLANNED + background pushed', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsOperationOrder-main');

    const schedule = await seedSchedule(page, 'rush');

    // 背景工序：priority=50（低优先级），先经 scheduleForward 排定 → PLANNED
    const background = await seedOpOrder(page, 'bg', { priority: 50, earliestStart: '2026-07-10T08:00:00' });
    expect(background.status, 'precondition bg status=DRAFT').toBe('DRAFT');

    await callMutationOk(page, 'ErpApsOperationOrder', 'scheduleForward', { scheduleId: schedule.id }, 'feasible scheduledOperationIds');

    const bgBefore = await verifyState(page, 'ErpApsOperationOrder', background.id, 'status plannedStartDateT plannedEndDateT');
    expect(bgBefore.status, 'bg status after scheduleForward = PLANNED').toBe('PLANNED');
    expect(bgBefore.plannedStartDateT, 'bg plannedStart should be written back').not.toBeNull();
    const bgPlannedStartBefore = bgBefore.plannedStartDateT;

    // 急单工序：priority=10（高优先级），同 machineId，earliestStartDateT 与背景工序窗口重叠
    const rush = await seedOpOrder(page, 'rush', {
      priority: 10,
      earliestStart: '2026-07-10T08:00:00',
      latestEnd: '2026-07-10T09:00:00',
    });
    expect(rush.status, 'precondition rush status=DRAFT').toBe('DRAFT');

    // insertRushOrder：急单插入 → 背景工序（priority 50 > rush 10）回退 DRAFT 后重排
    const result = await callMutationOk(
      page, 'ErpApsOperationOrder', 'insertRushOrder',
      { operationOrderId: rush.id },
      'feasible scheduledOperationIds',
    );
    expect(result.feasible, 'insertRushOrder should report feasible=true').toBe(true);
    expect(Array.isArray(result.scheduledOperationIds), 'scheduledOperationIds should be array').toBe(true);
    expect(result.scheduledOperationIds.length, 'at least 1 op scheduled').toBeGreaterThan(0);
    expect(result.scheduledOperationIds.map(String), 'rush id should be in scheduledOperationIds').toContain(String(rush.id));

    // 急单 PLANNED + plannedStart/End 写回
    const rushV = await verifyState(page, 'ErpApsOperationOrder', rush.id, 'status plannedStartDateT plannedEndDateT');
    expect(rushV.status, 'rush status after insertRushOrder = PLANNED').toBe('PLANNED');
    expect(rushV.plannedStartDateT, 'rush plannedStart should be written back').not.toBeNull();
    expect(rushV.plannedEndDateT, 'rush plannedEnd should be written back').not.toBeNull();

    // 背景工序区间重排效果：plannedStart 被推移（急单优先排定，背景工序被推后）
    const bgAfter = await verifyState(page, 'ErpApsOperationOrder', background.id, 'status plannedStartDateT plannedEndDateT');
    expect(bgAfter.status, 'bg status after reschedule = PLANNED').toBe('PLANNED');
    expect(bgAfter.plannedStartDateT, 'bg plannedStart should be written back after reschedule').not.toBeNull();
    expect(bgAfter.plannedStartDateT, 'bg plannedStart should be pushed later (interval rescheduled)').not.toBe(bgPlannedStartBefore);

    await deleteById(page, 'ErpApsOperationOrder', rush.id);
    await deleteById(page, 'ErpApsOperationOrder', background.id);
    await deleteById(page, 'ErpApsSchedule', schedule.id);
  });
});
