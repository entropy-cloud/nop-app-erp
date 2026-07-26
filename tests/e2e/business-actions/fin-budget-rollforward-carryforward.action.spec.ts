import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  findBudgetScenarioByCode,
  findBudgetLineAmount,
  countBudgetRollforwardLogs,
  countBudgetCarryForwardLogs,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * Finance ErpFinBudgetScenario 滚动预算自动复制（rollForward）+ 预算结转（carryForward）浏览器层 E2E
 * （plan 2026-07-26-1407-2）。
 *
 * 验证 A2 两入口 `@BizMutation`（rollForward / carryForward）经 GraphQL `/graphql` 全栈可达 +
 * 目标方案字段翻转 + 新方案行金额确定性派生 + RollforwardLog/CarryForwardLog 审计写入：
 *   (a) rollForward FIXED_PERCENTAGE：源行金额 1000 → 目标行 100% 复制 = 1000
 *   (b) rollForward ZERO_BASED：源行金额 1000 → 目标行金额清零 = 0
 *   (c) rollForward INCREMENTAL：源行金额 1000 → 目标行 ×(1+0.05) = 1050
 *   (d) carryForward REMAINING_FULL：budget 1000 − actual 400 = 600
 *   (e) carryForward REMAINING_RATIO：(1000−400) × 0.5 = 300
 *   (f) carryForward USED_FULL：actual = 400
 *   (g) carryForward NONE：不结转 = 0
 *
 * 权威设计（docs/design/finance/budget.md §滚动预算自动复制引擎 + §结转规则引擎）：
 *   - rollForward：源 APPROVED + newFiscalYear > source.fiscalYear → 创建目标方案（fiscalYear/parentScenarioId/
 *     docStatus=DRAFT）+ 按 strategy 复制 BudgetLine（FIXED_PERCENTAGE 100% / ZERO_BASED 清零 /
 *     INCREMENTAL ×(1+config incremental-rate 默认 0.05)）+ 写 RollforwardLog。
 *   - carryForward：源 APPROVED + 目标 DRAFT + 同 orgId/acctSchemaId/currencyId → 按 rule 计算结转金额
 *     （REMAINING_FULL=max(budget−actual,0) / REMAINING_RATIO=remaining×config ratio 默认 0.5 /
 *     USED_FULL=max(actual,0) / NONE=0）+ carriedAmount>0 时目标方案增补 CARRY-FORWARD 行 +
 *     写 BUDGET TRANSFER 凭证 + 源方案 CLOSED + 写 CarryForwardLog。
 *
 * Explore Decision（Phase 1，落盘 plan Execution Decisions 段）：
 *   - **config-gate 启用**：两入口默认 false（`ErpFinBudgetScenarioProcessor.isRollForwardEnabled:179-182` /
 *     `isCarryForwardEnabled:184-187`），playwright.config.ts webServer JVM args 追加
 *     `-Derp-fin.budget-roll-forward-enabled=true -Derp-fin.budget-carry-forward-enabled=true`。
 *   - **直置状态绕状态机**：源方案经 `__save` 直置 APPROVED（绕 submit/approve，避免生成 BUDGET 影子凭证
 *     污染 actual 聚合基线；JUnit seedApprovedScenario 同范式）；目标方案（carryForward）直置 DRAFT。
 *   - **测试专用 subject 隔离**：经 `ErpMdSubject__save` 建唯一 code 科目（EXPENSE/DEBIT/ACTIVE），使
 *     carryForward actual 聚合（`aggregateActualForLine:352-395` 按 subjectId 过滤）仅命中本 spec 的 actual
 *     voucher（fresh-DB 无同 code 种子，零混合）。
 *   - **测试专用 period（rollForward 期间重映射）**：经 `ErpFinAccountingPeriod__save` 建 source 年度 +
 *     target 年度两 OPEN 期间（同 month 使 `remapPeriodId:285-303` 命中）；carryForward 仅单一 period。
 *   - **actual voucher（carryForward REMAINING_FULL/RATIO/USED_FULL）**：经 `ErpFinVoucher__save` 直置
 *     postingType=NORMAL + docStatus=POSTED + isReversed=false（aggregateActualForLine 排除 BUDGET/COMMITMENT），
 *     VoucherLine subjectId 同源 BudgetLine + debitAmount=actual（DEBIT 科目 actual=debit−credit）。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1。自包含隔离：scenario/subject/period/voucher
 * code 均唯一（E2E-RF-/E2E-CF- + ts），cleanup 按反依赖链删除 Log + 凭证 + 行 + 方案 + actual voucher + period + subject。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const SOURCE_AMOUNT = 1000;
