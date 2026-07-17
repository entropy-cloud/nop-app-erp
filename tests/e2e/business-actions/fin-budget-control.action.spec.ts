import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  findPageTotal,
  findFirst,
  deleteByFilter,
  deleteById,
  GraphQLClient,
} from './_helper';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * Finance 预算控制 hook（采购订单 approve）+ getBudgetVsActual 对比查询浏览器层 E2E
 * （plan 2026-07-14-0606-1 Phase 2）。
 *
 * 验证预算控制 SPI（`IErpFinBudgetControlBiz.check`）经 `ErpPurOrderProcessor.validateBusinessRulesForApprove`
 * 钩子在采购订单 approve 时触发（config-gated `erp-fin.budget-check-enabled=true`）：
 *   (a) HARD 阻断（负路径）：scenario.controlLevel=HARD + 预算余量 < 订单含税合计 → approve 抛 ERR_BUDGET_EXCEEDED
 *       + approveStatus 保持 SUBMITTED + 无 ControlLog 持久化（@BizMutation 事务回滚）
 *   (b) WARN 放行（正路径）：scenario.controlLevel=WARN → approve 放行（APPROVED）+ ControlLog actionResult=WARNED 持久化
 *   (c) getBudgetVsActual 对比查询：approve 预算方案（BUDGET 凭证存预算额）→ 返回 BudgetVsActualRow 结构非空
 *       + budgetAmount 匹配 setup 值
 *
 * 权威设计（docs/design/finance/budget.md §业务规则2/4/5/8）：
 *   - 控制级别 controlLevel 来自命中的 APPROVED 预算方案 `scenario.getControlLevel()`（实体字段，非 config）：
 *     NONE→PASS；WARN→不足时 WARNED 写日志放行；HARD→不足时 BLOCKED 抛异常。
 *   - 余量 = budgetBalance(BUDGET 凭证行) − actualBalance(NORMAL 凭证行)，按 (subjectId + costCenterId + periodId) 维度聚合。
 *   - 采购订单无科目维度，按 config `erp-fin.budget-purchase-expense-subject-code`（=6601 销售费用）解析科目，
 *     按订单 businessDate 解析会计期间，对订单 totalAmountWithTax 校验。
 *
 * Explore Decision（Phase 2 触发点 + 科目匹配 + controlLevel 路径 + ControlLog 持久性核实）：
 *   - **触发点**：ErpPurOrderProcessor.validateBusinessRulesForApprove → runBudgetCheckHook（config 双门控：
 *     budget-check-enabled=true + budget-purchase-expense-subject-code=6601）。科目 6601 经 resolveBudgetSubjectId
 *     findByCode 解析（种子 id=8, DEBIT 方向）；期间经 resolvePeriodId(businessDate) 解析（2026-07 → period id=1）。
 *   - **controlLevel 路径**：controlLevel 来自 scenario 实体字段（ErpFinBudgetControlBiz.java:82 读 scenario.getControlLevel()），
 *     非 config；HARD→writeControlLog(BLOCKED)+throw，WARN→writeControlLog(WARNED)+放行。
 *   - **ControlLog 持久性（关键裁定）**：HARD 路径的 writeControlLog 与 throw 同处 `@BizMutation` approve 事务内，
 *     NopException（RuntimeException）触发事务回滚 → BLOCKED 日志**不持久化**（与后端 TestErpPurBudgetControlIntegration
 *     testPurchaseOrderHardBlocked 仅断言 -1+ErrorCode、不断言日志一致；WARN testPurchaseOrderWarnPassedWithLog 才断言日志）。
 *     故 HARD 断言无持久化日志（事务完整性），WARN 断言 WARNED 日志持久化。此裁定经实际运行验证。
 *   - **余量确定性**：budget line amount=100（小值），订单 totalAmountWithTax=200（>余量必触发）；
 *     actualBalance 对 6601 在种子期间≈0（种子 NORMAL 凭证不打 6601），available≈100<200，双路径均触发。
 *
 * 科目依赖（Infrastructure And Config Prereqs）：webServer JVM arg 追加
 *   `-Derp-fin.budget-check-enabled=true` + `-Derp-fin.budget-purchase-expense-subject-code=6601`（playwright.config.ts）。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN，endDate=2026-07-31，
 *   PO businessDate=2026-07-15 落此期间）/ supplier SUP-001 id=3（ACTIVE）/ warehouse WH-RAW id=2 / MAT-001 id=1 / UOM PCS id=1。
 * 自包含隔离：每用例独立建预算方案+行（code 唯一 E2E-BUDCTL-*），cleanup 删 ControlLog + BUDGET 凭证 + 预算行 + 方案 + PO 行 + PO。
 * 向后兼容：budget-check-enabled=true 对无匹配预算行的维度（其余 e2e PO）返回 PASS，不影响既有套件。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
