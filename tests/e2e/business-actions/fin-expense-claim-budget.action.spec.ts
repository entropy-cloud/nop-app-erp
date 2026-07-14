import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  findPageTotal,
  findFirst,
  deleteByFilter,
  deleteById,
} from './_helper';
import { cleanupVoucherByBillCode, cleanupArApByCode } from '../orchestration/_helper';

/**
 * Finance ExpenseClaim 预算控制 hook 浏览器层 E2E（plan 2026-07-14-1218-2 Phase 2）。
 *
 * 验证预算控制 SPI（`IErpFinBudgetControlBiz.check`）经 `ErpFinExpenseClaimProcessor.runBudgetCheckHook`
 * 钩子在费用报销 approve 时触发（config 双门控 `erp-fin.expense-budget-check-enabled=true`
 * + `erp-fin.budget-check-enabled=true` + `erp-fin.budget-expense-subject-code=6602`）：
 *   (a) HARD 阻断：scenario.controlLevel=HARD + 预算余量 < 报销本位币金额 → approve 抛 ERR_BUDGET_EXCEEDED
 *       + approveStatus 保持 SUBMITTED + 无 ControlLog 持久化（@BizMutation 事务回滚）
 *   (b) WARN 放行：scenario.controlLevel=WARN → approve 放行（APPROVED + posted=true）
 *       + ControlLog actionResult=WARNED 持久化
 *
 * 权威设计（docs/design/finance/budget.md §业务规则 + docs/design/finance/expense-claim.md §跨域协作）：
 *   - 余量 = budgetBalance(BUDGET 凭证行) − actualBalance(NORMAL 凭证行)，按 (subjectId + costCenterId + periodId) 聚合
 *   - controlLevel 来自 scenario 实体字段（非 config）：NONE→PASS / WARN→WARNED 放行 / HARD→BLOCKED 抛异常
 *   - 报销预算控制 hook 独立于 PO 预算控制 hook（独立 config 键 expense-budget-check-enabled + budget-expense-subject-code）
 *
 * Explore Decision（Phase 2 触发点 + 科目匹配 + controlLevel 路径 + ControlLog 持久性）：
 *   - 触发点：ErpFinExpenseClaimProcessor.approve → runBudgetCheckHook（config 门控：
 *     expense-budget-check-enabled=true + budget-expense-subject-code=6602）。科目 6602 经
 *     resolveBudgetSubjectId findByCode 解析（种子 id=31, DEBIT 方向）；期间经 resolvePeriodId(businessDate) 解析。
 *   - budget check 金额 = claim.getAmountFunctional()（非 amountWithoutTax）；设 amountFunctional=amountWithoutTax=100 对齐。
 *   - ControlLog 持久性：HARD 路径 writeControlLog(BLOCKED) 与 throw 同处 @BizMutation approve 事务，
 *     NopException 触发回滚 → BLOCKED 日志不持久化（同 0606-1 PO HARD 范式）。
 *   - WARN 路径 writeControlLog(WARNED) 后放行，approve 提交事务 → WARNED 日志持久化。
 *
 * 科目引用：6602 折旧费用（id=31, EXPENSE, DEBIT）—— expense-budget-check subject-code。
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN）。
 * 自包含隔离：每用例独立建 partner+employee+budget scenario+budget line+claim+line（code 唯一），
 * cleanup 删 ControlLog+凭证+AR-AP+行+头+预算行+方案+员工+partner。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
/** 种子费用科目 6602 折旧费用（id=31, EXPENSE, DEBIT）。 */
const SUBJECT_EXPENSE_ID = 31;
const SUBJECT_EXPENSE_CODE = '6602';
const BDATE = '2026-07-15';
const BUDGET_AMOUNT = 50;
const CLAIM_AMOUNT = 100;

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

async function setupEmployee(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  const partnerCode = uniq('EP');
  const partner = await createViaSave(page, 'ErpMdPartner', {
    code: partnerCode, name: `E2E Bud Partner ${partnerCode}`,
    partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
  }, 'id');
  ctx.partnerId = partner.id;
  const empCode = uniq('EE');
  const employee = await createViaSave(page, 'ErpMdEmployee', {
    code: empCode, name: `E2E Bud Claimant ${empCode}`,
    orgId: ORG, status: 'ACTIVE', partnerId: partner.id,
  }, 'id');
  ctx.employeeId = employee.id;
}