const ACTUAL_AMOUNT = 400;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  // Compact: tag + base36(Date.now()) (~8 chars) + seq. Avoids the carry-forward voucher
  // code precision-50 constraint: writeCarryForwardVoucher emits "CARRY-FORWARD-"+sourceCode
  // +"-"+targetCode+"-"+uuid8 (~23 fixed chars) → sourceCode+targetCode ≤ 27. Short single-char
  // tags for scenarios keep combined code well under 50 (1407-1 latent-defect 范式).
  return `${tag}${Date.now().toString(36)}${_seq}`;
}

interface Subject { id: string | number; code: string; }
interface Period { id: string | number; code: string; }
interface Scenario { id: string | number; code: string; }

interface RollForwardSetup {
  subject?: Subject;
  periodSrc?: Period;
  periodTgt?: Period;
  source?: Scenario;
  sourceLineIds: Array<string | number>;
  /** 目标方案（rollForward 产物），cleanup 用。 */
  targetCode?: string;
}

interface CarryForwardSetup {
  subject?: Subject;
  period?: Period;
  source?: Scenario;
  target?: Scenario;
  sourceLineIds: Array<string | number>;
  actualVoucherCode?: string;
  /** carryForward 结转凭证 billCode（carriedAmount>0 时），cleanup 用。 */
  carryBillCode?: string;
}

async function createSubject(page: Page, code: string): Promise<Subject> {
  const s = await createViaSave(
    page, 'ErpMdSubject',
    { code, name: `E2E Subject ${code}`, subjectClass: 'EXPENSE', direction: 'DEBIT', status: 'ACTIVE' },
    'id code',
  );
  return { id: s.id, code };
}

async function createPeriod(page: Page, code: string, year: number, month: number): Promise<Period> {
  const p = await createViaSave(
    page, 'ErpFinAccountingPeriod',
    {
      code, name: code, orgId: ORG, year, month,
      startDate: `${year}-${String(month).padStart(2, '0')}-01`,
      endDate: `${year}-${String(month).padStart(2, '0')}-28`,
      status: 'OPEN',
    },
    'id code',
  );
  return { id: p.id, code };
}

async function createScenario(
  page: Page, code: string, fiscalYear: number, docStatus: string, approveStatus: string,
): Promise<Scenario> {
  const s = await createViaSave(
    page, 'ErpFinBudgetScenario',
    {
      code, name: `E2E Scenario ${code}`, orgId: ORG, acctSchemaId: ACCT_SCHEMA,
      fiscalYear, scenarioType: 'ANNUAL', currencyId: CURRENCY, exchangeRate: 1,
      controlLevel: 'NONE', docStatus, approveStatus,
    },
    'id code',
  );
  return { id: s.id, code };
}

async function addBudgetLine(
  page: Page, scenarioId: string | number, periodId: string | number, subject: Subject, amount: number,
): Promise<string | number> {
  const line = await createViaSave(
    page, 'ErpFinBudgetLine',
    {
      scenarioId: Number(scenarioId), lineNo: 1, orgId: ORG, acctSchemaId: ACCT_SCHEMA,
      periodId: Number(periodId), subjectId: Number(subject.id), subjectCode: subject.code,
      budgetAmountSource: amount, budgetAmountFunctional: amount,
      currencyId: CURRENCY, exchangeRate: 1,
    },
    'id',
  );
  return line.id;
}

/** 直置 actual 凭证（postingType=NORMAL + POSTED + isReversed=false）供 carryForward actual 聚合。 */
async function createActualVoucher(
  page: Page, code: string, periodId: string | number, subject: Subject, amount: number,
): Promise<string> {
  const v = await createViaSave(
    page, 'ErpFinVoucher',
    {
      code, voucherType: 'TRANSFER', postingType: 'NORMAL', voucherDate: '2024-06-15',
      orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: Number(periodId),
      totalDebit: amount, totalCredit: amount, isReversed: false, docStatus: 'POSTED',
    },
    'id',
  );
  await createViaSave(
    page, 'ErpFinVoucherLine',
    {
      voucherId: Number(v.id), lineNo: 1, subjectId: Number(subject.id), subjectCode: subject.code,
      subjectName: subject.code, dcDirection: 'DEBIT', debitAmount: amount, creditAmount: 0,
      currencyId: CURRENCY, exchangeRate: 1, amountSource: amount, amountFunctional: amount,
      acctSchemaId: ACCT_SCHEMA, orgId: ORG,
    },
    'id',
  );
  return code;
}

