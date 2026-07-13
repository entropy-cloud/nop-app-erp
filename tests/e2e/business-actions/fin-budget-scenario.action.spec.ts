import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import {
  findBudgetVoucherIdByCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
} from '../orchestration/_helper';

/**
 * Finance ErpFinBudgetScenario 预算方案审批生命周期 + BUDGET 影子凭证浏览器层 E2E
 * （plan 2026-07-14-0606-1 Phase 1）。
 *
 * 验证 finance 域预算方案 4 个 DIRECT `@BizMutation`（submit/approve/reject/cancel）经 GraphQL `/graphql`
 * 的全栈可达性 + BUDGET 影子凭证生成/红冲：
 *   (a) submit：DRAFT → SUBMITTED（docStatus + approveStatus 双字段翻转）
 *   (b) approve：SUBMITTED → APPROVED + scenario.voucherId 回写 + BUDGET 影子凭证行数值断言
 *       （BudgetVoucherGenerator 按 periodId 分组，billCode=scenario.code；借贷方向按 ErpMdSubject.direction
 *        自动取：资产/费用=DEBIT，负债/收入=CREDIT）
 *   (c) reject：另建 SUBMITTED 方案 → REJECTED（docStatus + approveStatus 均→REJECTED）
 *   (d) cancel：APPROVED 方案 → CANCELLED + 红冲 BUDGET 凭证（reversalOfVoucherId 非空 + 原正向凭证 isReversed=true）
 *   (e) 非法迁移守卫（APPROVED→submit / DRAFT→approve 抛 ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION）
 *
 * 权威设计（docs/design/finance/budget.md §影子凭证 §业务规则）：
 *   - approve 经 BudgetVoucherGenerator.generate 按 periodId 分组写 postingType=BUDGET 影子凭证；
 *     正向凭证 isReversed=false、reversalOfVoucherId=null。
 *   - cancel 经 BudgetVoucherGenerator.reverse 反查全部 BUDGET 凭证逐张红冲（红冲凭证同 postingType=BUDGET，
 *     reversalOfVoucherId=原凭证 id，金额取负 + 原正向凭证 isReversed=true）。
 *   - 借贷规则：资产/费用类（direction=DEBIT）记借方；负债/收入类（direction=CREDIT）记贷方。
 *
 * Explore Decision（Phase 1 BUDGET 凭证行科目结构核实）：
 *   - BudgetVoucherGenerator.toFact 读 budgetLine.subjectId → ErpMdSubject.direction 决定 dcDirection；
 *     金额取 budgetLine.budgetAmountFunctional。故 setup 须设 subjectId + subjectCode + budgetAmountFunctional。
 *   - periodId 来自 budgetLine（非 scenario）；无 periodId 的行不生成凭证。controlLevel 来自 scenario 实体字段
 *     （非 config），本 Phase 不验证控制行为故置 NONE。
 *   - 自包含最小 setup：建 scenario(DRAFT) + 2 行预算（DEBIT 费用科目 6601 + CREDIT 收入科目 5001 同 periodId），
 *     覆盖双借贷方向。种子科目 6601(id=8,DEBIT) / 5001(id=6,CREDIT) 均在 erp_md_subject.csv。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN，endDate=2026-07-31）。
 * 自包含隔离：scenario.code 唯一（E2E-BUD-{ts}），cleanup 删 BUDGET 凭证（按 scenario.code）+ 预算行 + 方案，
 * 使 getBudgetVsActual/预算控制查询基线无漂移。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
/** 种子费用科目 6601 销售费用（id=8, EXPENSE, DEBIT）。 */
const SUBJECT_DEBIT_ID = 8;
const SUBJECT_DEBIT_CODE = '6601';
/** 种子收入科目 5001 主营业务收入（id=6, INCOME, CREDIT）。 */
const SUBJECT_CREDIT_ID = 6;
const SUBJECT_CREDIT_CODE = '5001';
const AMOUNT = 100;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface BudgetSetup {
  scenario?: any;
  scenarioCode?: string;
  scenarioId?: string | number;
  lineIds: Array<string | number>;
}

