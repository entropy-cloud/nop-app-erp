import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  verifyState,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  GraphQLClient,
  findIntercompanyMatchByPairKey,
  findEliminationCandidates,
  findEliminationVoucherId,
} from './_helper';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * Finance 公司间配对 + 合并抵消浏览器层 E2E（plan 2026-07-26-1407-1）。
 *
 * 验证 A3 公司间配对 + 合并抵消四入口经 GraphQL `/graphql` 端到端可达：
 *   - `ErpFinIntercompanyMatch__runMatching(periodId)` @BizMutation → int
 *     按 `ErpFinVoucherBillR.billCode` 配对 INTERCOMPANY_SALE/PURCHASE 凭证对，
 *     金额一致 → status=MATCHED + matchedAmount=min(sale,purchase)；差额 → status=DIFF + diffAmount。
 *   - `ErpFinIntercompanyMatch__checkDualSideConsistency(pairKey, periodId)` @BizQuery → DualSideDiffReport
 *     返回按 pairKey(+periodId) 反查 match 记录生成的 DualSideDiffRow[]。
 *   - `ErpFinConsolidationElimination__generateEliminationCandidates(periodId)` @BizMutation → int
 *     config-gated `erp-fin.consolidation-elimination-enabled=true`；扫描 MATCHED 记录写入
 *     AR_AP + REVENUE_COST 两类 CANDIDATE（INVENTORY_PROFIT 默认 off，Non-Goal）。
 *   - `ErpFinConsolidationElimination__postElimination(candidateId)` @BizMutation → Long voucherId
 *     生成 DRAFT 抵消凭证（ErpFinVoucher docStatus=DRAFT voucherType=TRANSFER）+ candidate 状态翻转为 DRAFT_VOUCHER。
 *
 * 5 用例镜像 JUnit `TestErpFinIntercompanyMatchingAndElimination` 5 场景（MATCHED / DIFF=200 /
 * checkDualSideConsistency 非空报告 / AR_AP+REVENUE_COST 两类候选 / postElimination DRAFT 凭证+状态翻转）。
 *
 * **自包含 setup（直置配对凭证，Phase 1 Decision）**：经 `ErpFinAccountingPeriod__save` 建测试专用 OPEN
 * 期间（unique code+ts，使 `runMatching(myPeriodId)` 仅扫描本 spec 凭证，零跨 spec 干扰）+
 * `ErpFinVoucher__save` 直置 SALE/PURCHASE 配对凭证（voucherType=TRANSFER/docStatus=POSTED）+
 * `ErpFinVoucherBillR__save` 挂 billType/billCode（配对键）。
 *
 * 权威实现：
 *   - `ErpFinIntercompanyMatchBizModel.runMatching:56-97`（配对键 + MATCHED/DIFF 判定）
 *   - `ErpFinIntercompanyMatchBizModel.checkDualSideConsistency:152-188`（DualSideDiffReport 生成）
 *   - `ErpFinConsolidationEliminationBizModel.generateEliminationCandidates:55-127`（config-gate + 3 类候选扫描）
 *   - `ErpFinConsolidationEliminationBizModel.postElimination:131-157`（DRAFT 凭证 + 状态翻转）
 *   - `ErpFinConsolidationEliminationBizModel.writeDraftEliminationVoucher:171-248`（Dr/Cr 两行 + 业财回链）
 *
 * config-gated：`erp-fin.consolidation-elimination-enabled=true`（playwright.config.ts webServer JVM arg，默认 false）。
 * `runMatching`/`checkDualSideConsistency` 无 config gate 不受影响。
 */

const ORG_GROUP = 1; // 种子 GROUP 法人根（period/voucher orgId，对齐 JUnit seedOpenPeriod orgId=1）
const ORG_CO = 2; // 种子 ERP-CO COMPANY（PURCHASE 侧 voucher orgId，对齐 JUnit orgId=2）
const ACCT_SCHEMA = 1; // 种子 ACCT-FIN-01
const BDATE = '2026-07-26'; // 凭证日期（落在测试期间内）

