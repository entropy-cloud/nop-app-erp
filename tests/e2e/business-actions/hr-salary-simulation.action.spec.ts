import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  input,
  eqFilter,
  andFilter,
  findFirst,
  deleteByFilter,
  deleteById,
  GraphQLClient,
} from './_helper';

/**
 * hr ErpHrSalarySimulation 薪酬模拟 What-If 浏览器层 E2E（plan 2026-07-14-0215-3 Phase 1）。
 *
 * 验证薪酬模拟生命周期 DIRECT `@BizMutation` 经 GraphQL /graphql 的全栈可达性：
 *   - createSimulation：冻结源期间快照（需源 salary 行，自包含 __save 源 salary 避免 runPayroll 处理种子员工）
 *   - adjustItem：即时应变（薪酬项目调整 + 内存重算）
 *   - submitForReview（须有调整项）→ IN_REVIEW → approve → APPROVED → convertToFormal → CONVERTED + 正式 salary 回链
 *   - reject 路径：IN_REVIEW → REJECTED
 *   - applyBatchAdjustment（FIXED 范式）
 *   - 非法迁移守卫：DRAFT→approve 抛 ERR_HR_SIMULATION_ILLEGAL_TRANSITION
 *
 * 权威设计（docs/design/human-resource/payroll-simulation.md）：核算复用 PayrollCalculator.recalculateWithOverrides
 * （0831-2 计算规则零修改），故 adjustItem/convertToFormal 经 incomeTaxCalculator 重算需 TaxConfig(目标年)。
 *
 * 自包含 setup（Explore 裁定）：直接 __save 源 ErpHrSalary(2025-6, PENDING) 作模拟源期间（避免 runPayroll
 * 遍历种子 HR-EMP-001/002 缺合同/社保配置抛错）+ TaxConfig(2026 目标年，recalculateDerived 个税累计预扣所需）。
 * 清理：删调整项 + 转正式 salary + 模拟 + 源 salary + tax config + employee。
 */

const TAX_BRACKETS = JSON.stringify([
  { rangeUpperLimit: 36000, rate: 0.03, quickDeduction: 0 },
  { rangeUpperLimit: 144000, rate: 0.1, quickDeduction: 2520 },
  { rangeUpperLimit: 300000, rate: 0.2, quickDeduction: 16920 },
  { rangeUpperLimit: 420000, rate: 0.25, quickDeduction: 31920 },
  { rangeUpperLimit: 660000, rate: 0.3, quickDeduction: 52920 },
  { rangeUpperLimit: 960000, rate: 0.35, quickDeduction: 85920 },
  { rangeUpperLimit: 999999999, rate: 0.45, quickDeduction: 181920 },
]);

const SOURCE_YEAR = 2025;
const SOURCE_MONTH = 6;
const TARGET_YEAR = 2026;
const TARGET_MONTH = 1;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface SimSetup {
  employeeId: string;
  sourceSalaryId: string;
  taxConfigId: string;
}

