import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findItems, deleteByFilter, deleteById, eqFilter, GraphQLClient } from './_helper';

/**
 * drp DRP 仿真引擎业务动作浏览器层 E2E（plan 2026-07-26-0500-2 Phase 3）。
 *
 * 验证 runSimulation / compareVersions 经 GraphQL /graphql 的全栈可达性 + 场景状态机 +
 * COMPUTED plan 生成 + 版本对比 2 维 DTO 结构。同构 MRP spec（plan Phase 2）。
 *
 * 权威实现（ErpDrpScenarioBizModel 薄委派 → SimulationDrpEngine / DrpSimulationVersionComparator，
 * 对齐 docs/design/manufacturing/simulation-engine.md §DRP 对应物）：
 *   runSimulation(scenarioId) @BizMutation —— requireScenario(DRAFT) → 新建 COMPUTED plan →
 *     DrpDemandAggregator.aggregate 按 plan.orgId 加载 ErpDrpParameter → 场景覆盖 safetyStock/
 *     orderMultiple → fork DRP 净需求算法 → 写场景版本 + scenario status=COMPLETED。非 DRAFT 抛
 *     ERR_DRP_SIMULATION_SCENARIO_NOT_DRAFT（description token「不允许此操作」）。
 *   compareVersions(versionIdA, versionIdB) @BizQuery —— 须同 scenarioId → DrpSimulationDiffResult
 *     （2 维 diff: replenishmentQtyDelta + safetyStockDelta）。
 *
 * config-gate：`erp-drp.simulation-enabled` 默认 false；webServer JVM arg 追加 `=true` 启用。
 *
 * 隔离策略：自包含建测试专用物料（避免与 drp-plan-engine/drp-safety-stock 参数三元组冲突）+
 * ErpDrpParameter（safetyStock=10）+ baseline ErpDrpPlan + scenario + scenarioParam。物料无
 * stock_balance → sumAvailable=0（确定性：net=safetyStock）。种子 org id=2 + warehouse id=1 +
 * uom id=1（PCS）。DrpDemandAggregator.loadParametersInScope 按 plan.orgId 过滤——本 spec 用
 * orgId=2，会加载 org=2 所有 ErpDrpParameter；行级断言按 (test_mat, test_wh) 精确过滤。
 *
 * Code 长度约束：orderCode domain precision=50。baseline code 经 `-SIM-V{n}` (7) +
 * `-PROMOTED-{n}` (11) 累加（promote 不在本 spec 范围，但仍留余量）→ baseline ≤ 32 char。
 *
 * ParamResolver 缓存约束：同 MRP，浏览器层无法 invalidateCache → compareVersions 两版本同 params
 * （delta=0），断言降级为结构非空（精确 delta +20 由 JUnit testCompareVersionsProducesStructuredDiff
 * 覆盖）。
 *
 * 清理：COMPUTED plan lines + plan + scenarioParam + scenario + parameter + baseline plan +
 * test material（经 findItems 反查 + __delete）。
 */

const ORG_ID = 2; // 种子 ERP-CO（__save 强制 org FK 校验）
const WAREHOUSE_ID = 1; // WH-MAIN
const UOM_ID = 1; // PCS

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function seedMaterial(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMdMaterial',
    {
      code: uniq(`E2E-DM-${tag}`),
      name: `E2E DRP Sim Mat ${tag}`,
      materialType: 'GOODS',
      uoMId: UOM_ID,
      status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE',
      defaultWarehouseId: WAREHOUSE_ID,
    },
    'id',
  );
}

async function seedParameter(
  page: import('@playwright/test').Page,
  materialId: string | number,
  safetyStock: number,
  tag: string,
): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpParameter',
    {
      warehouseId: WAREHOUSE_ID,
      materialId,
      safetyStock,
      replenishmentMethod: 'LOT_FOR_LOT',
      orgId: ORG_ID,
    },
    'id',
  );
}

async function seedBaselinePlan(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpPlan',
    {
      code: uniq(`E2E-DP-${tag}`),
      planName: uniq(`E2E DRP Sim Plan ${tag}`),
      periodFrom: '2026-08-01',
      periodTo: '2026-08-31',
      businessDate: '2026-08-01',
      status: 'DRAFT',
      orgId: ORG_ID,
    },
    'id',
  );
}

async function seedScenario(
  page: import('@playwright/test').Page,
  basePlanId: string | number,
  tag: string,
): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpDrpScenario',
    {
      code: uniq(`E2E-DS-${tag}`),
      orgId: ORG_ID,
      baseDrpPlanId: basePlanId,
      status: 'DRAFT',
    },
    'id status',
  );
}

async function seedParam(
  page: import('@playwright/test').Page,
  scenarioId: string | number,
  paramType: string,
  value: number,
  materialId: string | number,
  warehouseId: string | number,
): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpScenarioParam',
    {
      scenarioId,
      materialId,
      warehouseId,
      paramType,
      paramValue: value,
    },
    'id',
  );
}

