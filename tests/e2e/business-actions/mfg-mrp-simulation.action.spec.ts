import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findFirst, findItems, deleteByFilter, deleteById, eqFilter, GraphQLClient } from './_helper';

/**
 * manufacturing MRP 仿真引擎业务动作浏览器层 E2E（plan 2026-07-26-0500-2 Phase 2）。
 *
 * 验证 runSimulation / compareVersions / promoteToFormalPlan 经 GraphQL /graphql 的全栈可达性 +
 * 场景状态机 + COMPUTED plan 生成 + 版本对比 DTO 结构 + 转正 DRAFT plan 生成。
 *
 * 权威实现（ErpMfgMrpScenarioBizModel 薄委派 → SimulationMrpEngine / SimulationVersionComparator，
 * 对齐 docs/design/manufacturing/simulation-engine.md）：
 *   runSimulation(scenarioId) @BizMutation —— requireScenario(DRAFT) → 新建 COMPUTED plan →
 *     从基线 plan 加载 demands → 内存中 SAFETY_STOCK + LOT_SIZE 覆盖重算 → 写场景版本 +
 *     scenario status=COMPLETED。非 DRAFT 抛 ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT
 *     （description token「不允许此操作」）。
 *   compareVersions(versionIdA, versionIdB) @BizQuery —— 须同 scenarioId 否则
 *     ERR_MFG_SIMULATION_VERSIONS_NOT_COMPARABLE → SimulationDiffResult（4 维 diff + 缺料集）。
 *   promoteToFormalPlan(scenarioVersionId) @BizMutation —— COMPLETED 版本 → DRAFT plan（code 后缀
 *     -PROMOTED-{versionNo}）+ 行复制 + 版本 ARCHIVED + promotedPlanId 回写。重复抛
 *     ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED。
 *
 * config-gate：`erp-mfg.simulation-enabled` 默认 false；webServer JVM arg 追加 `=true` 启用。
 *
 * 隔离策略：自包含建测试专用物料（避免污染种子 MRP 数据；种子无 MRP plan/demand CSV）+
 * baseline ErpMfgMrpPlan（DRAFT）+ ErpMfgMrpDemand（MANUAL qty=25）+ scenario + scenarioParam。
 * 物料 safetyStock 默认 null → 仿真 applySafetyStockOverride 不为本物料产 SAFETY_STOCK demand
 * （确定性：net=demandQty=25）。种子 org id=2（FK 校验）+ uom id=1（PCS）。
 *
 * Code 长度约束：workOrderCode domain precision=50。baseline code 经 `-SIM-V{n}` (7) +
 * `-PROMOTED-{n}` (11) 累加，须 ≤ 50 → baseline ≤ 32 char。本 spec 用短前缀 `E2E-M-` + 时间戳。
 *
 * ParamResolver 缓存约束：进程内 Map<scenarioId, params> 首次加载不刷新（设计 AP-06）。浏览器层
 * 无法 invalidateCache → compareVersions 两版本同 params（delta=0），断言降级为结构非空 +
 * shortageInBoth 含 test_mat（精确 delta +10 由 JUnit testCompareVersionsProducesStructuredDiff 覆盖）。
 *
 * 清理：COMPUTED/PROMOTED plan lines + plans + scenarioParam + scenario + demand + baseline plan +
 * test material（经 findItems 反查 + __delete）。避免 -PROMOTED- plan 污染 MRP 基线。
 */

const ORG_ID = 2; // 种子 ERP-CO（__save 强制 org FK 校验）
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
      code: uniq(`E2E-MM-${tag}`),
      name: `E2E MFG Sim Mat ${tag}`,
      materialType: 'GOODS',
      uoMId: UOM_ID,
      status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE',
      defaultWarehouseId: 1,
    },
    'id',
  );
}

async function seedBaselinePlan(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMfgMrpPlan',
    {
      code: uniq(`E2E-MP-${tag}`),
      orgId: ORG_ID,
      businessDate: '2026-08-01',
      planningHorizonDays: 30,
      status: 'DRAFT',
    },
    'id',
  );
}

