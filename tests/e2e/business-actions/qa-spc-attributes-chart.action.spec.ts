import { test, expect, loginAndNavigate, createViaSave, verifyState, deleteById, GraphQLClient } from './_helper';

/**
 * quality ErpQaSpcChart 计数型（P/NP/C/U）控制图业务动作浏览器层 E2E（plan 2026-07-19-0120-2 Phase 4）。
 *
 * 验证 ErpQaSpcChart__recalculateControlLimit @BizMutation 对 4 计数型 chartType 的全栈可达：
 *   - P chart recalculate → calcStatus=CALCULATED + cl/ucl/lcl 非空 + UCL ≥ CL ≥ LCL 不变量
 *   - NP chart recalculate → 同上
 *   - C chart recalculate → 同上
 *   - U chart recalculate → 同上
 *
 * mutation 名实测核实：ErpQaSpcChartBizModel.java:69 `recalculateControlLimit`（iter-1 审查 B4 核实）。
 * 种子引用：materialId=1 / parameterId=1（既有种子参数占位）。
 * 清理：删除 E2E 创建的 chart + 关联 sample（cascade-delete）。
 */

const MATERIAL_ID = 1;
const PARAMETER_ID = 1;
const BDATE = '2026-07-19';

interface SpcChartSeed {
  id: string;
  chartType: string;
}

async function seedAttributesChart(
  page: import('@playwright/test').Page,
  tag: string,
  chartType: string,
): Promise<SpcChartSeed> {
  return createViaSave(
    page, 'ErpQaSpcChart',
    {
      code: `E2E-SPC-${tag}-${Date.now()}`,
      name: `E2E SPC ${tag}`,
      chartType,
      materialId: MATERIAL_ID,
      parameterId: PARAMETER_ID,
      subgroupSize: 2,
      clCenterType: 'AUTO_FROM_DATA',
      ruleSet: '1,2,3,4',
      calcStatus: 'PENDING',
      isActive: true,
      docStatus: 'ACTIVE',
      approveStatus: 'APPROVED',
      remark: `E2E SPC ${tag}`,
    },
    'id chartType',
  );
}

async function seedSample(
  page: import('@playwright/test').Page,
  chartId: string,
  subgroupNo: number,
  defectCount: number,
  inspectedCount: number,
): Promise<void> {
  // 经 __save 直接建计数型 sample（绕过 samplingService 集成测试链路）
  await createViaSave(
    page, 'ErpQaSpcSample',
    {
      chartId: Number(chartId),
      subgroupNo,
      sampleTime: `${BDATE} 10:0${subgroupNo % 10}:00`,
      defectCount,
      inspectedCount,
      mean: inspectedCount > 0 ? Number((defectCount / inspectedCount).toFixed(6)) : 0,
      range: 0,
      stdDev: 0,
      sourceBillType: 'ERP_QA_INSPECTION',
      sourceCode: `E2E-SAMPLE-${subgroupNo}`,
      sourceLineCode: '1',
      isOutOfControl: false,
    },
    'id',
  );
}