async function createDraftScenario(page: import('@playwright/test').Page, controlLevel = 'NONE'): Promise<BudgetSetup> {
  const code = uniq('E2E-BUD-SCN');
  const scenario = await createViaSave(
    page,
    'ErpFinBudgetScenario',
    {
      code,
      name: `E2E Budget Scenario ${code}`,
      orgId: ORG,
      acctSchemaId: ACCT_SCHEMA,
      fiscalYear: 2026,
      scenarioType: 'ANNUAL',
      currencyId: CURRENCY,
      exchangeRate: 1,
      controlLevel,
      docStatus: 'DRAFT',
      // approveStatus 走 wf/approve-status 字典（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，无 DRAFT）；
      // 初始态=UNSUBMITTED，submit 时 Processor 置 SUBMITTED（Processor 仅按 docStatus 校验迁移）
      approveStatus: 'UNSUBMITTED',
    },
    'id code docStatus approveStatus voucherId',
  );
  const setup: BudgetSetup = { scenario, scenarioCode: code, scenarioId: scenario.id, lineIds: [] };

  // 2 行预算：DEBIT 费用 + CREDIT 收入，同 periodId，覆盖双借贷方向
  const debitLine = await createViaSave(
    page,
    'ErpFinBudgetLine',
    {
      scenarioId: scenario.id, lineNo: 1, orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: PERIOD,
      subjectId: SUBJECT_DEBIT_ID, subjectCode: SUBJECT_DEBIT_CODE,
      budgetAmountSource: AMOUNT, budgetAmountFunctional: AMOUNT,
      currencyId: CURRENCY, exchangeRate: 1,
    },
    'id',
  );
  const creditLine = await createViaSave(
    page,
    'ErpFinBudgetLine',
    {
      scenarioId: scenario.id, lineNo: 2, orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: PERIOD,
      subjectId: SUBJECT_CREDIT_ID, subjectCode: SUBJECT_CREDIT_CODE,
      budgetAmountSource: AMOUNT, budgetAmountFunctional: AMOUNT,
      currencyId: CURRENCY, exchangeRate: 1,
    },
    'id',
  );
  setup.lineIds.push(debitLine.id, creditLine.id);
  return setup;
}

async function cleanupBudget(page: import('@playwright/test').Page, setup: BudgetSetup): Promise<void> {
  if (!setup) return;
  if (setup.scenarioCode) {
    // BUDGET 凭证（正向 + 红冲）经 billCode=scenario.code 关联，cleanupVoucherByBillCode 覆盖凭证行+凭证+回链
    await cleanupVoucherByBillCode(page, setup.scenarioCode);
  }
  for (const id of setup.lineIds ?? []) {
    await deleteById(page, 'ErpFinBudgetLine', id);
  }
  if (setup.scenarioId != null) {
    await deleteById(page, 'ErpFinBudgetScenario', setup.scenarioId);
  }
}