async function cleanupRollForward(page: Page, setup: RollForwardSetup): Promise<void> {
  if (!setup) return;
  // 反依赖链：RollforwardLog → 目标方案行 → 目标方案 → 源行 → 源方案 → period → subject
  if (setup.source) {
    await deleteByFilter(page, 'ErpFinBudgetRollforwardLog', eqFilter('sourceScenarioId', Number(setup.source.id)));
  }
  if (setup.targetCode) {
    const tgt = await findBudgetScenarioByCode<{ id: string | number }>(page, setup.targetCode, 'id');
    if (tgt) {
      await deleteByFilter(page, 'ErpFinBudgetLine', eqFilter('scenarioId', Number(tgt.id)));
      await deleteById(page, 'ErpFinBudgetScenario', tgt.id);
    }
  }
  for (const id of setup.sourceLineIds ?? []) {
    await deleteById(page, 'ErpFinBudgetLine', id);
  }
  if (setup.source) {
    await deleteById(page, 'ErpFinBudgetScenario', setup.source.id);
  }
  if (setup.periodSrc) {
    await deleteById(page, 'ErpFinAccountingPeriod', setup.periodSrc.id);
  }
  if (setup.periodTgt) {
    await deleteById(page, 'ErpFinAccountingPeriod', setup.periodTgt.id);
  }
  if (setup.subject) {
    await deleteById(page, 'ErpMdSubject', setup.subject.id);
  }
}

async function cleanupCarryForward(page: Page, setup: CarryForwardSetup): Promise<void> {
  if (!setup) return;
  // 反依赖链：CarryForwardLog → 结转凭证（billCode）→ 目标方案行（含 CARRY-FORWARD 行）→
  //           actual 凭证行+凭证 → 源行 → 源方案 + 目标方案 → period → subject
  if (setup.source) {
    await deleteByFilter(page, 'ErpFinBudgetCarryForwardLog', eqFilter('sourceScenarioId', Number(setup.source.id)));
  }
  if (setup.carryBillCode) {
    await cleanupVoucherByBillCode(page, setup.carryBillCode);
  }
  if (setup.target) {
    await deleteByFilter(page, 'ErpFinBudgetLine', eqFilter('scenarioId', Number(setup.target.id)));
  }
  if (setup.actualVoucherCode) {
    // actual 凭证无 billR 回链，按 code 反查凭证后清行+凭证
    const { findFirst } = await import('../orchestration/_helper');
    const v = await findFirst<{ id: string | number }>(page, 'ErpFinVoucher', eqFilter('code', setup.actualVoucherCode), 'id');
    if (v) {
      await deleteByFilter(page, 'ErpFinVoucherLine', eqFilter('voucherId', Number(v.id)));
      await deleteById(page, 'ErpFinVoucher', v.id);
    }
  }
  for (const id of setup.sourceLineIds ?? []) {
    await deleteById(page, 'ErpFinBudgetLine', id);
  }
  if (setup.source) {
    await deleteById(page, 'ErpFinBudgetScenario', setup.source.id);
  }
  if (setup.target) {
    await deleteById(page, 'ErpFinBudgetScenario', setup.target.id);
  }
  if (setup.period) {
    await deleteById(page, 'ErpFinAccountingPeriod', setup.period.id);
  }
  if (setup.subject) {
    await deleteById(page, 'ErpMdSubject', setup.subject.id);
  }
}

