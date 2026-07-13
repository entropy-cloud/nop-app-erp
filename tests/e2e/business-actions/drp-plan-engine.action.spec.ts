import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, eqFilter, findPageTotal, deleteByFilter, deleteById } from './_helper';

/**
 * drp ErpDrpPlan 净需求计算引擎业务动作浏览器层 E2E（plan 2026-07-14-0215-2 Phase 2）。
 *
 * 验证 runDrp / resetToDraft / approvePlan 经 GraphQL /graphql 的全栈可达性 + 状态机迁移。
 *
 * 权威状态机（ErpDrpPlanBizModel 委派 DrpEngine，对齐 docs/design/drp/state-machine.md）：
 *   DRAFT --runDrp(须 DRAFT)--> COMPUTED --approvePlan(须 COMPUTED)--> APPROVED
 *   resetToDraft: COMPUTED|APPROVED → DRAFT（清除 SUGGESTED 行）
 *   净需求公式：net = safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty；
 *   无历史/在途/预测时 net = safetyStock（>0 即产 SUGGESTED 行 + suggestedQty）。
 *   非法迁移抛 ERR_DRP_PLAN_ILLEGAL_TRANSITION（message token「不允许此操作」）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 隔离策略：DrpDemandAggregator.loadParametersInScope 按 plan.orgId 过滤参数行。本 spec 用种子
 *   org id=2（__save 强制 FK 校验，须真实 org）+ 自包含 cleanup（每用例末删 Parameter），保证 runDrp
 *   仅聚合本用例自建的 ErpDrpParameter，确定性地产出净需求行。物料/仓库选 materialId=4/warehouseId=1
 *   （种子无该组合 stock_balance/PO/transfer/forecast → currentStock=allocated=onOrder=forecast=0；
 *   safetyStock=100 → net=100 > 0）。行级断言按 planId 隔离（findPageTotal > 0 容错同 org 他参）。
 *
 * 自包含 setup：建 ErpDrpParameter（safetyStock=100 → net=100）+ ErpDrpPlan（DRAFT）。
 * 清理：删 DrpLine（planId 隔离）+ Parameter + Plan。
 */

const MATERIAL_ID = 4; // MAT-004 包装（种子无 stock_balance/PO/forecast）
const WAREHOUSE_ID = 1; // WH-MAIN
const ORG_ID = 2; // 种子 ERP-CO（__save 强制 org FK 校验）

async function seedParameter(page: import('@playwright/test').Page, orgId: number, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpParameter',
    {
      warehouseId: WAREHOUSE_ID,
      materialId: MATERIAL_ID,
      safetyStock: 100,
      replenishmentMethod: 'LOT_FOR_LOT',
      orgId,
    },
    'id',
  );
}

async function seedPlan(page: import('@playwright/test').Page, orgId: number, tag: string): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpDrpPlan',
    {
      code: `E2E-DRP-PLAN-${tag}-${Date.now()}`,
      planName: `E2E DRP Plan ${tag}`,
      periodFrom: '2026-07-01',
      periodTo: '2026-07-31',
      status: 'DRAFT',
      orgId,
    },
    'id status',
  );
}

test.describe('drp ErpDrpPlan net-requirement engine state machine', () => {
  test('engine happy path: runDrp(DRAFT→COMPUTED + line) → resetToDraft(COMPUTED→DRAFT + lines cleared) → runDrp → approvePlan(COMPUTED→APPROVED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpPlan-main');

    const ORG = ORG_ID;
    const param = await seedParameter(page, ORG, 'hp');
    const plan = await seedPlan(page, ORG, 'hp');
    expect(plan.status, 'new plan status=DRAFT').toBe('DRAFT');

    // runDrp: DRAFT → COMPUTED + ErpDrpLine 净需求行非空（计划订单生成）
    await callMutationOk(page, 'ErpDrpPlan', 'runDrp', { planId: plan.id }, 'id');
    let s = await verifyState(page, 'ErpDrpPlan', plan.id, 'status totalReplenishmentQty');
    expect(s.status, 'after runDrp status=COMPUTED').toBe('COMPUTED');
    expect(Number(s.totalReplenishmentQty), 'totalReplenishmentQty>0 (net=safetyStock=100)').toBeGreaterThan(0);
    let lineTotal = await findPageTotal(page, 'ErpDrpLine', eqFilter('planId', Number(plan.id)));
    expect(lineTotal, 'runDrp should produce SUGGESTED DrpLine rows').toBeGreaterThan(0);

    // resetToDraft: COMPUTED → DRAFT + SUGGESTED 行清理
    await callMutationOk(page, 'ErpDrpPlan', 'resetToDraft', { planId: plan.id }, 'id');
    s = await verifyState(page, 'ErpDrpPlan', plan.id, 'status totalReplenishmentQty');
    expect(s.status, 'after resetToDraft status=DRAFT').toBe('DRAFT');
    lineTotal = await findPageTotal(page, 'ErpDrpLine', eqFilter('planId', Number(plan.id)));
    expect(lineTotal, 'resetToDraft should clear SUGGESTED lines').toBe(0);

    // 重算 runDrp: DRAFT → COMPUTED（调参后重算场景）
    await callMutationOk(page, 'ErpDrpPlan', 'runDrp', { planId: plan.id }, 'id');
    s = await verifyState(page, 'ErpDrpPlan', plan.id, 'status');
    expect(s.status, 'after re-runDrp status=COMPUTED').toBe('COMPUTED');

    // approvePlan: COMPUTED → APPROVED（SUGGESTED 行 → APPROVED）
    await callMutationOk(page, 'ErpDrpPlan', 'approvePlan', { planId: plan.id }, 'id');
    s = await verifyState(page, 'ErpDrpPlan', plan.id, 'status');
    expect(s.status, 'after approvePlan status=APPROVED').toBe('APPROVED');

    // 清理：DrpLine（planId 隔离）+ Parameter + Plan
    await deleteByFilter(page, 'ErpDrpLine', eqFilter('planId', Number(plan.id)));
    await deleteById(page, 'ErpDrpParameter', param.id);
    await deleteById(page, 'ErpDrpPlan', plan.id);
  });

  test('illegal transition guard: approvePlan from DRAFT rejected (ERR_DRP_PLAN_ILLEGAL_TRANSITION, requires COMPUTED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpPlan-main');

    const ORG = ORG_ID;
    const param = await seedParameter(page, ORG, 'gd');
    const plan = await seedPlan(page, ORG, 'gd');
    expect(plan.status, 'new plan status=DRAFT').toBe('DRAFT');

    // DRAFT → approvePlan（须 COMPUTED）：经 GraphQL 返回 errors
    const rej = await callMutation(page, 'ErpDrpPlan', 'approvePlan', { planId: plan.id }, 'id');
    expect(rej.errors, 'approvePlan from DRAFT should be rejected (requires COMPUTED)').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许此操作');

    // 清理
    await deleteById(page, 'ErpDrpParameter', param.id);
    await deleteById(page, 'ErpDrpPlan', plan.id);
  });
});
