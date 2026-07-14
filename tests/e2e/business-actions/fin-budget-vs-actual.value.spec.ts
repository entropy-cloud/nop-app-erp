import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import { cleanupVoucherByBillCode, cleanupArApByCode } from '../orchestration/_helper';

/**
 * Finance 预算对比报表 getBudgetVsActual 数值断言浏览器层 E2E（plan 2026-07-14-1218-2 Phase 2）。
 *
 * 验证 `ErpFinBudgetLine__getBudgetVsActual` @BizQuery 的 budgetAmount / actualAmount / availableAmount
 * 三值增量断言（初始 → actual 增量 → 红冲回退），覆盖 BUDGET 凭证（预算）+ NORMAL 凭证（实际）+ REVERSAL 凭证（冲销）的
 * postingType 聚合 + isReversed 过滤语义：
 *   (a) 初始：approve 预算方案（BUDGET 凭证 Dr 6602=1000）→ budgetAmount=1000 / actualAmount=0 / availableAmount=1000
 *   (b) actual 增量：approve ExpenseClaim（NORMAL 凭证 Dr 6602=200）→ actualAmount=200 / availableAmount=800
 *   (c) 红冲回退：reverseApprove（原+红冲凭证均 isReversed=true → 双双排除）→ actualAmount=0 / availableAmount=1000
 *
 * 权威设计（docs/design/finance/budget.md §业务规则5 + ErpFinBudgetLineBizModel.getBudgetVsActual）：
 *   - budgetAmount = Σ BUDGET 凭证行 amountFunctional（postingType=BUDGET + isReversed=false + docStatus=POSTED）
 *   - actualAmount = Σ NORMAL/NULL 凭证行 amountFunctional（非 BUDGET + isReversed=false + docStatus=POSTED）
 *   - availableAmount = budgetAmount − actualAmount
 *   - 聚合维度 (subjectId, costCenterId, projectId)：budget line 须 costCenterId=null + projectId=null 以匹配 NORMAL 凭证行
 *   - subjectName 按 ErpMdSubject.name（种子 6602 name="折旧费用"，非 Provider 注释"管理费用"）
 *
 * Explore Decision（红冲后 actualAmount=0 裁定）：
 *   - persistVoucher 对 REVERSAL 凭证设 isReversed=true（reverseProcess line 222），
 *     markOriginalVoucherReversed 对原 NORMAL 凭证设 isReversed=true（line 233）。
 *   - getBudgetVsActual 过滤 isReversed=false → 原凭证 + 红冲凭证双双排除 → actualAmount 归零。
 *   - 与 budget CHECK（ErpFinBudgetControlBiz.aggregateAmount）一致：同 isReversed=false 过滤。
 *
 * 科目引用：6602 折旧费用（id=31, EXPENSE, DEBIT, name="折旧费用"）。
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN）。
 * 自包含隔离：建 partner+employee+budget scenario(NONE)+budget line+claim+line，cleanup 删凭证+AR-AP+行+头+预算行+方案+员工+partner。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
const SUBJECT_EXPENSE_ID = 31;
const SUBJECT_EXPENSE_CODE = '6602';
const SUBJECT_EXPENSE_NAME = '折旧费用';
const BDATE = '2026-07-15';
const BUDGET_AMOUNT = 1000;
const CLAIM_AMOUNT_WITHOUT_TAX = 200;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now()}${_seq}`;
}

interface Ctx {
  partnerId?: string | number;
  employeeId?: string | number;
  scenarioId?: string | number;
  scenarioCode?: string;
  budgetLineIds: Array<string | number>;
  claimId?: string | number;
  claimCode?: string;
  lineId?: string | number;
}

function newCtx(): Ctx {
  return { budgetLineIds: [] };
}

async function setupFull(page: import('@playwright/test').Page): Promise<Ctx> {
  const ctx = newCtx();

  const partnerCode = uniq('EP');
  const partner = await createViaSave(page, 'ErpMdPartner', {
    code: partnerCode, name: `E2E BVA Partner ${partnerCode}`,
    partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
  }, 'id');
  ctx.partnerId = partner.id;
  const empCode = uniq('EE');
  const employee = await createViaSave(page, 'ErpMdEmployee', {
    code: empCode, name: `E2E BVA Claimant ${empCode}`,
    orgId: ORG, status: 'ACTIVE', partnerId: partner.id,
  }, 'id');
  ctx.employeeId = employee.id;

  const scnCode = uniq('BS');
  const scenario = await createViaSave(page, 'ErpFinBudgetScenario', {
    code: scnCode, name: `E2E BVA Scenario ${scnCode}`,
    orgId: ORG, acctSchemaId: ACCT_SCHEMA, fiscalYear: 2026, scenarioType: 'ANNUAL',
    currencyId: CURRENCY, exchangeRate: 1, controlLevel: 'NONE',
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
  }, 'id code');
  ctx.scenarioId = scenario.id;
  ctx.scenarioCode = scnCode;

  const line = await createViaSave(page, 'ErpFinBudgetLine', {
    scenarioId: scenario.id, lineNo: 1, orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: PERIOD,
    subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
    budgetAmountSource: BUDGET_AMOUNT, budgetAmountFunctional: BUDGET_AMOUNT,
    currencyId: CURRENCY, exchangeRate: 1,
  }, 'id');
  ctx.budgetLineIds.push(line.id);

  await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: scenario.id }, 'id');
  await callMutationOk(page, 'ErpFinBudgetScenario', 'approve', { id: scenario.id }, 'id');

  const claimCode = uniq('EC');
  const claim = await createViaSave(page, 'ErpFinExpenseClaim', {
    code: claimCode, orgId: ORG, claimantId: ctx.employeeId, departmentId: ORG,
    businessDate: BDATE, paymentMode: 'OWN_ACCOUNT',
    currencyId: CURRENCY, exchangeRate: 1,
    amountSource: CLAIM_AMOUNT_WITHOUT_TAX, amountFunctional: CLAIM_AMOUNT_WITHOUT_TAX,
    amountWithoutTax: CLAIM_AMOUNT_WITHOUT_TAX, taxAmount: 0, amountWithTax: CLAIM_AMOUNT_WITHOUT_TAX,
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    reason: `E2E BVA test ${claimCode}`,
  }, 'id code');
  ctx.claimId = claim.id;
  ctx.claimCode = claimCode;
  const claimLine = await createViaSave(page, 'ErpFinExpenseClaimLine', {
    claimId: claim.id, lineNo: 1, expenseType: 'OFFICE',
    subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
    amountWithoutTax: CLAIM_AMOUNT_WITHOUT_TAX, taxRate: 0, taxAmount: 0, amountWithTax: CLAIM_AMOUNT_WITHOUT_TAX,
  }, 'id');
  ctx.lineId = claimLine.id;

  return ctx;
}

async function getBudgetVsActual(page: import('@playwright/test').Page): Promise<any[]> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `query{ ErpFinBudgetLine__getBudgetVsActual(acctSchemaId:${ACCT_SCHEMA},periodId:${PERIOD},subjectId:${SUBJECT_EXPENSE_ID}){ subjectId subjectCode subjectName budgetAmount actualAmount availableAmount } }`,
    },
  });
  const json: any = await resp.json();
  expect(json?.errors, `getBudgetVsActual should not return errors: ${JSON.stringify(json?.errors)}`).toBeFalsy();
  return json?.data?.ErpFinBudgetLine__getBudgetVsActual ?? [];
}

function findRow(rows: any[]): any | null {
  return rows.find((r) => Number(r.subjectId) === SUBJECT_EXPENSE_ID && r.costCenterId == null) || null;
}

async function cleanupCtx(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  if (ctx.claimCode) {
    await cleanupArApByCode(page, ctx.claimCode);
    await cleanupVoucherByBillCode(page, ctx.claimCode);
  }
  if (ctx.lineId) await deleteById(page, 'ErpFinExpenseClaimLine', ctx.lineId);
  if (ctx.claimId) await deleteById(page, 'ErpFinExpenseClaim', ctx.claimId);
  if (ctx.scenarioCode) await cleanupVoucherByBillCode(page, ctx.scenarioCode);
  for (const id of ctx.budgetLineIds ?? []) await deleteById(page, 'ErpFinBudgetLine', id);
  if (ctx.scenarioId) await deleteById(page, 'ErpFinBudgetScenario', ctx.scenarioId);
  if (ctx.employeeId) await deleteById(page, 'ErpMdEmployee', ctx.employeeId);
  if (ctx.partnerId) await deleteById(page, 'ErpMdPartner', ctx.partnerId);
}

test.describe('Finance budget-vs-actual report value assertions browser-layer E2E', () => {
  test('budgetAmount/actualAmount/availableAmount increments: initial → actual → reversal rollback', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetLine-main');

    const ctx = await setupFull(page);
    try {
      // (a) 初始断言：BUDGET 凭证存预算额，无 NORMAL 凭证
      let rows = await getBudgetVsActual(page);
      let row = findRow(rows);
      expect(row, 'getBudgetVsActual should include a row for subject 6602 (costCenterId=null)').toBeTruthy();
      expect(row.subjectCode, 'row.subjectCode should be 6602').toBe(SUBJECT_EXPENSE_CODE);
      expect(row.subjectName, 'row.subjectName should be seed name "折旧费用"').toBe(SUBJECT_EXPENSE_NAME);
      expect(Number(row.budgetAmount), 'initial budgetAmount should be 1000 (BUDGET voucher)').toBe(BUDGET_AMOUNT);
      expect(Number(row.actualAmount), 'initial actualAmount should be 0 (no NORMAL voucher)').toBe(0);
      expect(Number(row.availableAmount), 'initial availableAmount should be 1000').toBe(BUDGET_AMOUNT);

      // (b) actual 增量断言：approve ExpenseClaim → NORMAL 凭证 Dr 6602=200
      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id');
      await callMutationOk(page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id posted');

      rows = await getBudgetVsActual(page);
      row = findRow(rows);
      expect(row, 'row should still exist after claim approve').toBeTruthy();
      expect(Number(row.budgetAmount), 'budgetAmount should remain 1000 after claim approve').toBe(BUDGET_AMOUNT);
      expect(Number(row.actualAmount), 'actualAmount should be 200 after claim approve (NORMAL voucher)').toBe(CLAIM_AMOUNT_WITHOUT_TAX);
      expect(Number(row.availableAmount), 'availableAmount should be 800 (1000-200)').toBe(BUDGET_AMOUNT - CLAIM_AMOUNT_WITHOUT_TAX);

      // (c) 红冲回退断言：reverseApprove → 原+红冲凭证均 isReversed=true → 双双排除
      await callMutationOk(page, 'ErpFinExpenseClaim', 'reverseApprove', { id: ctx.claimId }, 'id posted');

      rows = await getBudgetVsActual(page);
      row = findRow(rows);
      expect(row, 'row should still exist after reverseApprove').toBeTruthy();
      expect(Number(row.budgetAmount), 'budgetAmount should remain 1000 after reversal').toBe(BUDGET_AMOUNT);
      expect(Number(row.actualAmount), 'actualAmount should be 0 after reversal (both vouchers isReversed=true)').toBe(0);
      expect(Number(row.availableAmount), 'availableAmount should be 1000 after reversal').toBe(BUDGET_AMOUNT);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