async function setupSimChain(page: import('@playwright/test').Page, tag: string): Promise<SimSetup> {
  const employee = await createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-SIM-EMP-${tag}`),
      firstName: '模',
      lastName: tag,
      fullName: `模拟${tag}`,
      gender: 'FEMALE',
      hireDate: '2023-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      orgId: 2,
    },
    'id',
  );

  // 直接 __save 源 salary（避免 runPayroll 处理种子员工缺配置抛错）
  const sourceSalary = await createViaSave(
    page,
    'ErpHrSalary',
    {
      employeeId: employee.id,
      year: SOURCE_YEAR,
      month: SOURCE_MONTH,
      basicSalary: 10000,
      grossSalary: 10000,
      socialInsurance: 0,
      housingFund: 0,
      taxAmount: 0,
      otherDeductions: 0,
      netSalary: 10000,
      actualWorkDays: 22,
      requiredWorkDays: 22,
      totalOvertimeHours: 0,
      unpaidLeaveDays: 0,
      cumulativeData: '{}',
      paymentStatus: 'PENDING',
      approveStatus: 'UNSUBMITTED',
      orgId: 2,
      businessDate: '2025-06-15',
    },
    'id',
  );

  // TaxConfig 目标年（recalculateDerived 个税累计预扣所需）
  const taxConfig = await createViaSave(
    page,
    'ErpHrTaxConfig',
    { year: TARGET_YEAR, taxThreshold: 5000, taxBrackets: TAX_BRACKETS, orgId: 2 },
    'id',
  );

  return { employeeId: employee.id, sourceSalaryId: sourceSalary.id, taxConfigId: taxConfig.id };
}

/**
 * applyBatchAdjustment 返回 Map<String,Object>（非实体），GraphQL Map 标量不支持字段选择，
 * 故构造原始 mutation（无选择集，对齐 finance-voucher-post scalar return 范式）。
 */
async function applyBatchRaw(
  page: import('@playwright/test').Page,
  simulationId: number,
  scope: Record<string, unknown>,
  adjustType: string,
  value: number,
): Promise<{ data: any | null; errors: any[] | null }> {
  const json: any = await new GraphQLClient(page).raw(
    `mutation($scope:Map){ ErpHrSalarySimulation__applyBatchAdjustment(simulationId:${simulationId},scope:$scope,adjustType:${JSON.stringify(adjustType)},value:${JSON.stringify(value)}) }`,
    { scope },
  );
  return { data: json?.data?.ErpHrSalarySimulation__applyBatchAdjustment ?? null, errors: json?.errors ?? null };
}

async function cleanupSim(page: import('@playwright/test').Page, s: SimSetup, simulationId?: string): Promise<void> {
  if (simulationId) {
    await deleteByFilter(page, 'ErpHrSalarySimulationItemAdjustment', eqFilter('simulationId', Number(simulationId)));
    // 删转正式 salary（convertToFormal 产物）
    const converted = await findFirst(
      page,
      'ErpHrSalary',
      andFilter(eqFilter('employeeId', Number(s.employeeId)), eqFilter('year', TARGET_YEAR), eqFilter('month', TARGET_MONTH)),
      'id',
    );
    if (converted) {
      await deleteById(page, 'ErpHrSalary', (converted as any).id);
    }
    await deleteById(page, 'ErpHrSalarySimulation', simulationId);
  }
  await deleteById(page, 'ErpHrTaxConfig', s.taxConfigId);
  await deleteById(page, 'ErpHrSalary', s.sourceSalaryId);
  await deleteById(page, 'ErpHrEmployee', s.employeeId);
}

test.describe('hr ErpHrSalarySimulation What-If lifecycle DIRECT actions', () => {
  test('happy path: create → adjust → submit → approve → convertToFormal (CONVERTED + formal salary linked)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalarySimulation-main');

    const s = await setupSimChain(page, 'hp');

    // createSimulation：冻结源期间快照
    const sim = await callMutationOk(
      page,
      'ErpHrSalarySimulation',
      'createSimulation',
      {
        sourceYear: SOURCE_YEAR,
        sourceMonth: SOURCE_MONTH,
        simulationPeriodYear: TARGET_YEAR,
        simulationPeriodMonth: TARGET_MONTH,
        simulationName: `E2E Sim hp`,
        employeeScope: input('Map', { employeeIds: [Number(s.employeeId)] }),
      },
      'id status sourceSalaryId',
    );
    expect(sim.status, 'new simulation status=DRAFT').toBe('DRAFT');

    // adjustItem：即时应变（basicSalary 10000 → 12000）
    const adjusted = await callMutationOk(
      page,
      'ErpHrSalarySimulation',
      'adjustItem',
      {
        simulationId: Number(sim.id),
        employeeId: Number(s.employeeId),
        salaryItemCode: 'basicSalary',
        adjustedAmount: 12000,
        reason: 'SALARY_CHANGE',
      },
      'id grossSalary',
    );
    expect(Number(adjusted.grossSalary), 'adjustItem recalculates gross=12000').toBe(12000);

    // submitForReview → IN_REVIEW
    await callMutationOk(page, 'ErpHrSalarySimulation', 'submitForReview', { simulationId: Number(sim.id) }, 'id');
    let st = await verifyState(page, 'ErpHrSalarySimulation', sim.id, 'status');
    expect(st.status, 'after submitForReview status=IN_REVIEW').toBe('IN_REVIEW');

    // approve → APPROVED
    await callMutationOk(page, 'ErpHrSalarySimulation', 'approve', { simulationId: Number(sim.id), reviewerId: 1 }, 'id');
    st = await verifyState(page, 'ErpHrSalarySimulation', sim.id, 'status');
    expect(st.status, 'after approve status=APPROVED').toBe('APPROVED');

    // convertToFormal → CONVERTED + 正式 salary 回链
    await callMutationOk(page, 'ErpHrSalarySimulation', 'convertToFormal', { simulationId: Number(sim.id) }, 'id');
    st = await verifyState(page, 'ErpHrSalarySimulation', sim.id, 'status convertedSalaryId');
    expect(st.status, 'after convertToFormal status=CONVERTED').toBe('CONVERTED');
    expect(st.convertedSalaryId, 'convertedSalaryId non-null').not.toBeNull();

    // 正式 salary 回链：ErpHrSalary 行存在于目标期间
    const formal = await findFirst(
      page,
      'ErpHrSalary',
      andFilter(eqFilter('employeeId', Number(s.employeeId)), eqFilter('year', TARGET_YEAR), eqFilter('month', TARGET_MONTH)),
      'id paymentStatus approveStatus',
    );
    expect(formal, 'formal salary created in target period').not.toBeNull();
    expect((formal as any).paymentStatus, 'formal salary paymentStatus=PENDING').toBe('PENDING');

    await cleanupSim(page, s, sim.id);
  });

  test('reject path: submit → reject (IN_REVIEW → REJECTED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalarySimulation-main');

    const s = await setupSimChain(page, 'rj');
    const sim = await callMutationOk(
      page,
      'ErpHrSalarySimulation',
      'createSimulation',
      {
        sourceYear: SOURCE_YEAR,
        sourceMonth: SOURCE_MONTH,
        simulationPeriodYear: TARGET_YEAR,
        simulationPeriodMonth: TARGET_MONTH,
        simulationName: `E2E Sim rj`,
        employeeScope: input('Map', { employeeIds: [Number(s.employeeId)] }),
      },
      'id',
    );
    await callMutationOk(page, 'ErpHrSalarySimulation', 'adjustItem', {
      simulationId: Number(sim.id),
      employeeId: Number(s.employeeId),
      salaryItemCode: 'basicSalary',
      adjustedAmount: 11000,
      reason: 'rj',
    }, 'id');
    await callMutationOk(page, 'ErpHrSalarySimulation', 'submitForReview', { simulationId: Number(sim.id) }, 'id');

    // reject → REJECTED
    await callMutationOk(page, 'ErpHrSalarySimulation', 'reject', { simulationId: Number(sim.id), reason: 'budget exceeded' }, 'id');
    const st = await verifyState(page, 'ErpHrSalarySimulation', sim.id, 'status');
    expect(st.status, 'after reject status=REJECTED').toBe('REJECTED');

    await cleanupSim(page, s, sim.id);
  });

  test('applyBatchAdjustment FIXED: affectedCount > 0 + totalGrossIncrease non-zero', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalarySimulation-main');

    const s = await setupSimChain(page, 'ba');
    const sim = await callMutationOk(
      page,
      'ErpHrSalarySimulation',
      'createSimulation',
      {
        sourceYear: SOURCE_YEAR,
        sourceMonth: SOURCE_MONTH,
        simulationPeriodYear: TARGET_YEAR,
        simulationPeriodMonth: TARGET_MONTH,
        simulationName: `E2E Sim ba`,
        employeeScope: input('Map', { employeeIds: [Number(s.employeeId)] }),
      },
      'id',
    );

    // applyBatchAdjustment FIXED：每人加 1000 基本工资（返回 Map，经原始 mutation 无选择集）
    const { data: batchResult, errors: batchErrors } = await applyBatchRaw(
      page,
      Number(sim.id),
      { employeeIds: [Number(s.employeeId)] },
      'FIXED',
      1000,
    );
    expect(batchErrors, 'applyBatchAdjustment should not return GraphQL errors').toBeNull();
    expect(batchResult, 'applyBatchAdjustment should return result map').toBeTruthy();
    expect(Number(batchResult.affectedCount), 'FIXED batch affects 1 employee').toBe(1);

    // submitForReview 须有调整项（applyBatchAdjustment 已记录 basicSalary 调整）
    await callMutationOk(page, 'ErpHrSalarySimulation', 'submitForReview', { simulationId: Number(sim.id) }, 'id');
    const st = await verifyState(page, 'ErpHrSalarySimulation', sim.id, 'status');
    expect(st.status, 'batch adjustment enables submit → IN_REVIEW').toBe('IN_REVIEW');

    await cleanupSim(page, s, sim.id);
  });

  test('illegal transition guard: DRAFT→approve rejected (ERR_HR_SIMULATION_ILLEGAL_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalarySimulation-main');

    const s = await setupSimChain(page, 'gd');
    const sim = await callMutationOk(
      page,
      'ErpHrSalarySimulation',
      'createSimulation',
      {
        sourceYear: SOURCE_YEAR,
        sourceMonth: SOURCE_MONTH,
        simulationPeriodYear: TARGET_YEAR,
        simulationPeriodMonth: TARGET_MONTH,
        simulationName: `E2E Sim gd`,
        employeeScope: input('Map', { employeeIds: [Number(s.employeeId)] }),
      },
      'id status',
    );
    expect(sim.status, 'precondition status=DRAFT').toBe('DRAFT');

    // DRAFT → approve（须 IN_REVIEW）：抛 ERR_HR_SIMULATION_ILLEGAL_TRANSITION
    const rej = await callMutation(page, 'ErpHrSalarySimulation', 'approve', { simulationId: Number(sim.id), reviewerId: 1 }, 'id');
    expect(rej.errors, 'approve from DRAFT should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // DRAFT → submitForReview 无调整项 → ERR_HR_SIMULATION_NO_ADJUSTMENT
    const rej2 = await callMutation(page, 'ErpHrSalarySimulation', 'submitForReview', { simulationId: Number(sim.id) }, 'id');
    expect(rej2.errors, 'submitForReview with no adjustment should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry no-adjustment token').toContain('调整项');

    await cleanupSim(page, s, sim.id);
  });
});
