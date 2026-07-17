import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  deleteById,
  deleteByFilter,
  eqFilter,
} from './_helper';
import { GraphQLClient } from '../pages';
import type { Page } from '@playwright/test';

/**
 * finance ErpFinCashForecast 现金预测 refreshForecast DIRECT 浏览器层 E2E
 * （plan 2026-07-17-2256-1 Phase 2）。
 *
 * 验证 `ErpFinCashForecastBizModel.refreshForecast(fromDate,toDate)` 三源聚合 +
 * 区间清理 + 幂等重写经 GraphQL `/graphql` 的全栈可达性。
 *
 * 权威聚合逻辑（ErpFinCashForecastBizModel:42-68）：
 *   1. 先删区间内旧预测行（forecastDate ∈ [from,to]）。
 *   2. ErpFinArApItem 未核销到期项（status ∈ {OPEN,PARTIAL}）：
 *      RECEIVABLE→INFLOW / PAYABLE→OUTFLOW。
 *   3. ErpFinNotesReceivable 非终态到期项（status ∈ {RECEIVED,DISCOUNTED,ENDORSED,
 *      COLLECTION_PENDING}）→ INFLOW。
 *   4. ErpFinNotesPayable ISSUED 到期项 → OUTFLOW。
 *   返回写入计数 Integer（GraphQL 标量无选择集）。
 *
 * 测试区间 [2026-08-10, 2026-08-15]（晚于 today=2026-07-17，种子 AR/AP OPEN 项
 * dueDate=2026-08-04/05 落区间外，无种子票据），确定性派生可复现。
 *
 * 自包含 setup：__save 直置 status 入口（ORM tagSet="gid,erp.finance" 无
 * use-approval/use-workflow）。cleanup 按 sourceBillCode 逐码删 ErpFinCashForecast
 * 行 + 删 setup 源实体，保护共享 DB 数值断言基线。
 *
 * 种子引用：org=2 / acctSchema=1 / currency=1(CNY) / AR partner=1(CUST-001) /
 * AP partner=3(SUP-001)。
 */
const ORG_ID = 2;
const ACCT_SCHEMA_ID = 1;
const CURRENCY_ID = 1;
const AR_PARTNER_ID = 1; // CUST-001
const AP_PARTNER_ID = 3; // SUP-001
const FROM_DATE = '2026-08-10';
const TO_DATE = '2026-08-15';
const OUT_OF_WINDOW_DATE = '2026-09-01';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now().toString(36)}-${_seq}`;
}

interface SourceRefs {
  arApItemIds: Array<{ id: string; code: string }>;
  notesRecvIds: Array<{ id: string; code: string }>;
  notesPayIds: Array<{ id: string; code: string }>;
  forecastSourceCodes: string[];
}

function emptyRefs(): SourceRefs {
  return { arApItemIds: [], notesRecvIds: [], notesPayIds: [], forecastSourceCodes: [] };
}

async function createArApItem(
  page: Page,
  overrides: Record<string, unknown>,
): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-CF-ARAP');
  return createViaSave(
    page,
    'ErpFinArApItem',
    {
      code,
      orgId: ORG_ID,
      acctSchemaId: ACCT_SCHEMA_ID,
      partnerId: AR_PARTNER_ID,
      sourceBillType: 'AR_INVOICE',
      sourceBillCode: code,
      businessDate: FROM_DATE,
      dueDate: FROM_DATE,
      currencyId: CURRENCY_ID,
      exchangeRate: 1,
      amountSource: 100,
      amountFunctional: 100,
      openAmountSource: 100,
      openAmountFunctional: 100,
      status: 'OPEN',
      ...overrides,
    },
    'id code',
  );
}

async function createNotesReceivable(
  page: Page,
  overrides: Record<string, unknown>,
): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-CF-NR');
  return createViaSave(
    page,
    'ErpFinNotesReceivable',
    {
      code,
      orgId: ORG_ID,
      notesType: 'BANK_ACCEPTANCE',
      notesNo: uniq('NN'),
      currencyId: CURRENCY_ID,
      exchangeRate: 1,
      amountSource: 300,
      amountFunctional: 300,
      partnerId: AR_PARTNER_ID,
      issueDate: FROM_DATE,
      dueDate: FROM_DATE,
      status: 'RECEIVED',
      ...overrides,
    },
    'id code',
  );
}

async function createNotesPayable(
  page: Page,
  overrides: Record<string, unknown>,
): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-CF-NP');
  return createViaSave(
    page,
    'ErpFinNotesPayable',
    {
      code,
      orgId: ORG_ID,
      notesType: 'COMMERCIAL_ACCEPTANCE',
      notesNo: uniq('NN'),
      currencyId: CURRENCY_ID,
      exchangeRate: 1,
      amountSource: 400,
      amountFunctional: 400,
      partnerId: AP_PARTNER_ID,
      issueDate: FROM_DATE,
      dueDate: FROM_DATE,
      status: 'ISSUED',
      ...overrides,
    },
    'id code',
  );
}

async function refreshForecast(
  page: Page,
  from: string,
  to: string,
): Promise<{ count: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation{ ErpFinCashForecast__refreshForecast(fromDate:${JSON.stringify(from)},toDate:${JSON.stringify(to)}) }`,
  );
  const raw = json?.data?.ErpFinCashForecast__refreshForecast;
  const count = raw == null ? null : Number(raw);
  return { count, errors: json?.errors ?? null, json };
}