const INTERCOMPANY_SALE = 'INTERCOMPANY_SALE';
const INTERCOMPANY_PURCHASE = 'INTERCOMPANY_PURCHASE';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

// ---------- 自包含 setup ----------

interface PeriodCtx {
  periodId: string;
  periodCode: string;
}

async function createTestPeriod(page: import('@playwright/test').Page, tag: string): Promise<PeriodCtx> {
  const code = uniq(`E2E-IC-MATCH-${tag}`);
  const saved = await createViaSave(
    page,
    'ErpFinAccountingPeriod',
    {
      code,
      name: `E2E IC Match Period ${code}`,
      orgId: ORG_GROUP,
      year: 2026,
      month: 7,
      startDate: '2026-07-01',
      endDate: '2026-07-31',
      quarter: 3,
      isAdjustment: false,
      status: 'OPEN',
    },
    'id code',
  );
  return { periodId: saved.id, periodCode: saved.code };
}

interface VoucherCtx {
  voucherId: string;
  voucherCode: string;
  billRId: string;
}

async function seedIntercompanyVoucher(
  page: import('@playwright/test').Page,
  billType: 'INTERCOMPANY_SALE' | 'INTERCOMPANY_PURCHASE',
  orgId: number,
  periodId: string,
  amount: number,
  billCode: string,
  tag: string,
): Promise<VoucherCtx> {
  const voucherCode = uniq(`IC-${billType === INTERCOMPANY_SALE ? 'S' : 'P'}-${tag}`);
  const voucher = await createViaSave(
    page,
    'ErpFinVoucher',
    {
      code: voucherCode,
      voucherType: 'TRANSFER',
      voucherDate: BDATE,
      orgId,
      acctSchemaId: ACCT_SCHEMA,
      periodId,
      totalDebit: amount,
      totalCredit: amount,
      isReversed: false,
      docStatus: 'POSTED',
    },
    'id code',
  );
  const billR = await createViaSave(
    page,
    'ErpFinVoucherBillR',
    {
      voucherId: voucher.id,
      billType,
      billCode,
    },
    'id',
  );
  return { voucherId: voucher.id, voucherCode: voucher.code, billRId: billR.id };
}

// ---------- scalar-return @BizMutation 原语（gql.raw 无选择集，对齐 finance-voucher-post 范式） ----------

async function runMatchingRaw(
  page: import('@playwright/test').Page,
  periodId: string | number,
): Promise<{ count: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation{ ErpFinIntercompanyMatch__runMatching(periodId:${Number(periodId)}) }`,
  );
  return {
    count: json?.data?.ErpFinIntercompanyMatch__runMatching ?? null,
    errors: json?.errors ?? null,
    json,
  };
}

async function generateEliminationCandidatesRaw(
  page: import('@playwright/test').Page,
  periodId: string | number,
): Promise<{ count: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation{ ErpFinConsolidationElimination__generateEliminationCandidates(periodId:${Number(periodId)}) }`,
  );
  return {
    count: json?.data?.ErpFinConsolidationElimination__generateEliminationCandidates ?? null,
    errors: json?.errors ?? null,
    json,
  };
}