async function setupApprovedBudgetScenario(
  page: import('@playwright/test').Page,
  ctx: Ctx,
  controlLevel: 'HARD' | 'WARN',
): Promise<void> {
  const code = uniq('BS');
  const scenario = await createViaSave(page, 'ErpFinBudgetScenario', {
    code, name: `E2E Bud Scenario ${code}`,
    orgId: ORG, acctSchemaId: ACCT_SCHEMA, fiscalYear: 2026, scenarioType: 'ANNUAL',
    currencyId: CURRENCY, exchangeRate: 1, controlLevel,
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
  }, 'id code');
  ctx.scenarioId = scenario.id;
  ctx.scenarioCode = code;

  const line = await createViaSave(page, 'ErpFinBudgetLine', {
    scenarioId: scenario.id, lineNo: 1, orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: PERIOD,
    subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
    budgetAmountSource: BUDGET_AMOUNT, budgetAmountFunctional: BUDGET_AMOUNT,
    currencyId: CURRENCY, exchangeRate: 1,
  }, 'id');
  ctx.budgetLineIds.push(line.id);

  await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: scenario.id }, 'id');
  await callMutationOk(page, 'ErpFinBudgetScenario', 'approve', { id: scenario.id }, 'id');
}

async function setupClaim(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  const code = uniq('EC');
  const claim = await createViaSave(page, 'ErpFinExpenseClaim', {
    code, orgId: ORG, claimantId: ctx.employeeId, departmentId: ORG,
    businessDate: BDATE, paymentMode: 'OWN_ACCOUNT',
    currencyId: CURRENCY, exchangeRate: 1,
    amountSource: CLAIM_AMOUNT, amountFunctional: CLAIM_AMOUNT,
    amountWithoutTax: CLAIM_AMOUNT, taxAmount: 0, amountWithTax: CLAIM_AMOUNT,
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    reason: `E2E budget test ${code}`,
  }, 'id code');
  ctx.claimId = claim.id;
  ctx.claimCode = code;
  const line = await createViaSave(page, 'ErpFinExpenseClaimLine', {
    claimId: claim.id, lineNo: 1, expenseType: 'OFFICE',
    subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
    amountWithoutTax: CLAIM_AMOUNT, taxRate: 0, taxAmount: 0, amountWithTax: CLAIM_AMOUNT,
  }, 'id');
  ctx.lineId = line.id;
}

async function cleanupCtx(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  if (ctx.claimCode) {
    await deleteByFilter(page, 'ErpFinBudgetControlLog', eqFilter('sourceBillCode', ctx.claimCode));
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

test.describe('Finance ExpenseClaim budget control hook browser-layer E2E', () => {
  test('(a) HARD block: claim approve over budget → ERR_BUDGET_EXCEEDED + SUBMITTED + no ControlLog', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = newCtx();
    try {
      await setupEmployee(page, ctx);
      await setupApprovedBudgetScenario(page, ctx, 'HARD');
      await setupClaim(page, ctx);

      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id approveStatus');
      const beforeApprove = await verifyState(page, 'ErpFinExpenseClaim', ctx.claimId!, 'approveStatus');
      expect(beforeApprove.approveStatus, 'claim should be SUBMITTED before approve').toBe('SUBMITTED');

      const rej = await callMutation(page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id approveStatus');
      expect(rej.errors, 'HARD over-budget approve should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'should carry budget-exceeded message').toContain('预算超支');

      const afterApprove = await verifyState(page, 'ErpFinExpenseClaim', ctx.claimId!, 'approveStatus');
      expect(afterApprove.approveStatus, 'claim approveStatus should remain SUBMITTED after HARD block').toBe('SUBMITTED');

      const blockedLogCount = await findPageTotal(
        page, 'ErpFinBudgetControlLog', eqFilter('sourceBillCode', ctx.claimCode!),
      );
      expect(blockedLogCount, 'HARD block ControlLog should not persist (transaction rollback)').toBe(0);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) WARN pass: claim approve over budget → APPROVED + posted + ControlLog WARNED persists', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = newCtx();
    try {
      await setupEmployee(page, ctx);
      await setupApprovedBudgetScenario(page, ctx, 'WARN');
      await setupClaim(page, ctx);

      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id approveStatus');

      const approved = await callMutationOk(
        page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id approveStatus posted',
      );
      expect(approved.approveStatus, 'WARN over-budget approve should pass (APPROVED)').toBe('APPROVED');
      expect(approved.posted, 'WARN path should set posted=true').toBe(true);

      const log = await findFirst<any>(
        page, 'ErpFinBudgetControlLog',
        eqFilter('sourceBillCode', ctx.claimCode!),
        'actionResult sourceBillType requestedAmount availableAmount',
      );
      expect(log, 'WARN path should persist a ControlLog by sourceBillCode').toBeTruthy();
      expect(log.actionResult, 'ControlLog actionResult should be WARNED').toBe('WARNED');
      expect(log.sourceBillType, 'ControlLog sourceBillType should be EXPENSE_CLAIM').toBe('EXPENSE_CLAIM');
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
