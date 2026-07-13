import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';

/**
 * aps ErpApsOperationOrder 工序排产引擎业务动作浏览器层 E2E（plan 2026-07-14-0508-1 Phase 1）。
 *
 * 验证排产引擎 DIRECT @BizMutation（scheduleForward/scheduleBackward）经 GraphQL /graphql 的全栈可达性 +
 * SchedulingResult 结构断言 + 工序状态翻转（DRAFT→PLANNED）+ plannedStart/EndDateT 写回。
 *
 * 权威引擎（ErpApsSchedulingProcessor 委派 ErpApsSchedulingEngine，对齐 docs/design/aps/scheduling.md）：
 *   scheduleForward(scheduleId)：拉取 DRAFT 态 ErpApsOperationOrder（earliestStartDateT 落
 *     Schedule.horizonStart~horizonEnd 内）+ MAINTENANCE 约束，按 (priority ASC, latestEndDateT ASC,
 *     sequence ASC) 排序，从 earliestStartDateT 正向填充工作中心可用时段；写回 plannedStart/EndDateT +
 *     status=PLANNED。
 *   scheduleBackward(scheduleId)：从 latestEndDateT（或 horizonEnd 兜底）逆向倒推；交期可达时
 *     status=PLANNED，否则 feasible=false + conflicts 非空。
 *   排产方案须 DRAFT 才允许重排（PUBLISHED/ARCHIVED 抛 ERR_APS_SCHEDULE_ILLEGAL_STATUS）。
 *
 * Phase 1 Explore Decision（scheduleForward 可达性裁定）：
 *   - 经自包含 setup（建 Schedule DRAFT + 单条 OperationOrder DRAFT，无维护约束、无工作中心配置链依赖）
 *     可达 status=PLANNED 翻转 + plannedStart/EndDateT 非空写回。理由：
 *     (1) ErpApsOperationOrder 的 workOrderId/machineId 列无 FK 约束（仅 stdSqlType=BIGINT），
 *         Java 集成测试 TestErpApsSchedulingEngine 用任意 1L/100L 即可；
 *     (2) ErpApsSchedulingEngine 为纯算法 POJO，capacity=1 单工位，无工作中心日历/产能配置依赖；
 *     (3) loadPendingOrders 按 status=DRAFT + earliestStartDateT ∈ [horizonStart, horizonEnd] 过滤，
 *         horizon 内工序必被纳入。
 *   - 故 scheduleForward/scheduleBackward 均落 spec（不降级）。
 *
 * 自包含 setup：建 Schedule（DRAFT + horizon 区间）+ OperationOrder（DRAFT + earliestStartDateT 落区间内 +
 *   setupTime/runtimePerUnit/qty 派生 duration ≥1）。machineId/workOrderId 用 100L/1L（与 Java 集成测试同范式）。
 * 清理：删 OperationOrder + Schedule（无下游产物）。
 */

const HORIZON_START = '2026-07-10T00:00:00';
const HORIZON_END = '2026-07-20T00:00:00';
const MACHINE_ID = 100; // 无 FK 约束，与 Java 测试 MACHINE_A=100L 一致
const WORK_ORDER_ID = 1; // 无 FK 约束

async function seedSchedule(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpApsSchedule',
    {
      code: `E2E-APS-OPS-SCH-${tag}-${Date.now()}`,
      name: `E2E OpOrder Schedule ${tag}`,
      scheduleDate: '2026-07-10',
      schedulingMode: 'FORWARD',
      horizonStart: HORIZON_START,
      horizonEnd: HORIZON_END,
      status: 'DRAFT',
    },
    'id status',
  );
}

async function seedOpOrder(page: import('@playwright/test').Page, tag: string, opts: { latestEnd?: string } = {}): Promise<{ id: string; status: string }> {
  const data: Record<string, unknown> = {
    code: `E2E-APS-OP-${tag}-${Date.now()}`,
    workOrderId: WORK_ORDER_ID,
    operationName: `E2E Op ${tag}`,
    sequence: 10,
    machineId: MACHINE_ID,
    priority: 10,
    setupTime: 0,
    runtimePerUnit: 10,
    qty: 3, // duration = setup(0) + perUnit(10) × qty(3) = 30 分钟
    earliestStartDateT: '2026-07-10T08:00:00',
    status: 'DRAFT',
  };
  if (opts.latestEnd) {
    data.latestEndDateT = opts.latestEnd;
  }
  return createViaSave(page, 'ErpApsOperationOrder', data, 'id status');
}

test.describe('aps ErpApsOperationOrder scheduling engine (scheduleForward/scheduleBackward)', () => {
  test('scheduleForward: DRAFT op → SchedulingResult.feasible + op PLANNED + plannedStart/End written back', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsOperationOrder-main');

    const schedule = await seedSchedule(page, 'fwd');
    const op = await seedOpOrder(page, 'fwd');
    expect(op.status, 'precondition op status=DRAFT').toBe('DRAFT');

    // scheduleForward 返回 SchedulingResult（@DataBean，GraphQL 暴露字段）
    const result = await callMutationOk(
      page, 'ErpApsOperationOrder', 'scheduleForward',
      { scheduleId: schedule.id },
      'feasible scheduledOperationIds',
    );
    expect(result.feasible, 'scheduleForward should report feasible=true').toBe(true);
    expect(Array.isArray(result.scheduledOperationIds), 'scheduledOperationIds should be array').toBe(true);
    expect(result.scheduledOperationIds.length, 'at least 1 op scheduled').toBeGreaterThan(0);

    // op 状态翻转 + plannedStart/End 写回（__get 独立断言）
    const v = await verifyState(page, 'ErpApsOperationOrder', op.id, 'status plannedStartDateT plannedEndDateT');
    expect(v.status, 'op status after scheduleForward = PLANNED').toBe('PLANNED');
    expect(v.plannedStartDateT, 'plannedStartDateT should be written back').not.toBeNull();
    expect(v.plannedEndDateT, 'plannedEndDateT should be written back').not.toBeNull();

    await deleteById(page, 'ErpApsOperationOrder', op.id);
    await deleteById(page, 'ErpApsSchedule', schedule.id);
  });

  test('scheduleBackward: DRAFT op with latestEndDateT → SchedulingResult feasible + op PLANNED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsOperationOrder-main');

    const schedule = await seedSchedule(page, 'bwd');
    const op = await seedOpOrder(page, 'bwd', { latestEnd: '2026-07-11T12:00:00' });
    expect(op.status, 'precondition op status=DRAFT').toBe('DRAFT');

    const result = await callMutationOk(
      page, 'ErpApsOperationOrder', 'scheduleBackward',
      { scheduleId: schedule.id },
      'feasible scheduledOperationIds',
    );
    expect(result.feasible, 'scheduleBackward should report feasible=true (deadline reachable)').toBe(true);
    expect(Array.isArray(result.scheduledOperationIds), 'scheduledOperationIds should be array').toBe(true);
    expect(result.scheduledOperationIds.length, 'at least 1 op scheduled').toBeGreaterThan(0);

    const v = await verifyState(page, 'ErpApsOperationOrder', op.id, 'status plannedStartDateT plannedEndDateT');
    expect(v.status, 'op status after scheduleBackward = PLANNED').toBe('PLANNED');
    expect(v.plannedStartDateT, 'plannedStartDateT should be written back').not.toBeNull();
    expect(v.plannedEndDateT, 'plannedEndDateT should be written back').not.toBeNull();

    await deleteById(page, 'ErpApsOperationOrder', op.id);
    await deleteById(page, 'ErpApsSchedule', schedule.id);
  });
});