test.describe('Finance ErpFinBudgetScenario rollForward (3 strategies) + carryForward (4 rules) browser-layer E2E', () => {
  test('(a) rollForward FIXED_PERCENTAGE: source 1000 → target line 1000 (100% copy) + DRAFT + parentScenarioId + RollforwardLog', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: RollForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.periodSrc = await createPeriod(page, uniq('P'), 2024, 6);
      setup.periodTgt = await createPeriod(page, uniq('Q'), 2025, 6);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.periodSrc.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);

      const target = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'rollForward',
        { id: setup.source.id, newFiscalYear: 2025, strategy: 'FIXED_PERCENTAGE' },
        'id code fiscalYear parentScenarioId docStatus',
      );
      expect(target.fiscalYear, 'target fiscalYear should be 2025').toBe(2025);
      expect(target.docStatus, 'target docStatus should be DRAFT').toBe('DRAFT');
      expect(Number(target.parentScenarioId), 'target parentScenarioId should point to source').toBe(Number(setup.source.id));
      setup.targetCode = target.code;

      // __get 独立反查字段翻转
      const after = await verifyState(
        page, 'ErpFinBudgetScenario', target.id, 'fiscalYear parentScenarioId docStatus approveStatus',
      );
      expect(after.fiscalYear, '__get should confirm fiscalYear 2025').toBe(2025);
      expect(after.docStatus, '__get should confirm DRAFT').toBe('DRAFT');
      expect(after.approveStatus, '__get should confirm approveStatus DRAFT').toBe('DRAFT');
      expect(Number(after.parentScenarioId), '__get should confirm parentScenarioId → source').toBe(Number(setup.source.id));

      // 目标方案行金额 100% 复制 = 1000
      const amount = await findBudgetLineAmount(page, target.id, subjectCode);
      expect(amount, 'FIXED_PERCENTAGE should 100% copy amount = 1000').toBe(SOURCE_AMOUNT);

      // RollforwardLog 审计写入
      const logCount = await countBudgetRollforwardLogs(page, setup.source.id);
      expect(logCount, 'should write >=1 RollforwardLog').toBeGreaterThanOrEqual(1);
    } finally {
      await cleanupRollForward(page, setup);
    }
  });

  test('(b) rollForward ZERO_BASED: source 1000 → target line 0 (amount cleared, structure copied)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: RollForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.periodSrc = await createPeriod(page, uniq('P'), 2024, 7);
      setup.periodTgt = await createPeriod(page, uniq('Q'), 2025, 7);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.periodSrc.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);

      const target = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'rollForward',
        { id: setup.source.id, newFiscalYear: 2025, strategy: 'ZERO_BASED' },
        'id code fiscalYear docStatus',
      );
      setup.targetCode = target.code;

      // ZERO_BASED：行金额清零 = 0（结构仍复制：行存在但 amount=0）
      const amount = await findBudgetLineAmount(page, target.id, subjectCode);
      expect(amount, 'ZERO_BASED should clear amount to 0 (structure copied)').toBe(0);
    } finally {
      await cleanupRollForward(page, setup);
    }
  });

  test('(c) rollForward INCREMENTAL: source 1000 → target line 1050 (×(1+0.05))', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: RollForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.periodSrc = await createPeriod(page, uniq('P'), 2024, 8);
      setup.periodTgt = await createPeriod(page, uniq('Q'), 2025, 8);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.periodSrc.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);

      const target = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'rollForward',
        { id: setup.source.id, newFiscalYear: 2025, strategy: 'INCREMENTAL' },
        'id code fiscalYear docStatus',
      );
      setup.targetCode = target.code;

      // INCREMENTAL：1000 × (1 + 0.05) = 1050（config erp-fin.budget-rollforward-incremental-rate 默认 0.05）
      const amount = await findBudgetLineAmount(page, target.id, subjectCode);
      expect(amount, 'INCREMENTAL should raise amount ×(1+0.05) = 1050').toBe(1050);
    } finally {
      await cleanupRollForward(page, setup);
    }
  });

  test('(d) carryForward REMAINING_FULL: budget 1000 − actual 400 = 600 + source CLOSED + CarryForwardLog', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: CarryForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.period = await createPeriod(page, uniq('P'), 2024, 6);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      setup.target = await createScenario(page, uniq('T'), 2024, 'DRAFT', 'UNSUBMITTED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.period.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);
      setup.actualVoucherCode = await createActualVoucher(page, uniq('A'), setup.period.id, setup.subject, ACTUAL_AMOUNT);

      const updated = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'carryForward',
        { id: setup.source.id, targetScenarioId: setup.target.id, rule: 'REMAINING_FULL' },
        'id docStatus closedAt',
      );
      expect(updated.docStatus, 'source scenario should transition to CLOSED').toBe('CLOSED');
      expect(updated.closedAt, 'source closedAt should be non-null').toBeTruthy();

      const after = await verifyState(page, 'ErpFinBudgetScenario', setup.source.id, 'docStatus closedAt');
      expect(after.docStatus, '__get should confirm source CLOSED').toBe('CLOSED');

      setup.carryBillCode = `CARRY-FORWARD-${setup.source.code}-${setup.target.code}`;
      // REMAINING_FULL：1000 − 400 = 600（appendCarryForwardLines 写 subjectCode=CARRY-FORWARD-{sourceCode}）
      const carried = await findBudgetLineAmount(page, setup.target.id, `CARRY-FORWARD-${setup.source.code}`);
      expect(carried, 'REMAINING_FULL carried amount = budget − actual = 600').toBe(600);

      const logCount = await countBudgetCarryForwardLogs(page, setup.source.id);
      expect(logCount, 'should write >=1 CarryForwardLog').toBeGreaterThanOrEqual(1);
    } finally {
      await cleanupCarryForward(page, setup);
    }
  });

  test('(e) carryForward REMAINING_RATIO: (1000 − 400) × 0.5 = 300', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: CarryForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.period = await createPeriod(page, uniq('P'), 2024, 7);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      setup.target = await createScenario(page, uniq('T'), 2024, 'DRAFT', 'UNSUBMITTED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.period.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);
      setup.actualVoucherCode = await createActualVoucher(page, uniq('A'), setup.period.id, setup.subject, ACTUAL_AMOUNT);

      const updated = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'carryForward',
        { id: setup.source.id, targetScenarioId: setup.target.id, rule: 'REMAINING_RATIO' },
        'id docStatus',
      );
      expect(updated.docStatus, 'source scenario should transition to CLOSED').toBe('CLOSED');

      setup.carryBillCode = `CARRY-FORWARD-${setup.source.code}-${setup.target.code}`;
      // REMAINING_RATIO：(1000−400) × 0.5 = 300（config erp-fin.budget-carry-forward-ratio 默认 0.5）
      const carried = await findBudgetLineAmount(page, setup.target.id, `CARRY-FORWARD-${setup.source.code}`);
      expect(carried, 'REMAINING_RATIO carried amount = remaining × 0.5 = 300').toBe(300);
    } finally {
      await cleanupCarryForward(page, setup);
    }
  });

  test('(f) carryForward USED_FULL: actual = 400', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: CarryForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.period = await createPeriod(page, uniq('P'), 2024, 8);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      setup.target = await createScenario(page, uniq('T'), 2024, 'DRAFT', 'UNSUBMITTED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.period.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);
      setup.actualVoucherCode = await createActualVoucher(page, uniq('A'), setup.period.id, setup.subject, ACTUAL_AMOUNT);

      const updated = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'carryForward',
        { id: setup.source.id, targetScenarioId: setup.target.id, rule: 'USED_FULL' },
        'id docStatus',
      );
      expect(updated.docStatus, 'source scenario should transition to CLOSED').toBe('CLOSED');

      setup.carryBillCode = `CARRY-FORWARD-${setup.source.code}-${setup.target.code}`;
      // USED_FULL：actual = 400
      const carried = await findBudgetLineAmount(page, setup.target.id, `CARRY-FORWARD-${setup.source.code}`);
      expect(carried, 'USED_FULL carried amount = actual = 400').toBe(ACTUAL_AMOUNT);
    } finally {
      await cleanupCarryForward(page, setup);
    }
  });

  test('(g) carryForward NONE: no carry = 0 (no target CARRY-FORWARD line) + source CLOSED + CarryForwardLog', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetScenario-main');

    const setup: CarryForwardSetup = { sourceLineIds: [] };
    try {
      const subjectCode = uniq('B');
      setup.subject = await createSubject(page, subjectCode);
      setup.period = await createPeriod(page, uniq('P'), 2024, 9);
      setup.source = await createScenario(page, uniq('S'), 2024, 'APPROVED', 'APPROVED');
      setup.target = await createScenario(page, uniq('T'), 2024, 'DRAFT', 'UNSUBMITTED');
      const lineId = await addBudgetLine(page, setup.source.id, setup.period.id, setup.subject, SOURCE_AMOUNT);
      setup.sourceLineIds.push(lineId);

      const updated = await callMutationOk(
        page, 'ErpFinBudgetScenario', 'carryForward',
        { id: setup.source.id, targetScenarioId: setup.target.id, rule: 'NONE' },
        'id docStatus',
      );
      expect(updated.docStatus, 'source scenario should transition to CLOSED even for NONE').toBe('CLOSED');

      // NONE：不结转 = 0；目标方案无 CARRY-FORWARD 行（appendCarryForwardLines 未调用）
      const carried = await findBudgetLineAmount(page, setup.target.id, `CARRY-FORWARD-${setup.source.code}`);
      expect(carried, 'NONE should not carry any amount = 0').toBe(0);

      const logCount = await countBudgetCarryForwardLogs(page, setup.source.id);
      expect(logCount, 'should write >=1 CarryForwardLog even for NONE').toBeGreaterThanOrEqual(1);
    } finally {
      await cleanupCarryForward(page, setup);
    }
  });
});