test.describe('quality ErpQaSpcChart 计数型 recalculateControlLimit（P/NP/C/U）', () => {
  test('P chart: recalculate → CALCULATED + UCL ≥ CL ≥ LCL', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaSpcChart-main');
    const chart = await seedAttributesChart(page, 'P', 'P');
    // 撒 20 子组（满足 SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT=20）
    for (let i = 1; i <= 20; i++) {
      await seedSample(page, chart.id, i, i % 5, 100); // defect=0..4, inspected=100
    }

    // mutation 返回 Boolean（非实体），用 raw 调用避免字段选择
    const json: any = await new GraphQLClient(page).raw(
      `mutation{ ErpQaSpcChart__recalculateControlLimit(chartId:${chart.id}) }`,
    );
    expect(json.errors ?? null, `recalculateControlLimit should not error: ${JSON.stringify(json.errors)}`).toBeNull();
    expect(json.data?.ErpQaSpcChart__recalculateControlLimit, 'recalculate returns true').toBe(true);

    const s = await verifyState(page, 'ErpQaSpcChart', chart.id, 'calcStatus cl ucl lcl');
    expect(s.calcStatus, 'after recalculate calcStatus=CALCULATED').toBe('CALCULATED');
    expect(s.cl, 'CL non-null').not.toBeNull();
    expect(s.ucl, 'UCL non-null').not.toBeNull();
    expect(s.lcl, 'LCL non-null').not.toBeNull();
    expect(Number(s.ucl), 'UCL >= CL').toBeGreaterThanOrEqual(Number(s.cl));
    expect(Number(s.cl), 'CL >= LCL').toBeGreaterThanOrEqual(Number(s.lcl));
    // P 图缺陷率 CL 应在 0~1 之间
    expect(Number(s.cl)).toBeGreaterThan(0);
    expect(Number(s.cl)).toBeLessThan(1);

    await deleteById(page, 'ErpQaSpcChart', chart.id);
  });

  test('NP chart: recalculate → CALCULATED + UCL ≥ CL ≥ LCL', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaSpcChart-main');
    const chart = await seedAttributesChart(page, 'NP', 'NP');
    for (let i = 1; i <= 20; i++) {
      await seedSample(page, chart.id, i, i % 5, 100);
    }

    const json: any = await new GraphQLClient(page).raw(
      `mutation{ ErpQaSpcChart__recalculateControlLimit(chartId:${chart.id}) }`,
    );
    expect(json.errors ?? null).toBeNull();
    expect(json.data?.ErpQaSpcChart__recalculateControlLimit).toBe(true);

    const s = await verifyState(page, 'ErpQaSpcChart', chart.id, 'calcStatus cl ucl lcl');
    expect(s.calcStatus).toBe('CALCULATED');
    expect(s.cl).not.toBeNull();
    expect(s.ucl).not.toBeNull();
    expect(Number(s.ucl)).toBeGreaterThanOrEqual(Number(s.cl));
    expect(Number(s.cl)).toBeGreaterThanOrEqual(Number(s.lcl));
    // NP 图 CL = n·p̄ ∈ [0, n=100]
    expect(Number(s.cl)).toBeGreaterThan(0);
    expect(Number(s.cl)).toBeLessThan(100);

    await deleteById(page, 'ErpQaSpcChart', chart.id);
  });

  test('C chart: recalculate → CALCULATED + UCL ≥ CL ≥ LCL', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaSpcChart-main');
    const chart = await seedAttributesChart(page, 'C', 'C');
    for (let i = 1; i <= 20; i++) {
      await seedSample(page, chart.id, i, 5 + (i % 10), 1); // defects 5..14, inspected=1 (1 unit)
    }

    const json: any = await new GraphQLClient(page).raw(
      `mutation{ ErpQaSpcChart__recalculateControlLimit(chartId:${chart.id}) }`,
    );
    expect(json.errors ?? null).toBeNull();
    expect(json.data?.ErpQaSpcChart__recalculateControlLimit).toBe(true);

    const s = await verifyState(page, 'ErpQaSpcChart', chart.id, 'calcStatus cl ucl lcl');
    expect(s.calcStatus).toBe('CALCULATED');
    expect(s.cl).not.toBeNull();
    expect(s.ucl).not.toBeNull();
    expect(Number(s.ucl)).toBeGreaterThanOrEqual(Number(s.cl));
    expect(Number(s.cl)).toBeGreaterThanOrEqual(Number(s.lcl));

    await deleteById(page, 'ErpQaSpcChart', chart.id);
  });

  test('U chart: recalculate → CALCULATED + UCL ≥ CL ≥ LCL', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaSpcChart-main');
    const chart = await seedAttributesChart(page, 'U', 'U');
    for (let i = 1; i <= 20; i++) {
      await seedSample(page, chart.id, i, 5 + (i % 5), 80 + (i % 40));
    }

    const json: any = await new GraphQLClient(page).raw(
      `mutation{ ErpQaSpcChart__recalculateControlLimit(chartId:${chart.id}) }`,
    );
    expect(json.errors ?? null).toBeNull();
    expect(json.data?.ErpQaSpcChart__recalculateControlLimit).toBe(true);

    const s = await verifyState(page, 'ErpQaSpcChart', chart.id, 'calcStatus cl ucl lcl');
    expect(s.calcStatus).toBe('CALCULATED');
    expect(s.cl).not.toBeNull();
    expect(s.ucl).not.toBeNull();
    expect(Number(s.ucl)).toBeGreaterThanOrEqual(Number(s.cl));
    expect(Number(s.cl)).toBeGreaterThanOrEqual(Number(s.lcl));

    await deleteById(page, 'ErpQaSpcChart', chart.id);
  });
});