const SUPPLIER = 3;       // SUP-001 北方钢铁供应商（ACTIVE）
const WAREHOUSE = 2;      // WH-RAW
const MAT_1 = 1;          // MAT-001
const UOM = 1;            // PCS
/** 种子费用科目 6601 销售费用（id=8, EXPENSE, DEBIT）—— 预算控制采购费用科目。 */
const PURCHASE_EXPENSE_SUBJECT_ID = 8;
const PURCHASE_EXPENSE_SUBJECT_CODE = '6601';
const BUDGET_AMOUNT = 100;   // 预算余量（小值）
const ORDER_AMOUNT = 200;    // 订单含税合计（> 预算余量，必触发控制）
const PO_BDATE = '2026-07-15';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface Ctx {
  scenarioCode?: string;
  scenarioId?: string | number;
  lineIds: Array<string | number>;
  poCode?: string;
  poId?: string | number;
  poLineIds: Array<string | number>;
}

function newCtx(): Ctx {
  return { lineIds: [], poLineIds: [] };
}

/** 建+submit+approve 预算方案（生成 BUDGET 凭证存预算余量），controlLevel 决定控制路径。 */
async function setupApprovedBudgetScenario(
  page: import('@playwright/test').Page,
  controlLevel: 'HARD' | 'WARN' | 'NONE',
): Promise<Ctx> {
  const ctx = newCtx();
  const code = uniq(`E2E-BUDCTL-SCN-${controlLevel}`);
  const scenario = await createViaSave(
    page,
    'ErpFinBudgetScenario',
    {
      code, name: `E2E Budget Control ${controlLevel} ${code}`,
      orgId: ORG, acctSchemaId: ACCT_SCHEMA, fiscalYear: 2026, scenarioType: 'ANNUAL',
      currencyId: CURRENCY, exchangeRate: 1, controlLevel,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
    },
    'id code',
  );
  ctx.scenarioId = scenario.id;
  ctx.scenarioCode = code;

  // 预算行：科目 6601 + period 1 + costCenterId=null（PO 检查传 null，须 isNull 匹配）
  const line = await createViaSave(
    page,
    'ErpFinBudgetLine',
    {
      scenarioId: scenario.id, lineNo: 1, orgId: ORG, acctSchemaId: ACCT_SCHEMA, periodId: PERIOD,
      subjectId: PURCHASE_EXPENSE_SUBJECT_ID, subjectCode: PURCHASE_EXPENSE_SUBJECT_CODE,
      budgetAmountSource: BUDGET_AMOUNT, budgetAmountFunctional: BUDGET_AMOUNT,
      currencyId: CURRENCY, exchangeRate: 1,
    },
    'id',
  );
  ctx.lineIds.push(line.id);

  await callMutationOk(page, 'ErpFinBudgetScenario', 'submit', { id: scenario.id }, 'id');
  await callMutationOk(page, 'ErpFinBudgetScenario', 'approve', { id: scenario.id }, 'id voucherId');
  return ctx;
}

/** 建采购订单（totalAmountWithTax=ORDER_AMOUNT，超预算）+ 行，返回 ctx 补充 poCode/poId/poLineIds。 */
async function setupPurchaseOrder(page: import('@playwright/test').Page, ctx: Ctx): Promise<Ctx> {
  const code = uniq('E2E-BUDCTL-PO');
  const po = await createViaSave(
    page,
    'ErpPurOrder',
    {
      code, orgId: ORG, supplierId: SUPPLIER, warehouseId: WAREHOUSE,
      businessDate: PO_BDATE, currencyId: CURRENCY, exchangeRate: 1,
      totalAmount: ORDER_AMOUNT, totalAmountWithTax: ORDER_AMOUNT,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED', posted: false,
    },
    'id approveStatus',
  );
  ctx.poCode = code;
  ctx.poId = po.id;
  const poLine = await createViaSave(
    page,
    'ErpPurOrderLine',
    {
      orderId: po.id, lineNo: 1, materialId: MAT_1, uoMId: UOM,
      quantity: 10, unitPrice: 20, amount: ORDER_AMOUNT,
    },
    'id',
  );
  ctx.poLineIds.push(poLine.id);
  return ctx;
}

async function cleanupCtx(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  // ControlLog（WARN 路径持久化；HARD 路径回滚无残留，删除幂等）
  if (ctx.poCode) {
    await deleteByFilter(page, 'ErpFinBudgetControlLog', eqFilter('sourceBillCode', ctx.poCode));
  }
  // PO 行 + PO
  for (const id of ctx.poLineIds ?? []) {
    await deleteById(page, 'ErpPurOrderLine', id);
  }
  if (ctx.poId != null) {
    await deleteById(page, 'ErpPurOrder', ctx.poId);
  }
  // BUDGET 凭证（正向，scenario 未 cancel；HARD/WARN/QUERY 用例不 cancel）
  if (ctx.scenarioCode) {
    await cleanupVoucherByBillCode(page, ctx.scenarioCode);
  }
  // 预算行 + 方案
  for (const id of ctx.lineIds ?? []) {
    await deleteById(page, 'ErpFinBudgetLine', id);
  }
  if (ctx.scenarioId != null) {
    await deleteById(page, 'ErpFinBudgetScenario', ctx.scenarioId);
  }
}

