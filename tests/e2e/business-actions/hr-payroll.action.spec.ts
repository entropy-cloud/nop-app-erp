import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  deleteById,
} from './_helper';

/**
 * hr ErpHrSalary 薪酬引擎浏览器层 E2E（plan 2026-07-14-0215-3 Phase 1）。
 *
 * 验证薪酬引擎 DIRECT `@BizMutation` 经 GraphQL /graphql 的全栈可达性：
 *   - calculateSalary(employeeId,year,month)：计算引擎触发（出勤比例→基本工资→社保→公积金→个税→实发）
 *     产 ErpHrSalary（grossSalary/netSalary 非空 + approveStatus=UNSUBMITTED + paymentStatus=PENDING）。
 *   - voidSalary(salaryId)：作废回退（paymentStatus PENDING→VOID）。
 *   - markPaid(salaryId) negative-path 守卫：UNSUBMITTED salary 调 markPaid 抛 ERR_SALARY_ILLEGAL_STATUS_TRANSITION
 *     （硬守卫 approveStatus==APPROVED，salary 审批轴经 useWorkflow xwf 浏览器层不可达，同 2330-1 裁决）。
 *   - generateBankFile(year,month,bankId) negative-path 守卫：无 APPROVED+PENDING 行 → ERR_NO_APPROVED_SALARY_FOR_BANK_FILE。
 *
 * 权威设计（docs/design/human-resource/payroll.md）：PayrollCalculator 编排，calculate 不触发过账
 * （仅 markPaid 触发 SalaryPostingDispatcher），故 calculate 产物 posted 保持默认 false。
 *
 * 自包含 setup（对齐 finance recon 自包含 partner 范式）：建 employee + ACTIVE EmploymentContract(monthlySalary)
 * + SocialInsuranceBase + SocialInsuranceConfig(HOUSING_FUND) + TaxConfig(year) 使 calculateSalary 浏览器层可达。
 * ORM ErpHrSalary useWorkflow=true，但 calculateSalary/voidSalary 为 DIRECT @BizMutation（不经 xwf）浏览器层可达。
 * 清理：删 salary + contract + social insurance base/config + tax config + employee。
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

interface PayrollSetup {
  employeeId: string;
  contractId: string;
  insuranceBaseId: string;
  insuranceConfigIds: string[];
  taxConfigId: string;
}

async function setupPayrollChain(
  page: import('@playwright/test').Page,
  tag: string,
  year: number,
): Promise<PayrollSetup> {
  const employee = await createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-PR-EMP-${tag}`),
      firstName: '测',
      lastName: tag,
      fullName: `测试${tag}`,
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
      code: uniq(`E2E-PR-CT-${tag}`),
      employeeId: employee.id,
      contractType: 'FIXED_TERM',
      signDate: '2024-01-01',
      startDate: '2024-01-01',
      monthlySalary: 10000,
      status: 'ACTIVE',
      orgId: 2,
      businessDate: '2026-07-14',
    },
    'id',
  );

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

async function cleanupSetup(page: import('@playwright/test').Page, s: PayrollSetup): Promise<void> {
  for (const cid of s.insuranceConfigIds) {
    await deleteById(page, 'ErpHrSocialInsuranceConfig', cid);
  }
  await deleteById(page, 'ErpHrTaxConfig', s.taxConfigId);
  await deleteById(page, 'ErpHrSocialInsuranceBase', s.insuranceBaseId);
  await deleteById(page, 'ErpHrEmploymentContract', s.contractId);
  await deleteById(page, 'ErpHrEmployee', s.employeeId);
}

test.describe('hr ErpHrSalary payroll engine DIRECT actions', () => {
  test('calculateSalary triggers engine (gross/net non-null + UNSUBMITTED/PENDING) then voidSalary rollback', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalary-main');

    const YEAR = 2026;
    const MONTH = 7;
    const s = await setupPayrollChain(page, 'calc', YEAR);

    // calculateSalary：DIRECT @BizMutation，触发计算引擎
    const salary = await callMutationOk(
      page,
      'ErpHrSalary',
      'calculateSalary',
      { employeeId: Number(s.employeeId), year: YEAR, month: MONTH },
      'id grossSalary netSalary paymentStatus approveStatus basicSalary',
    );

    // 计算引擎触发可观测性：金额非空 + UNSUBMITTED/PENDING
    expect(Number(salary.grossSalary), 'grossSalary should be non-zero after engine trigger').toBeGreaterThan(0);
    expect(salary.netSalary, 'netSalary should be non-null').not.toBeNull();
    expect(Number(salary.netSalary), 'netSalary should be a finite number').toBeGreaterThanOrEqual(0);
    expect(salary.approveStatus, 'calculateSalary sets approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');
    expect(salary.paymentStatus, 'calculateSalary sets paymentStatus=PENDING').toBe('PENDING');

    // 独立 __get 断言状态翻转
    const verified = await verifyState(page, 'ErpHrSalary', salary.id, 'grossSalary paymentStatus approveStatus');
    expect(Number(verified.grossSalary), '__get grossSalary non-zero').toBeGreaterThan(0);
    expect(verified.paymentStatus, '__get paymentStatus=PENDING').toBe('PENDING');

    // voidSalary 作废回退：PENDING → VOID
    await callMutationOk(page, 'ErpHrSalary', 'voidSalary', { salaryId: Number(salary.id) }, 'id');
    const voided = await verifyState(page, 'ErpHrSalary', salary.id, 'paymentStatus');
    expect(voided.paymentStatus, 'after voidSalary paymentStatus=VOID').toBe('VOID');

    // 清理
    await deleteById(page, 'ErpHrSalary', salary.id);
    await cleanupSetup(page, s);
  });

  test('markPaid negative-path guard: UNSUBMITTED salary rejected (ERR_SALARY_ILLEGAL_STATUS_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalary-main');

    const YEAR = 2026;
    const MONTH = 7;
    const s = await setupPayrollChain(page, 'mp', YEAR);

    // 先 calculateSalary 产 UNSUBMITTED/PENDING salary
    const salary = await callMutationOk(
      page,
      'ErpHrSalary',
      'calculateSalary',
      { employeeId: Number(s.employeeId), year: YEAR, month: MONTH },
      'id approveStatus paymentStatus',
    );
    expect(salary.approveStatus, 'precondition approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

    // markPaid 守卫：approveStatus≠APPROVED → 抛 ERR_SALARY_ILLEGAL_STATUS_TRANSITION
    const rej = await callMutation(page, 'ErpHrSalary', 'markPaid', { salaryId: Number(salary.id) }, 'id');
    expect(rej.errors, 'markPaid on UNSUBMITTED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // 状态不变
    const unchanged = await verifyState(page, 'ErpHrSalary', salary.id, 'paymentStatus approveStatus');
    expect(unchanged.paymentStatus, 'salary paymentStatus unchanged after guard reject').toBe('PENDING');
    expect(unchanged.approveStatus, 'salary approveStatus unchanged after guard reject').toBe('UNSUBMITTED');

    await deleteById(page, 'ErpHrSalary', salary.id);
    await cleanupSetup(page, s);
  });

  test('generateBankFile negative-path guard: no APPROVED salaries → ERR_NO_APPROVED_SALARY_FOR_BANK_FILE', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrSalary-main');

    // 使用无任何 APPROVED 薪酬的期间（2025-11），守卫拒绝
    const rej = await callMutation(
      page,
      'ErpHrSalary',
      'generateBankFile',
      { year: 2025, month: 11, bankId: 1 },
      'id',
    );
    expect(rej.errors, 'generateBankFile with no APPROVED salaries should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry no-approved-salary token').toContain('未找到');
  });
});