async function postEliminationRaw(
  page: import('@playwright/test').Page,
  candidateId: string | number,
): Promise<{ voucherId: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation{ ErpFinConsolidationElimination__postElimination(candidateId:${Number(candidateId)}) }`,
  );
  const raw = json?.data?.ErpFinConsolidationElimination__postElimination;
  return {
    voucherId: raw == null ? null : Number(raw),
    errors: json?.errors ?? null,
    json,
  };
}

async function checkDualSideConsistencyRaw(
  page: import('@playwright/test').Page,
  pairKey: string,
  periodId: string | number,
): Promise<{ report: any | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `query{ ErpFinIntercompanyMatch__checkDualSideConsistency(pairKey:${JSON.stringify(pairKey)},periodId:${Number(periodId)}){ direction consistent rows{ partnerId financeSettled domainSettled diff status } } }`,
  );
  return {
    report: json?.data?.ErpFinIntercompanyMatch__checkDualSideConsistency ?? null,
    errors: json?.errors ?? null,
    json,
  };
}

// ---------- cleanup（反向依赖顺序） ----------

interface ScenarioCtx {
  periodId?: string | number | null;
  periodCode?: string;
  pairKey?: string;
  saleVoucher?: VoucherCtx | null;
  purchaseVoucher?: VoucherCtx | null;
  candidateIds?: (string | number)[];
}

async function cleanupScenario(page: import('@playwright/test').Page, ctx: ScenarioCtx): Promise<void> {
  if (!ctx) return;
  // 1. DRAFT 抵消凭证（postElimination 产物，candidate.code 反查 bill_r）
  for (const candId of ctx.candidateIds ?? []) {
    try {
      const cand = await verifyState(
        page, 'ErpFinConsolidationElimination', candId, 'id code draftVoucherId status',
      );
      if (cand?.code) {
        await cleanupVoucherByBillCode(page, cand.code);
      }
    } catch {
      // tolerant: candidate may already be deleted
    }
  }
  // 2. ErpFinConsolidationElimination 候选（含 DRAFT_VOUCHER 状态行）
  if (ctx.periodId != null) {
    await deleteByFilter(page, 'ErpFinConsolidationElimination', eqFilter('periodId', Number(ctx.periodId)));
  }
  // 3. ErpFinIntercompanyMatch 配对记录
  if (ctx.periodId != null) {
    await deleteByFilter(page, 'ErpFinIntercompanyMatch', eqFilter('periodId', Number(ctx.periodId)));
  }
  // 4. SALE/PURCHASE 配对凭证（bill_r + voucher）
  for (const v of [ctx.saleVoucher, ctx.purchaseVoucher]) {
    if (!v) continue;
    await deleteById(page, 'ErpFinVoucherBillR', v.billRId).catch(() => {});
    await deleteById(page, 'ErpFinVoucher', v.voucherId).catch(() => {});
  }
  // 5. ErpFinAccountingPeriod
  if (ctx.periodId != null) {
    await deleteById(page, 'ErpFinAccountingPeriod', ctx.periodId).catch(() => {});
  }
}

// ---------- spec ----------

test.describe('Finance intercompany matching + consolidation elimination browser-layer E2E', () => {
  test('(1) runMatching MATCHED: equal SALE/PURCHASE amounts → status=MATCHED + matchedAmount', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinIntercompanyMatch-main');

    const pairKey = uniq('E2E-PAIR-MATCHED');
    const AMOUNT = 1000;
    const ctx: ScenarioCtx = { pairKey };
    try {
      const period = await createTestPeriod(page, 'MATCHED');
      ctx.periodId = period.periodId;
      ctx.periodCode = period.periodCode;

      ctx.saleVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_SALE, ORG_GROUP, period.periodId, AMOUNT, pairKey, 'MATCHED-SALE',
      );
      ctx.purchaseVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_PURCHASE, ORG_CO, period.periodId, AMOUNT, pairKey, 'MATCHED-PUR',
      );

      const { count, errors } = await runMatchingRaw(page, period.periodId);
      expect(errors, `runMatching should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(count, 'runMatching should return positive count for matched pair').toBeGreaterThan(0);

      const match = await findIntercompanyMatchByPairKey<any>(
        page, pairKey, Number(period.periodId),
        'id pairKey periodId matchedAmount diffAmount status',
      );
      expect(match, 'ErpFinIntercompanyMatch record should exist for matched pair').toBeTruthy();
      expect(match!.status, 'matched pair status should be MATCHED').toBe('MATCHED');
      expect(Number(match!.matchedAmount), 'matchedAmount=min(sale,purchase)=1000').toBe(AMOUNT);
      expect(Number(match!.diffAmount), 'diffAmount=0 for equal amounts').toBe(0);
    } finally {
      await cleanupScenario(page, ctx);
    }
  });

  test('(2) runMatching DIFF: unequal amounts (1000 vs 800) → status=DIFF + diffAmount=200', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinIntercompanyMatch-main');

    const pairKey = uniq('E2E-PAIR-DIFF');
    const SALE_AMT = 1000;
    const PURCHASE_AMT = 800;
    const ctx: ScenarioCtx = { pairKey };
    try {
      const period = await createTestPeriod(page, 'DIFF');
      ctx.periodId = period.periodId;
      ctx.periodCode = period.periodCode;

      ctx.saleVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_SALE, ORG_GROUP, period.periodId, SALE_AMT, pairKey, 'DIFF-SALE',
      );
      ctx.purchaseVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_PURCHASE, ORG_CO, period.periodId, PURCHASE_AMT, pairKey, 'DIFF-PUR',
      );

      const { count, errors } = await runMatchingRaw(page, period.periodId);
      expect(errors, `runMatching should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(count, 'runMatching should return positive count for diff pair').toBeGreaterThan(0);

      const match = await findIntercompanyMatchByPairKey<any>(
        page, pairKey, Number(period.periodId),
        'id pairKey periodId matchedAmount diffAmount status',
      );
      expect(match, 'ErpFinIntercompanyMatch record should exist for diff pair').toBeTruthy();
      expect(match!.status, 'diff pair status should be DIFF').toBe('DIFF');
      expect(Number(match!.matchedAmount), 'matchedAmount=min(1000,800)=800').toBe(PURCHASE_AMT);
      expect(Number(match!.diffAmount), 'diffAmount=|1000-800|=200').toBe(SALE_AMT - PURCHASE_AMT);
    } finally {
      await cleanupScenario(page, ctx);
    }
  });

  test('(3) checkDualSideConsistency: returns non-empty DualSideDiffReport with structured rows', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinIntercompanyMatch-main');

    const pairKey = uniq('E2E-PAIR-CONSIST');
    const AMOUNT = 500;
    const ctx: ScenarioCtx = { pairKey };
    try {
      const period = await createTestPeriod(page, 'CONSIST');
      ctx.periodId = period.periodId;
      ctx.periodCode = period.periodCode;

      // 单边 SALE 凭证（无 PURCHASE 对）：runMatching 仍写一条记录（matchedAmount=0,diffAmount=500,status=DIFF）
      ctx.saleVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_SALE, ORG_GROUP, period.periodId, AMOUNT, pairKey, 'CONSIST-SALE',
      );

      await runMatchingRaw(page, period.periodId);

      const { report, errors } = await checkDualSideConsistencyRaw(page, pairKey, period.periodId);
      expect(errors, `checkDualSideConsistency should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(report, 'DualSideDiffReport should be returned').toBeTruthy();
      expect(report.direction, 'report.direction echoes INTERCOMPANY marker').toBe('INTERCOMPANY');
      expect(typeof report.consistent, 'report.consistent is a boolean').toBe('boolean');
      expect(Array.isArray(report.rows), 'report.rows is an array').toBe(true);
      expect(report.rows.length, 'report.rows should contain at least one diff row').toBeGreaterThan(0);
      const row = report.rows[0];
      expect(row, 'diff row should have status field').toBeTruthy();
      expect(typeof row.status, 'diff row status is a string (CONSISTENT|INCONSISTENT)').toBe('string');
    } finally {
      await cleanupScenario(page, ctx);
    }
  });

  test('(4) generateEliminationCandidates: MATCHED pair → AR_AP + REVENUE_COST CANDIDATE rows', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinConsolidationElimination-main');

    const pairKey = uniq('E2E-PAIR-ELIM');
    const AMOUNT = 3000;
    const ctx: ScenarioCtx = { pairKey };
    try {
      const period = await createTestPeriod(page, 'ELIM');
      ctx.periodId = period.periodId;
      ctx.periodCode = period.periodCode;

      ctx.saleVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_SALE, ORG_GROUP, period.periodId, AMOUNT, pairKey, 'ELIM-SALE',
      );
      ctx.purchaseVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_PURCHASE, ORG_CO, period.periodId, AMOUNT, pairKey, 'ELIM-PUR',
      );

      // 先配对（generateEliminationCandidates 扫描 MATCHED 记录）
      await runMatchingRaw(page, period.periodId);

      const { count, errors } = await generateEliminationCandidatesRaw(page, period.periodId);
      expect(errors, `generateEliminationCandidates should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(count, 'generateEliminationCandidates should return positive count').toBeGreaterThan(0);

      const candidates = await findEliminationCandidates<any>(
        page, Number(period.periodId),
        'id code eliminationType periodId pairKey matchId eliminationAmount status',
      );
      ctx.candidateIds = candidates.map((c) => c.id);

      const arAp = candidates.filter((c) => c.eliminationType === 'AR_AP');
      const revenueCost = candidates.filter((c) => c.eliminationType === 'REVENUE_COST');
      expect(arAp.length, 'should produce at least 1 AR_AP CANDIDATE').toBeGreaterThanOrEqual(1);
      expect(revenueCost.length, 'should produce at least 1 REVENUE_COST CANDIDATE').toBeGreaterThanOrEqual(1);

      // 全部候选初始态为 CANDIDATE
      for (const c of candidates) {
        expect(c.status, `candidate ${c.code} status should be CANDIDATE`).toBe('CANDIDATE');
        expect(Number(c.eliminationAmount), `candidate ${c.code} eliminationAmount=${AMOUNT}`).toBe(AMOUNT);
      }
    } finally {
      await cleanupScenario(page, ctx);
    }
  });

  test('(5) postElimination: candidate → DRAFT voucher + status flips to DRAFT_VOUCHER', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinConsolidationElimination-main');

    const pairKey = uniq('E2E-PAIR-POST');
    const AMOUNT = 2000;
    const ctx: ScenarioCtx = { pairKey };
    try {
      const period = await createTestPeriod(page, 'POST');
      ctx.periodId = period.periodId;
      ctx.periodCode = period.periodCode;

      ctx.saleVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_SALE, ORG_GROUP, period.periodId, AMOUNT, pairKey, 'POST-SALE',
      );
      ctx.purchaseVoucher = await seedIntercompanyVoucher(
        page, INTERCOMPANY_PURCHASE, ORG_CO, period.periodId, AMOUNT, pairKey, 'POST-PUR',
      );

      await runMatchingRaw(page, period.periodId);
      await generateEliminationCandidatesRaw(page, period.periodId);

      const candidates = await findEliminationCandidates<any>(
        page, Number(period.periodId),
        'id code eliminationType status',
      );
      ctx.candidateIds = candidates.map((c) => c.id);
      expect(candidates.length, 'should have at least one candidate').toBeGreaterThanOrEqual(1);

      const candidate = candidates[0];
      const { voucherId, errors } = await postEliminationRaw(page, candidate.id);
      expect(errors, `postElimination should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
      expect(voucherId, 'postElimination should return non-null draft voucherId').not.toBeNull();

      // 候选状态翻转为 DRAFT_VOUCHER + draftVoucherId 回写（__get 权威查库）
      const updated = await verifyState(
        page, 'ErpFinConsolidationElimination', candidate.id,
        'id status draftVoucherId',
      );
      expect(updated?.status, 'candidate status should flip to DRAFT_VOUCHER').toBe('DRAFT_VOUCHER');
      expect(Number(updated?.draftVoucherId), 'draftVoucherId should be set').toBe(Number(voucherId));

      // DRAFT 抵消凭证：docStatus=DRAFT + voucherType=TRANSFER
      const draftVoucher = await verifyState(
        page, 'ErpFinVoucher', voucherId as number,
        'id code voucherType docStatus totalDebit totalCredit',
      );
      expect(draftVoucher, 'draft elimination voucher should exist').toBeTruthy();
      expect(draftVoucher.voucherType, 'elimination voucher voucherType=TRANSFER').toBe('TRANSFER');
      expect(draftVoucher.docStatus, 'elimination voucher docStatus=DRAFT').toBe('DRAFT');
      expect(Number(draftVoucher.totalDebit), `elimination voucher totalDebit=${AMOUNT}`).toBe(AMOUNT);
      expect(Number(draftVoucher.totalCredit), `elimination voucher totalCredit=${AMOUNT}`).toBe(AMOUNT);
    } finally {
      await cleanupScenario(page, ctx);
    }
  });
});