async function resetScenarioToDraft(
  page: import('@playwright/test').Page,
  scenarioId: string | number,
): Promise<void> {
  const gql = new GraphQLClient(page);
  await gql.raw(
    `mutation($id:ID!){ ErpDrpScenario__update(data:{id:$id, status:"DRAFT"}){ id status } }`,
    { id: String(scenarioId) },
  );
}

async function findLineByMaterialWarehouse(
  page: import('@playwright/test').Page,
  computedPlanId: string | number,
  materialId: string | number,
  warehouseId: string | number,
): Promise<{ suggestedQty: number; safetyStock: number; netRequirement: number } | null> {
  const items = await findItems(
    page, 'ErpDrpLine',
    eqFilter('planId', Number(computedPlanId)),
    'suggestedQty safetyStock netRequirement materialId warehouseId',
  );
  return items.find((it: any) =>
    Number(it.materialId) === Number(materialId) && Number(it.warehouseId) === Number(warehouseId),
  ) as any || null;
}

async function cleanupSimulation(
  page: import('@playwright/test').Page,
  ctx: {
    scenarioId: string | number;
    baselinePlanId: string | number;
    parameterId: string | number;
    materialId: string | number;
    paramIds?: (string | number)[];
  },
): Promise<void> {
  // 收集所有版本 → 每个 version 的 computedDrpPlanId + promotedPlanId
  const versions = await findItems(
    page, 'ErpDrpScenarioVersion',
    eqFilter('scenarioId', Number(ctx.scenarioId)),
    'id computedDrpPlanId promotedPlanId',
  );
  // 删 lines + plans（computed + promoted）
  for (const v of versions as any[]) {
    if (v.computedDrpPlanId) {
      await deleteByFilter(page, 'ErpDrpLine', eqFilter('planId', Number(v.computedDrpPlanId)));
      await deleteById(page, 'ErpDrpPlan', v.computedDrpPlanId);
    }
    if (v.promotedPlanId) {
      await deleteByFilter(page, 'ErpDrpLine', eqFilter('planId', Number(v.promotedPlanId)));
      await deleteById(page, 'ErpDrpPlan', v.promotedPlanId);
    }
  }
  // 删 versions
  await deleteByFilter(page, 'ErpDrpScenarioVersion', eqFilter('scenarioId', Number(ctx.scenarioId)));
  // 删 scenarioParams
  if (ctx.paramIds && ctx.paramIds.length > 0) {
    for (const pid of ctx.paramIds) {
      await deleteById(page, 'ErpDrpScenarioParam', pid);
    }
  } else {
    await deleteByFilter(page, 'ErpDrpScenarioParam', eqFilter('scenarioId', Number(ctx.scenarioId)));
  }
  // 删 scenario
  await deleteById(page, 'ErpDrpScenario', ctx.scenarioId);
  // 删 ErpDrpParameter（场景驱动参数，非种子）
  await deleteById(page, 'ErpDrpParameter', ctx.parameterId);
  // 删 baseline plan lines (empty for DRAFT) + plan
  await deleteByFilter(page, 'ErpDrpLine', eqFilter('planId', Number(ctx.baselinePlanId)));
  await deleteById(page, 'ErpDrpPlan', ctx.baselinePlanId);
  // 删 test material
  await deleteById(page, 'ErpMdMaterial', ctx.materialId);
}

