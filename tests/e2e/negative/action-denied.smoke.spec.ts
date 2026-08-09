import {
  test,
  expect,
  expectActionDenied,
  loginAsRole,
  callMutation,
} from './_helper';
import { createViaSave, callMutationOk, verifyState, deleteById } from '../business-actions/_helper';

/**
 * 负向隔离测试原语冒烟 demo（plan 2026-08-09-2210-2 / P2.3，permissions-enforcement mission）。
 *
 * 证明 `expectActionDenied` 原语对 GraphQL `{errors}` + token 的检测机制成立（rejection-source-
 * agnostic：业务拒绝与 enforcement 拒绝同信封）。**action-auth OFF 下 enforcement 拒绝不可观测**
 * （运行时确认 gated on P2.4），故 demo 复用一个**既有业务逻辑拒绝**作为载体——hr-payroll
 * markPaid UNSUBMITTED 守卫（`ERR_SALARY_ILLEGAL_STATUS_TRANSITION`，message token「不允许执行该操作」），
 * 与既有 `hr-payroll.action.spec.ts` markPaid negative-path 守卫同源（区别：本 demo 用负向原语断言）。
 *
 * **setup 机制**：hr-payroll 的 `setupPayrollChain`/`cleanupSetup` 为 spec-internal 未导出，本 demo
 * 重新实现最小内联 setup（employee + ACTIVE EmploymentContract + calculateSalary 产 UNSUBMITTED salary）
 * + 内联 cleanup（按反依赖链删 salary/contract/employee），镜像既有 hr-payroll spec 范式，自包含不污染共享 DB。
 *
 * **enforcement 拒绝运行时确认**：随 E1.x（action-auth 翻启后用同原语 + `ENFORCEMENT_ERROR_CODES`
 * 常量断言真权限拒绝，见 `_helper.ts` JSDoc 第二个 example）。
 */

const CITY = 'SHENZHEN';
const TAX_BRACKETS = JSON.stringify([
  { rangeUpperLimit: 36000, rate: 0.03, quickDeduction: 0 },
  { rangeUpperLimit: 144000, rate: 0.1, quickDeduction: 2520 },
  { rangeUpperLimit: 300000, rate: 0.2, quickDeduction: 16920 },
  { rangeUpperLimit: 420000, rate: 0.25, quickDeduction: 31920 },
  { rangeUpperLimit: 660000, rate: 0.3, quickDeduction: 52920 },
  { rangeUpperLimit: 960000, rate: 0.35, quickDeduction: 85920 },
  { rangeUpperLimit: 999999999, rate: 0.45, quickDeduction: 181920 },
]);

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface DemoSetup {
  employeeId: string;
  contractId: string;
  insuranceBaseId: string;
  insuranceConfigIds: string[];
  taxConfigId: string;
}

async function setupMinimalChain(
  page: import('@playwright/test').Page,
  tag: string,
  year: number,
): Promise<DemoSetup> {
  const employee = await createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-NEG-EMP-${tag}`),
      firstName: '测',
      lastName: tag,
      fullName: `负向${tag}`,
      gender: 'MALE',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      orgId: 2,
    },
    'id',
  );

  const contract = await createViaSave(
    page,
    'ErpHrEmploymentContract',
    {
      code: uniq(`E2E-NEG-CT-${tag}`),
      employeeId: employee.id,
      contractType: 'FIXED_TERM',
      signDate: '2024-01-01',
      startDate: '2024-01-01',
      monthlySalary: 10000,
      status: 'ACTIVE',
      orgId: 2,
      businessDate: '2026-08-09',
    },
    'id',
  );

  // calculateSalary 引擎硬前置：社保基数 + HOUSING_FUND 配置 + 个税配置（镜像 hr-payroll setup）
  const insuranceBase = await createViaSave(
    page,
    'ErpHrSocialInsuranceBase',
    {
      employeeId: employee.id,
      cityCode: CITY,
      socialInsuranceBase: 10000,
      housingFundBase: 10000,
      effectiveFrom: '2024-01-01',
      orgId: 2,
    },
    'id',
  );

  const fundCfg = await createViaSave(
    page,
    'ErpHrSocialInsuranceConfig',
    {
      cityCode: CITY,
      insuranceType: 'HOUSING_FUND',
      companyRate: 0.12,
      employeeRate: 0.12,
      baseLowerLimit: 1000,
      baseUpperLimit: 50000,
      effectiveFrom: '2024-01-01',
      orgId: 2,
    },
    'id',
  );

  const taxConfig = await createViaSave(
    page,
    'ErpHrTaxConfig',
    {
      year,
      taxThreshold: 5000,
      taxBrackets: TAX_BRACKETS,
      orgId: 2,
    },
    'id',
  );

  return {
    employeeId: employee.id,
    contractId: contract.id,
    insuranceBaseId: insuranceBase.id,
    insuranceConfigIds: [fundCfg.id],
    taxConfigId: taxConfig.id,
  };
}

async function cleanupMinimalChain(
  page: import('@playwright/test').Page,
  s: DemoSetup,
  salaryId?: string | number,
): Promise<void> {
  if (salaryId !== undefined) {
    await deleteById(page, 'ErpHrSalary', salaryId);
  }
  for (const cid of s.insuranceConfigIds) {
    await deleteById(page, 'ErpHrSocialInsuranceConfig', cid);
  }
  await deleteById(page, 'ErpHrTaxConfig', s.taxConfigId);
  await deleteById(page, 'ErpHrSocialInsuranceBase', s.insuranceBaseId);
  await deleteById(page, 'ErpHrEmploymentContract', s.contractId);
  await deleteById(page, 'ErpHrEmployee', s.employeeId);
}

test.describe('negative isolation primitives: expectActionDenied smoke demo', () => {
  test('business-logic rejection (markPaid UNSUBMITTED guard) asserted via expectActionDenied', async ({ page }) => {
    // loginAsRole 占位回退 nop admin（P2.2b 填充真实负向账号后此处插拔受限主体）
    await loginAsRole(page, 'requester');
    await page.goto('/#/ErpHrSalary-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    const YEAR = 2026;
    const MONTH = 8;
    const s = await setupMinimalChain(page, 'demo', YEAR);

    // calculateSalary 产 UNSUBMITTED/PENDING salary（markPaid 守卫硬前置 approveStatus==APPROVED 不可达）
    const salary = await callMutationOk(
      page,
      'ErpHrSalary',
      'calculateSalary',
      { employeeId: Number(s.employeeId), year: YEAR, month: MONTH },
      'id approveStatus paymentStatus',
    );
    expect(salary.approveStatus, 'precondition approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

    // markPaid on UNSUBMITTED → 拒绝（业务逻辑守卫，作 rejection-source-agnostic 原语的载体）
    const rejected = await callMutation(
      page,
      'ErpHrSalary',
      'markPaid',
      { salaryId: Number(salary.id) },
      'id',
    );

    // ★ 原语机制 Proof：expectActionDenied 检测 {errors} + token 成立
    const errors = expectActionDenied(rejected, { token: '不允许执行该操作' });
    expect(errors.length, 'expectActionDenied returns non-empty errors').toBeGreaterThan(0);

    // 状态不变（守卫拒绝未副作用）
    const unchanged = await verifyState(page, 'ErpHrSalary', salary.id, 'paymentStatus approveStatus');
    expect(unchanged.paymentStatus, 'paymentStatus unchanged after guard reject').toBe('PENDING');
    expect(unchanged.approveStatus, 'approveStatus unchanged after guard reject').toBe('UNSUBMITTED');

    await cleanupMinimalChain(page, s, salary.id);
  });
});