test.describe('Finance ErpFinBudgetScenario lifecycle + BUDGET shadow voucher browser-layer E2E', () => {
  test('(a) submit: DRAFT → SUBMITTED (docStatus + approveStatus)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup = await createDraftScenario(page);
    try {
      const submitted = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id docStatus approveStatus',
      );
      expect(submitted.docStatus, 'submit should transition docStatus DRAFT → SUBMITTED').toBe('SUBMITTED');
      expect(submitted.approveStatus, 'submit should transition approveStatus → SUBMITTED').toBe('SUBMITTED');

      const after = await verifyState(page, 'ErpFinBudgetScenario', setup.scenarioId, 'docStatus approveStatus');
      expect(after.docStatus, '__get should confirm SUBMITTED after submit').toBe('SUBMITTED');
      expect(after.approveStatus, '__get should confirm approveStatus SUBMITTED after submit').toBe('SUBMITTED');
    } finally {
      await cleanupBudget(page, setup);
    }
  });

  test('(b) approve: SUBMITTED → APPROVED + voucherId + BUDGET voucher lines (DEBIT 6601 / CREDIT 5001)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup = await createDraftScenario(page);
    try {
      await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id');

      const approved = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'approve', { id: setup.scenarioId },
        'id docStatus approveStatus voucherId',
      );
      expect(approved.docStatus, 'approve should transition docStatus → APPROVED').toBe('APPROVED');
      expect(approved.approveStatus, 'approve should transition approveStatus → APPROVED').toBe('APPROVED');
      expect(approved.voucherId, 'approve should write back scenario.voucherId (first BUDGET voucher)').toBeTruthy();

      // BUDGET 影子凭证反查（正向：postingType=BUDGET + reversalOfVoucherId IS NULL）+ 凭证行数值断言
      // 借贷方向按 ErpMdSubject.direction：6601(DEBIT)→借方 debitAmount=100；5001(CREDIT)→贷方 creditAmount=100
      const budgetVoucherId = await findBudgetVoucherIdByCode(page, setup.scenarioCode!, false);
      expect(budgetVoucherId, 'approve should generate forward BUDGET voucher by scenario.code').toBeTruthy();
      await assertVoucherLines(page, budgetVoucherId, [
        { subjectCode: SUBJECT_DEBIT_CODE, dcDirection: 'DEBIT', debitAmount: AMOUNT, creditAmount: 0 },
        { subjectCode: SUBJECT_CREDIT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: AMOUNT },
      ]);
    } finally {
      await cleanupBudget(page, setup);
    }
  });

  test('(c) reject: SUBMITTED → REJECTED (docStatus + approveStatus)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup = await createDraftScenario(page);
    try {
      await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id');

      const rejected = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'reject', { id: setup.scenarioId },
        'id docStatus approveStatus',
      );
      // Processor.reject：docStatus SUBMITTED→REJECTED + approveStatus→REJECTED（状态机 REJECTED 为终态，非回退 DRAFT）
      expect(rejected.docStatus, 'reject should transition docStatus → REJECTED').toBe('REJECTED');
      expect(rejected.approveStatus, 'reject should transition approveStatus → REJECTED').toBe('REJECTED');

      const after = await verifyState(page, 'ErpFinBudgetScenario', setup.scenarioId, 'docStatus approveStatus');
      expect(after.docStatus, '__get should confirm REJECTED after reject').toBe('REJECTED');
    } finally {
      await cleanupBudget(page, setup);
    }
  });

  test('(d) cancel: APPROVED → CANCELLED + reversal BUDGET voucher + original isReversed=true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup = await createDraftScenario(page);
    try {
      await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id');
      await callMutationOk(page, 'ErpFinBudgetScenario', 'approve', { id: setup.scenarioId }, 'id voucherId');

      const cancelled = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'cancel', { id: setup.scenarioId }, 'id docStatus',
      );
      expect(cancelled.docStatus, 'cancel should transition docStatus → CANCELLED').toBe('CANCELLED');

      // 原正向 BUDGET 凭证被标记 isReversed=true（reverse 逐张红冲 + 标记原凭证）
      const fwdVoucherId = await findBudgetVoucherIdByCode(page, setup.scenarioCode!, false);
      expect(fwdVoucherId, 'forward BUDGET voucher should still be resolvable after cancel').toBeTruthy();
      const fwd = await verifyState(page, 'ErpFinVoucher', fwdVoucherId!, 'id postingType isReversed reversalOfVoucherId');
      expect(fwd.postingType, 'forward BUDGET voucher postingType=BUDGET').toBe('BUDGET');
      expect(fwd.isReversed, 'forward BUDGET voucher should be marked isReversed=true after cancel').toBe(true);

      // 红冲 BUDGET 凭证存在（postingType=BUDGET + reversalOfVoucherId IS NOT NULL）+ 红冲凭证行同向取负
      const revVoucherId = await findBudgetVoucherIdByCode(page, setup.scenarioCode!, true);
      expect(revVoucherId, 'cancel should generate reversal BUDGET voucher').toBeTruthy();
      await assertVoucherLines(page, revVoucherId, [
        { subjectCode: SUBJECT_DEBIT_CODE, dcDirection: 'DEBIT', debitAmount: -AMOUNT, creditAmount: 0 },
        { subjectCode: SUBJECT_CREDIT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -AMOUNT },
      ]);
    } finally {
      await cleanupBudget(page, setup);
    }
  });

  test('(e) illegal transition guards: APPROVED→submit / DRAFT→approve → ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup = await createDraftScenario(page);
    try {
      // DRAFT → approve：approve 仅允许 SUBMITTED，DRAFT 非法
      const rej1 = await callMutation(
        page, 'ErpFinBudgetScenario', 'approve', { id: setup.scenarioId }, 'id',
      );
      expect(rej1.errors, 'approve from DRAFT should be rejected').toBeTruthy();
      expect(JSON.stringify(rej1.errors), 'should carry illegal-transition message').toContain('不允许此操作');

      // 推进到 APPROVED 后 → submit：submit 仅允许 DRAFT/REJECTED，APPROVED 非法
      await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id');
      await callMutationOk(page, 'ErpFinBudgetScenario', 'approve', { id: setup.scenarioId }, 'id');
      const rej2 = await callMutation(
        page, 'ErpFinBudgetScenario', 'submit', { id: setup.scenarioId }, 'id',
      );
      expect(rej2.errors, 'submit from APPROVED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej2.errors), 'should carry illegal-transition message').toContain('不允许此操作');
    } finally {
      await cleanupBudget(page, setup);
    }
  });
});