test.describe('drp DRP simulation engine browser-layer E2E', () => {
  test('runSimulation: SAFETY_STOCK override produces COMPUTED plan with observably-changed suggestedQty', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpScenario-main');

    const mat = await seedMaterial(page, 'SS');
    const param = await seedParameter(page, mat.id, 10, 'SS'); // baseline safetyStock=10
    const baseline = await seedBaselinePlan(page, 'SS');
    const scenario = await seedScenario(page, baseline.id, 'SS');
    expect(scenario.status, 'new scenario status=DRAFT').toBe('DRAFT');

    // scenario SAFETY_STOCK override=20（高于 baseline 10）
    const scenarioParam = await seedParam(
      page, scenario.id, 'SAFETY_STOCK', 20, mat.id, WAREHOUSE_ID,
    );

    // runSimulation: DRAFT → COMPLETED + COMPUTED plan line for (test_mat, wh1) suggested=20
    // 公式：net = safetyStock(override=20) - currentStock(0) + allocated(0) - onOrder(0) + forecast(0) = 20
    const version = await callMutationOk(
      page, 'ErpDrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id scenarioId versionNo computedDrpPlanId status',
    );
    expect(version.status, 'version status=COMPLETED').toBe('COMPLETED');
    expect(version.computedDrpPlanId, 'computedDrpPlanId set').toBeTruthy();

    const s = await verifyState(page, 'ErpDrpScenario', scenario.id, 'status');
    expect(s.status, 'scenario status=COMPLETED after runSimulation').toBe('COMPLETED');

    // COMPUTED plan line for (test_mat, wh1): suggestedQty=20 (override SS=20, no stock)
    const line = await findLineByMaterialWarehouse(page, version.computedDrpPlanId, mat.id, WAREHOUSE_ID);
    expect(line, 'COMPUTED plan has line for (test_mat, wh1)').toBeTruthy();
    expect(Number(line!.suggestedQty), 'SAFETY_STOCK override=20 → suggested=20').toBe(20);
    expect(Number(line!.safetyStock), 'line.safetyStock=20 (override applied)').toBe(20);
    expect(Number(line!.netRequirement), 'netRequirement=20').toBe(20);

    // 非法迁移守卫：COMPLETED scenario 重跑 runSimulation → ERR_DRP_SIMULATION_SCENARIO_NOT_DRAFT
    const rej = await callMutation(
      page, 'ErpDrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id',
    );
    expect(rej.errors, 'non-DRAFT runSimulation should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token')
      .toContain('不允许此操作');

    // 清理
    await cleanupSimulation(page, {
      scenarioId: scenario.id,
      baselinePlanId: baseline.id,
      parameterId: param.id,
      materialId: mat.id,
      paramIds: [scenarioParam.id],
    });
  });

  test('compareVersions: 2-dim structured diff (lineDiffs non-empty + replenishmentQtyDelta field present)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpScenario-main');

    const mat = await seedMaterial(page, 'CMP');
    const param = await seedParameter(page, mat.id, 10, 'CMP');
    const baseline = await seedBaselinePlan(page, 'CMP');
    const scenario = await seedScenario(page, baseline.id, 'CMP');
    const scenarioParam = await seedParam(
      page, scenario.id, 'SAFETY_STOCK', 20, mat.id, WAREHOUSE_ID,
    );

    // v1: runSimulation → COMPLETED
    const v1 = await callMutationOk(
      page, 'ErpDrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id versionNo computedDrpPlanId status',
    );
    expect(v1.status, 'v1 status=COMPLETED').toBe('COMPLETED');

    // 重置 scenario 为 DRAFT
    await resetScenarioToDraft(page, scenario.id);

    // ParamResolver 缓存约束：浏览器层无法 invalidateCache → v2 同 params
    // 此用例验证 compareVersions 浏览器层可达 + 2 维 DTO 结构，精确 delta 由 JUnit 覆盖
    const v2 = await callMutationOk(
      page, 'ErpDrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id versionNo computedDrpPlanId status',
    );
    expect(v2.status, 'v2 status=COMPLETED').toBe('COMPLETED');
    expect(Number(v2.versionNo), 'v2 versionNo=2').toBe(2);

    // compareVersions(v1, v2) via @BizQuery — DrpSimulationDiffResult 2 维 diff
    const gql = new GraphQLClient(page);
    const diffJson: any = await gql.raw(
      `query($a:ID!, $b:ID!){ ErpDrpScenario__compareVersions(versionIdA:$a, versionIdB:$b){
        versionIdA versionIdB
        lineDiffs{ materialId warehouseId suggestedQtyA suggestedQtyB replenishmentQtyDelta safetyStockA safetyStockB safetyStockDelta }
        totalReplenishmentQtyDelta totalSafetyStockDelta
      } }`,
      { a: String(v1.id), b: String(v2.id) },
    );
    expect(diffJson?.errors, 'compareVersions should not return errors').toBeFalsy();
    const diff = diffJson?.data?.ErpDrpScenario__compareVersions;
    expect(diff, 'DrpSimulationDiffResult returned').toBeTruthy();
    expect(String(diff.versionIdA), 'versionIdA matches v1').toBe(String(v1.id));
    expect(String(diff.versionIdB), 'versionIdB matches v2').toBe(String(v2.id));
    expect(Array.isArray(diff.lineDiffs), 'lineDiffs is array').toBe(true);
    expect(diff.lineDiffs.length, 'lineDiffs non-empty').toBeGreaterThan(0);
    // 2 维 diff 聚合字段可达
    expect(diff, 'totalReplenishmentQtyDelta field present').toHaveProperty('totalReplenishmentQtyDelta');
    expect(diff, 'totalSafetyStockDelta field present').toHaveProperty('totalSafetyStockDelta');

    // 行级 diff 中能定位到 (test_mat, wh1) 行
    const matLineDiff = (diff.lineDiffs as any[]).find(
      (d) => String(d.materialId) === String(mat.id) && String(d.warehouseId) === String(WAREHOUSE_ID),
    );
    expect(matLineDiff, 'lineDiffs contains (test_mat, wh1) entry').toBeTruthy();
    expect(Number(matLineDiff.suggestedQtyA), 'v1 suggestedQty=20').toBe(20);
    expect(Number(matLineDiff.suggestedQtyB), 'v2 suggestedQty=20 (same params, cache)').toBe(20);

    // 清理
    await cleanupSimulation(page, {
      scenarioId: scenario.id,
      baselinePlanId: baseline.id,
      parameterId: param.id,
      materialId: mat.id,
      paramIds: [scenarioParam.id],
    });
  });
});