test.describe('Finance budget control hook (PO approve) + getBudgetVsActual browser-layer E2E', () => {
  test('(a) HARD block: PO approve over budget → ERR_BUDGET_EXCEEDED + approveStatus SUBMITTED + no persistent ControlLog', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const ctx = await setupApprovedBudgetScenario(page, 'HARD');
    try {
      await setupPurchaseOrder(page, ctx);

      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: ctx.poId }, 'id approveStatus');
      const beforeApprove = await verifyState(page, 'ErpPurOrder', ctx.poId!, 'approveStatus');
      expect(beforeApprove.approveStatus, 'PO should be SUBMITTED before approve').toBe('SUBMITTED');

      // approve 超 HARD 预算 → 抛 ERR_BUDGET_EXCEEDED（message token 预算超支）
      const rej = await callMutation(page, 'ErpPurOrder', 'approve', { id: ctx.poId }, 'id approveStatus');
      expect(rej.errors, 'HARD over-budget approve should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'should carry budget-exceeded message').toContain('预算超支');

      // approveStatus 不变（doApprove 未达，事务回滚）
      const afterApprove = await verifyState(page, 'ErpPurOrder', ctx.poId!, 'approveStatus');
      expect(afterApprove.approveStatus, 'PO approveStatus should remain SUBMITTED after HARD block').toBe('SUBMITTED');

      // ControlLog 不持久化：writeControlLog(BLOCKED) 与 throw 同处 @BizMutation 事务，NopException 触发回滚
      // （后端 TestErpPurBudgetControlIntegration.testPurchaseOrderHardBlocked 不断言日志，佐证回滚语义）
      const blockedLogCount = await findPageTotal(
        page, 'ErpFinBudgetControlLog', eqFilter('sourceBillCode', ctx.poCode!),
      );
      expect(blockedLogCount, 'HARD block ControlLog should not persist (transaction rollback)').toBe(0);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) WARN pass: PO approve over budget → APPROVED + ControlLog WARNED persists', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const ctx = await setupApprovedBudgetScenario(page, 'WARN');
    try {
      await setupPurchaseOrder(page, ctx);

      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: ctx.poId }, 'id approveStatus');

      // WARN 超预算 → 放行（approve 成功提交事务，ControlLog 持久化）
      const approved = await callMutationOk(page, 'ErpPurOrder', 'approve', { id: ctx.poId }, 'id approveStatus');
      expect(approved.approveStatus, 'WARN over-budget approve should pass (APPROVED)').toBe('APPROVED');

      // ControlLog actionResult=WARNED 持久化（approve 提交事务）
      const log = await findFirst<any>(
        page, 'ErpFinBudgetControlLog',
        eqFilter('sourceBillCode', ctx.poCode!),
        'actionResult sourceBillType requestedAmount availableAmount',
      );
      expect(log, 'WARN path should persist a ControlLog by sourceBillCode').toBeTruthy();
      expect(log.actionResult, 'ControlLog actionResult should be WARNED').toBe('WARNED');
      expect(log.sourceBillType, 'ControlLog sourceBillType should be PURCHASE_ORDER').toBe('PURCHASE_ORDER');
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(c) getBudgetVsActual: approve budget scenario → BudgetVsActualRow non-empty + budgetAmount matches setup', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBudgetLine-main');

    const ctx = await setupApprovedBudgetScenario(page, 'NONE');
    try {
      // approve 后 BUDGET 凭证存预算额；getBudgetVsActual 按 postingType 聚合 BUDGET(预算)/NORMAL(实际)。
      // 返回 List<BudgetVsActualRow>（复杂类型，须显式 selection set；callQuery 不带 selection 故直接构造 query）。
      const json: any = await new GraphQLClient(page).raw(
        `query{ ErpFinBudgetLine__getBudgetVsActual(acctSchemaId:${ACCT_SCHEMA},periodId:${PERIOD},subjectId:${PURCHASE_EXPENSE_SUBJECT_ID}){ subjectId subjectCode subjectName budgetAmount actualAmount availableAmount } }`,
      );
      expect(json?.errors, `getBudgetVsActual should not return GraphQL errors: ${JSON.stringify(json?.errors)}`).toBeFalsy();
      const rows: any[] = json?.data?.ErpFinBudgetLine__getBudgetVsActual ?? [];
      expect(Array.isArray(rows), 'getBudgetVsActual should return a list of BudgetVsActualRow').toBe(true);

      // 命中 6601 的行（按 subjectId 过滤；聚合维度 subjectId|costCenterId|projectId）
      const row = rows.find((r) => Number(r.subjectId) === PURCHASE_EXPENSE_SUBJECT_ID);
      expect(row, 'getBudgetVsActual should include a row for subject 6601').toBeTruthy();
      expect(Number(row.budgetAmount), 'budgetAmount should include setup BUDGET voucher (>= setup)').toBeGreaterThanOrEqual(BUDGET_AMOUNT);
      expect(Number(row.actualAmount), 'actualAmount should be a non-negative number').toBeGreaterThanOrEqual(0);
      expect(Number(row.availableAmount), 'availableAmount should be a number').toBeGreaterThanOrEqual(0);
      expect(row.subjectCode, 'row.subjectCode should match 6601').toBe(PURCHASE_EXPENSE_SUBJECT_CODE);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