async function seedDemand(
  page: import('@playwright/test').Page,
  planId: string | number,
  materialId: string | number,
  qty: number,
  tag: string,
): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMfgMrpDemand',
    {
      mrpPlanId: planId,
      lineNo: 10,
      materialId,
      uoMId: UOM_ID,
      demandSource: 'MANUAL',
      sourceBillType: 'MRP_MANUAL',
      sourceBillCode: uniq(`E2E-MD-${tag}`),
      quantity: qty,
      requirementDate: '2026-08-15',
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
    page, 'ErpMfgMrpScenario',
    {
      code: uniq(`E2E-MS-${tag}`),
      orgId: ORG_ID,
      baseMrpPlanId: basePlanId,
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
  materialId?: string | number,
): Promise<{ id: string }> {
  const data: Record<string, unknown> = {
    scenarioId,
    paramType,
    paramValue: value,
  };
  if (materialId !== undefined) {
    data.materialId = materialId;
  }
  return createViaSave(page, 'ErpMfgMrpScenarioParam', data, 'id');
}

async function resetScenarioToDraft(
  page: import('@playwright/test').Page,
  scenarioId: string | number,
): Promise<void> {
  const gql = new GraphQLClient(page);
  await gql.raw(
    `mutation($id:ID!){ ErpMfgMrpScenario__update(data:{id:$id, status:"DRAFT"}){ id status } }`,
    { id: String(scenarioId) },
  );
}

async function findLineByMaterial(
  page: import('@playwright/test').Page,
  computedPlanId: string | number,
  materialId: string | number,
): Promise<{ plannedQuantity: number; netRequirement: number; materialId: number } | null> {
  const items = await findItems(
    page, 'ErpMfgMrpPlanLine',
    eqFilter('mrpPlanId', Number(computedPlanId)),
    'plannedQuantity netRequirement materialId',
  );
  return items.find((it: any) => Number(it.materialId) === Number(materialId)) as any || null;
}

async function cleanupSimulation(
  page: import('@playwright/test').Page,
  ctx: {
    scenarioId: string | number;
    baselinePlanId: string | number;
    demandId?: string | number;
    materialId: string | number;
    paramIds?: (string | number)[];
  },
): Promise<void> {
  // 收集所有版本 → 每个 version 的 computedMrpPlanId + promotedPlanId
  const versions = await findItems(
    page, 'ErpMfgMrpScenarioVersion',
    eqFilter('scenarioId', Number(ctx.scenarioId)),
    'id computedMrpPlanId promotedPlanId',
  );
  // 删 plan lines + plans（computed + promoted）
  for (const v of versions as any[]) {
    if (v.computedMrpPlanId) {
      await deleteByFilter(page, 'ErpMfgMrpPlanLine', eqFilter('mrpPlanId', Number(v.computedMrpPlanId)));
      await deleteById(page, 'ErpMfgMrpPlan', v.computedMrpPlanId);
    }
    if (v.promotedPlanId) {
      await deleteByFilter(page, 'ErpMfgMrpPlanLine', eqFilter('mrpPlanId', Number(v.promotedPlanId)));
      await deleteById(page, 'ErpMfgMrpPlan', v.promotedPlanId);
    }
  }
  // 删 versions
  await deleteByFilter(page, 'ErpMfgMrpScenarioVersion', eqFilter('scenarioId', Number(ctx.scenarioId)));
  // 删 params
  if (ctx.paramIds && ctx.paramIds.length > 0) {
    for (const pid of ctx.paramIds) {
      await deleteById(page, 'ErpMfgMrpScenarioParam', pid);
    }
  } else {
    await deleteByFilter(page, 'ErpMfgMrpScenarioParam', eqFilter('scenarioId', Number(ctx.scenarioId)));
  }
  // 删 scenario
  await deleteById(page, 'ErpMfgMrpScenario', ctx.scenarioId);
  // 删 demand
  if (ctx.demandId) {
    await deleteById(page, 'ErpMfgMrpDemand', ctx.demandId);
  }
  // 删 baseline plan lines (empty) + plan
  await deleteByFilter(page, 'ErpMfgMrpPlanLine', eqFilter('mrpPlanId', Number(ctx.baselinePlanId)));
  await deleteById(page, 'ErpMfgMrpPlan', ctx.baselinePlanId);
  // 删 test material
  await deleteById(page, 'ErpMdMaterial', ctx.materialId);
}

test.describe('manufacturing MRP simulation engine browser-layer E2E', () => {
  test('runSimulation: LOT_SIZE override produces COMPUTED plan with ceil-rounded plannedQuantity + non-DRAFT guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgMrpScenario-main');

    const mat = await seedMaterial(page, 'LOT');
    const baseline = await seedBaselinePlan(page, 'LOT');
    const demand = await seedDemand(page, baseline.id, mat.id, 25, 'LOT');
    const scenario = await seedScenario(page, baseline.id, 'LOT');
    expect(scenario.status, 'new scenario status=DRAFT').toBe('DRAFT');

    const param = await seedParam(page, scenario.id, 'LOT_SIZE', 10); // global LOT_SIZE=10

    // runSimulation: DRAFT → COMPLETED + COMPUTED plan line for test_mat planned=ceil(25/10)*10=30
    const version = await callMutationOk(
      page, 'ErpMfgMrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id scenarioId versionNo computedMrpPlanId status',
    );
    expect(version.status, 'version status=COMPLETED').toBe('COMPLETED');
    expect(version.computedMrpPlanId, 'computedMrpPlanId set').toBeTruthy();

    const s = await verifyState(page, 'ErpMfgMrpScenario', scenario.id, 'status');
    expect(s.status, 'scenario status=COMPLETED after runSimulation').toBe('COMPLETED');

    // COMPUTED plan line for test_mat: planned=30 (ceil(25/10)*10)
    const line = await findLineByMaterial(page, version.computedMrpPlanId, mat.id);
    expect(line, 'COMPUTED plan has line for test_mat').toBeTruthy();
    expect(Number(line!.plannedQuantity), 'LOT_SIZE=10 → planned=ceil(25/10)*10=30').toBe(30);
    expect(Number(line!.netRequirement), 'netRequirement=25 (no stock, no safetyStock)').toBe(25);

    // 非法迁移守卫：COMPLETED scenario 重跑 runSimulation → ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT
    // GraphQL 返回 errors[].message 为中文描述（NopException.description），含 token「不允许此操作」
    const rej = await callMutation(
      page, 'ErpMfgMrpScenario', 'runSimulation',
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
      demandId: demand.id,
      materialId: mat.id,
      paramIds: [param.id],
    });
  });

  test('compareVersions: structured diff (lineDiffs non-empty + shortageInBoth contains test_mat)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgMrpScenario-main');

    const mat = await seedMaterial(page, 'CMP');
    const baseline = await seedBaselinePlan(page, 'CMP');
    const demand = await seedDemand(page, baseline.id, mat.id, 25, 'CMP');
    const scenario = await seedScenario(page, baseline.id, 'CMP');
    const param = await seedParam(page, scenario.id, 'LOT_SIZE', 10);

    // v1: runSimulation → COMPLETED
    const v1 = await callMutationOk(
      page, 'ErpMfgMrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id versionNo computedMrpPlanId status',
    );
    expect(v1.status, 'v1 status=COMPLETED').toBe('COMPLETED');

    // 重置 scenario 为 DRAFT（runSimulation 后场景 COMPLETED，compareVersions 需两版本）
    await resetScenarioToDraft(page, scenario.id);

    // ParamResolver 缓存约束：浏览器层无法 invalidateCache → v2 同 params（planned 相同）
    // 此用例验证 compareVersions 浏览器层可达 + DTO 结构，精确 delta 由 JUnit testCompareVersionsProducesStructuredDiff 覆盖
    const v2 = await callMutationOk(
      page, 'ErpMfgMrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id versionNo computedMrpPlanId status',
    );
    expect(v2.status, 'v2 status=COMPLETED').toBe('COMPLETED');
    expect(Number(v2.versionNo), 'v2 versionNo=2').toBe(2);

    // compareVersions(v1, v2) via @BizQuery
    const gql = new GraphQLClient(page);
    const diffJson: any = await gql.raw(
      `query($a:ID!, $b:ID!){ ErpMfgMrpScenario__compareVersions(versionIdA:$a, versionIdB:$b){
        versionIdA versionIdB
        lineDiffs{ materialId plannedQuantityA plannedQuantityB plannedQuantityDelta }
        totalPlannedQuantityDelta
        shortageOnlyInA shortageOnlyInB shortageInBoth
      } }`,
      { a: String(v1.id), b: String(v2.id) },
    );
    expect(diffJson?.errors, 'compareVersions should not return errors').toBeFalsy();
    const diff = diffJson?.data?.ErpMfgMrpScenario__compareVersions;
    expect(diff, 'SimulationDiffResult returned').toBeTruthy();
    expect(String(diff.versionIdA), 'versionIdA matches v1').toBe(String(v1.id));
    expect(String(diff.versionIdB), 'versionIdB matches v2').toBe(String(v2.id));
    expect(Array.isArray(diff.lineDiffs), 'lineDiffs is array').toBe(true);
    expect(diff.lineDiffs.length, 'lineDiffs non-empty').toBeGreaterThan(0);
    expect(Array.isArray(diff.shortageInBoth), 'shortageInBoth is array').toBe(true);
    expect(
      (diff.shortageInBoth as any[]).map(String).includes(String(mat.id)),
      'shortageInBoth contains test_mat (net=25>0 in both versions)',
    ).toBe(true);

    // 清理
    await cleanupSimulation(page, {
      scenarioId: scenario.id,
      baselinePlanId: baseline.id,
      demandId: demand.id,
      materialId: mat.id,
      paramIds: [param.id],
    });
  });

  test('promoteToFormalPlan: COMPLETED version → DRAFT plan + ARCHIVED + repeated guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgMrpScenario-main');

    const mat = await seedMaterial(page, 'PRM');
    const baseline = await seedBaselinePlan(page, 'PRM');
    const demand = await seedDemand(page, baseline.id, mat.id, 25, 'PRM');
    const scenario = await seedScenario(page, baseline.id, 'PRM');
    const param = await seedParam(page, scenario.id, 'LOT_SIZE', 10);

    const version = await callMutationOk(
      page, 'ErpMfgMrpScenario', 'runSimulation',
      { scenarioId: scenario.id },
      'id versionNo',
    );

    // promoteToFormalPlan → DRAFT plan + 行复制 + 版本 ARCHIVED
    const promoted = await callMutationOk(
      page, 'ErpMfgMrpScenario', 'promoteToFormalPlan',
      { scenarioVersionId: version.id },
      'id code status',
    );
    expect(promoted.status, 'promoted plan status=DRAFT').toBe('DRAFT');
    expect(promoted.code, 'promoted code contains -PROMOTED-{versionNo}').toContain('-PROMOTED-');

    // 版本 status=ARCHIVED + promotedPlanId 回写
    const reloaded = await verifyState(
      page, 'ErpMfgMrpScenarioVersion', version.id,
      'status promotedPlanId',
    );
    expect(reloaded.status, 'version status=ARCHIVED after promote').toBe('ARCHIVED');
    expect(String(reloaded.promotedPlanId), 'promotedPlanId points to new plan')
      .toBe(String(promoted.id));

    // promoted plan 含复制行（test_mat planned=30）
    const promotedLine = await findLineByMaterial(page, promoted.id, mat.id);
    expect(promotedLine, 'promoted plan has copied line for test_mat').toBeTruthy();
    expect(Number(promotedLine!.plannedQuantity), 'promoted line planned=30 (same as COMPUTED)')
      .toBe(30);

    // 重复 promote 守卫 → ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED
    // GraphQL 返回 errors[].message 为中文描述，含 token「已转正」（ALREADY_PROMOTED description）
    const rej = await callMutation(
      page, 'ErpMfgMrpScenario', 'promoteToFormalPlan',
      { scenarioVersionId: version.id },
      'id',
    );
    expect(rej.errors, 'repeated promote should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry already-promoted token')
      .toContain('已转正');

    // 清理（promoted plan + lines 由 cleanupSimulation 通过版本 promotedPlanId 反查删除）
    await cleanupSimulation(page, {
      scenarioId: scenario.id,
      baselinePlanId: baseline.id,
      demandId: demand.id,
      materialId: mat.id,
      paramIds: [param.id],
    });
  });
});