async function findForecastBySourceCode(
  page: Page,
  sourceCode: string,
): Promise<any[]> {
  const gql = new GraphQLClient(page);
  return gql.findItems<any>(
    'ErpFinCashForecast',
    eqFilter('sourceBillCode', sourceCode),
    'id forecastDate direction sourceBillType sourceBillCode amountSource amountFunctional',
  );
}

async function cleanupRefs(page: Page, refs: SourceRefs): Promise<void> {
  for (const code of refs.forecastSourceCodes) {
    await deleteByFilter(page, 'ErpFinCashForecast', eqFilter('sourceBillCode', code));
  }
  for (const item of refs.arApItemIds) {
    await deleteById(page, 'ErpFinArApItem', item.id);
  }
  for (const note of refs.notesRecvIds) {
    await deleteById(page, 'ErpFinNotesReceivable', note.id);
  }
  for (const note of refs.notesPayIds) {
    await deleteById(page, 'ErpFinNotesPayable', note.id);
  }
}

test.describe('finance ErpFinCashForecast refreshForecast aggregation + idempotent rewrite', () => {
  test('aggregates three sources (AR/AP/notes) with correct direction + amount; out-of-window excluded', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCashForecast-main');
    const refs = emptyRefs();
    try {
      // In-window sources (4 rows expected)
      const ar = await createArApItem(page, {
        direction: 'RECEIVABLE',
        sourceBillType: 'AR_INVOICE',
        partnerId: AR_PARTNER_ID,
        dueDate: '2026-08-10',
        amountSource: 100,
        amountFunctional: 100,
        openAmountSource: 100,
        openAmountFunctional: 100,
        status: 'OPEN',
      });
      refs.arApItemIds.push(ar);
      refs.forecastSourceCodes.push(ar.code);

      const ap = await createArApItem(page, {
        direction: 'PAYABLE',
        sourceBillType: 'AP_INVOICE',
        partnerId: AP_PARTNER_ID,
        dueDate: '2026-08-12',
        amountSource: 200,
        amountFunctional: 200,
        openAmountSource: 200,
        openAmountFunctional: 200,
        status: 'OPEN',
      });
      refs.arApItemIds.push(ap);
      refs.forecastSourceCodes.push(ap.code);

      const nr = await createNotesReceivable(page, { dueDate: '2026-08-13', amountSource: 300, amountFunctional: 300 });
      refs.notesRecvIds.push(nr);
      refs.forecastSourceCodes.push(nr.code);

      const np = await createNotesPayable(page, { dueDate: '2026-08-14', amountSource: 400, amountFunctional: 400 });
      refs.notesPayIds.push(np);
      refs.forecastSourceCodes.push(np.code);

      // Out-of-window control (OPEN AR but dueDate outside [FROM, TO])
      const outOfWindow = await createArApItem(page, {
        direction: 'RECEIVABLE',
        sourceBillType: 'AR_INVOICE',
        partnerId: AR_PARTNER_ID,
        dueDate: OUT_OF_WINDOW_DATE,
        amountSource: 999,
        amountFunctional: 999,
        openAmountSource: 999,
        openAmountFunctional: 999,
        status: 'OPEN',
      });
      refs.arApItemIds.push(outOfWindow);
      // NOT added to forecastSourceCodes — cleanup via arApItemIds delete only

      const { count, errors } = await refreshForecast(page, FROM_DATE, TO_DATE);
      expect(errors, 'refreshForecast should not return GraphQL errors').toBeNull();
      expect(count, 'refreshForecast count should be >= 4 in-window sources').toBeGreaterThanOrEqual(4);

      // AR → INFLOW
      const arRows = await findForecastBySourceCode(page, ar.code);
      expect(arRows.length, 'AR item produces exactly 1 forecast row').toBe(1);
      expect(arRows[0].direction, 'AR RECEIVABLE → INFLOW').toBe('INFLOW');
      expect(Number(arRows[0].amountSource), 'AR amount matches source').toBe(100);
      expect(arRows[0].forecastDate, 'AR forecastDate = dueDate').toBe('2026-08-10');
      expect(arRows[0].sourceBillType, 'AR sourceBillType preserved').toBe('AR_INVOICE');

      // AP → OUTFLOW
      const apRows = await findForecastBySourceCode(page, ap.code);
      expect(apRows.length, 'AP item produces exactly 1 forecast row').toBe(1);
      expect(apRows[0].direction, 'AP PAYABLE → OUTFLOW').toBe('OUTFLOW');
      expect(Number(apRows[0].amountSource), 'AP amount matches source').toBe(200);
      expect(apRows[0].forecastDate, 'AP forecastDate = dueDate').toBe('2026-08-12');
      expect(apRows[0].sourceBillType, 'AP sourceBillType preserved').toBe('AP_INVOICE');

      // Notes Receivable → INFLOW
      const nrRows = await findForecastBySourceCode(page, nr.code);
      expect(nrRows.length, 'Notes Receivable produces exactly 1 forecast row').toBe(1);
      expect(nrRows[0].direction, 'Notes Receivable → INFLOW').toBe('INFLOW');
      expect(Number(nrRows[0].amountSource), 'NR amount matches source').toBe(300);
      expect(nrRows[0].forecastDate, 'NR forecastDate = dueDate').toBe('2026-08-13');
      expect(nrRows[0].sourceBillType, 'NR sourceBillType = NOTES_RECEIVABLE').toBe('NOTES_RECEIVABLE');

      // Notes Payable → OUTFLOW
      const npRows = await findForecastBySourceCode(page, np.code);
      expect(npRows.length, 'Notes Payable produces exactly 1 forecast row').toBe(1);
      expect(npRows[0].direction, 'Notes Payable → OUTFLOW').toBe('OUTFLOW');
      expect(Number(npRows[0].amountSource), 'NP amount matches source').toBe(400);
      expect(npRows[0].forecastDate, 'NP forecastDate = dueDate').toBe('2026-08-14');
      expect(npRows[0].sourceBillType, 'NP sourceBillType = NOTES_ENDORSED').toBe('NOTES_ENDORSED');

      // Out-of-window source excluded
      const oopRows = await findForecastBySourceCode(page, outOfWindow.code);
      expect(oopRows.length, 'out-of-window AR produces 0 forecast rows').toBe(0);
    } finally {
      await cleanupRefs(page, refs);
    }
  });

  test('terminal status filter: SETTLED AR item does not participate in forecast', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCashForecast-main');
    const refs = emptyRefs();
    try {
      const settled = await createArApItem(page, {
        direction: 'RECEIVABLE',
        sourceBillType: 'AR_INVOICE',
        partnerId: AR_PARTNER_ID,
        dueDate: '2026-08-11',
        amountSource: 500,
        amountFunctional: 500,
        openAmountSource: 0,
        openAmountFunctional: 0,
        status: 'SETTLED',
      });
      refs.arApItemIds.push(settled);
      refs.forecastSourceCodes.push(settled.code);

      const { errors } = await refreshForecast(page, FROM_DATE, TO_DATE);
      expect(errors, 'refreshForecast should not return GraphQL errors').toBeNull();

      const rows = await findForecastBySourceCode(page, settled.code);
      expect(rows.length, 'SETTLED AR produces 0 forecast rows').toBe(0);
    } finally {
      await cleanupRefs(page, refs);
    }
  });

  test('idempotent rewrite: second refresh clears stale rows and rewrites without accumulation', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCashForecast-main');
    const refs = emptyRefs();
    try {
      const ar = await createArApItem(page, {
        direction: 'RECEIVABLE',
        sourceBillType: 'AR_INVOICE',
        partnerId: AR_PARTNER_ID,
        dueDate: '2026-08-10',
        amountSource: 100,
        amountFunctional: 100,
        openAmountSource: 100,
        openAmountFunctional: 100,
        status: 'OPEN',
      });
      refs.arApItemIds.push(ar);
      refs.forecastSourceCodes.push(ar.code);

      const { count: count1, errors: errors1 } = await refreshForecast(page, FROM_DATE, TO_DATE);
      expect(errors1, 'first refresh should not error').toBeNull();
      expect(count1, 'first refresh count >= 1').toBeGreaterThanOrEqual(1);

      const rowsAfterFirst = await findForecastBySourceCode(page, ar.code);
      expect(rowsAfterFirst.length, '1 forecast row after first refresh').toBe(1);

      const { count: count2, errors: errors2 } = await refreshForecast(page, FROM_DATE, TO_DATE);
      expect(errors2, 'second refresh should not error').toBeNull();
      expect(count2, 'second refresh count == first (idempotent, no accumulation)').toBe(count1);

      const rowsAfterSecond = await findForecastBySourceCode(page, ar.code);
      expect(rowsAfterSecond.length, 'still exactly 1 forecast row after second refresh (cleared + rewritten)').toBe(1);
      expect(rowsAfterSecond[0].direction, 'rewritten row preserves direction').toBe('INFLOW');
      expect(Number(rowsAfterSecond[0].amountSource), 'rewritten row preserves amount').toBe(100);
    } finally {
      await cleanupRefs(page, refs);
    }
  });
});
