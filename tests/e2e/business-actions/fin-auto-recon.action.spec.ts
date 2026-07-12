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

/**
 * Finance ErpFinReconciliation runAutoReconciliation 三策略浏览器层 E2E（plan 2026-07-12-1321-2 Phase 2）。
 *
 * 验证 finance 域自动核销 DIRECT `@BizMutation` `runAutoReconciliation(direction, partnerId, strategy)`
 * 经 GraphQL /graphql 的全栈可达性 + 三匹配策略（FIFO/BY_AMOUNT/BY_RATIO）正路径：
 *   自包含建 partner + OPEN AR 对（AR_INVOICE + RECEIPT，同 partner+direction+金额）→
 *   `runAutoReconciliation(direction:"RECEIVABLE", partnerId, strategy)` →
 *   断言核销单自动创建 + posted（经 result.reconciliationIds）+ 双方辅助账 openAmount→0/status=SETTLED。
 *
 * 权威设计（docs/design/finance/ar-ap-reconciliation.md §自动核销引擎）：
 *   - config 门控 `erp-fin.auto-reconcile`（默认 false，ErpFinConstants.CONFIG_AUTO_RECONCILE）：
 *     关闭时抛 ERR_AUTO_RECON_DISABLED。webServer JVM arg `-Derp-fin.auto-reconcile=true` 启用。
 *   - 引擎按 partner + direction 查 OPEN 发票项（AR_INVOICE/AP_INVOICE）与收付款项（RECEIPT/PAYMENT），
 *     按策略生成 ReconciliationLineInput 候选行 → 内部 create+post。
 *   - FIFO：按到期日/业务日期升序逐笔核销；BY_AMOUNT：精确金额 1:1 匹配；BY_RATIO：按余额比例分摊。
 *
 * 自包含隔离（复用 0204-2 partner 隔离范式）：新建 partner `E2E-AUTORECON-PN-` + OPEN RECEIVABLE
 * ErpFinArApItem 对（AR_INVOICE + RECEIPT，同 partner+金额），避开种子 ar_ap_item（1-4 SETTLED，
 * 5/6 非发票-收付款对）。cleanup 删核销单+行+辅助账+partner，使 finance 看板/ar-ap-aging 基线无漂移。
 *
 * 种子基线无副作用（0204-2/0413-2 Deferred 触发条件裁决）：partnerId 非 null 限定该 partner；
 * 种子 1-4 全 SETTLED（assertOpen 守卫阻止再核销），5/6 非发票-收付款对不可匹配——故对种子基线无副作用。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period id=1（OPEN）。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
const AMOUNT = 100;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createPartner(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpMdPartner',
    {
      code: uniq('E2E-AUTORECON-PN'),
      name: `E2E AutoRecon Partner ${tag}`,
      partnerType: 'CUSTOMER',
      status: 'ACTIVE',
    },
    'id',
  );
}

async function createOpenItem(
  page: import('@playwright/test').Page,
  partnerId: string | number,
  sourceBillType: string,
  tag: string,
  amount = AMOUNT,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinArApItem',
    {
      code: uniq(`E2E-AUTORECON-${tag}`),
      orgId: ORG,
      acctSchemaId: ACCT_SCHEMA,
      direction: 'RECEIVABLE',
      partnerId,
      sourceBillType,
      sourceBillCode: uniq(`E2E-AUTORECON-BILL-${tag}`),
      businessDate: '2026-07-05',
      currencyId: CURRENCY,
      exchangeRate: 1,
      amountSource: amount,
      amountFunctional: amount,
      settledAmountSource: 0,
      settledAmountFunctional: 0,
      openAmountSource: amount,
      openAmountFunctional: amount,
      status: 'OPEN',
      periodId: PERIOD,
    },
    'id',
  );
}

interface CleanupCtx {
  partnerId?: string | number | null;
  itemIds: Array<string | number>;
  reconciliationIds: Array<string | number>;
}

async function cleanupAutoRecon(page: import('@playwright/test').Page, ctx: CleanupCtx): Promise<void> {
  for (const reconId of ctx.reconciliationIds) {
    await deleteByFilter(page, 'ErpFinReconciliationLine', eqFilter('reconciliationId', Number(reconId)));
    await deleteById(page, 'ErpFinReconciliation', reconId);
  }
  for (const id of ctx.itemIds) {
    await deleteById(page, 'ErpFinArApItem', id);
  }
  if (ctx.partnerId != null) {
    await deleteById(page, 'ErpMdPartner', ctx.partnerId);
  }
}

/**
 * 三策略共用编排：建 partner + OPEN AR 对 → runAutoReconciliation → 断言核销单创建 + 双方 SETTLED。
 * strategy ∈ {FIFO, BY_AMOUNT, BY_RATIO}。
 */
async function runAutoReconStrategy(
  page: import('@playwright/test').Page,
  strategy: string,
): Promise<void> {
  const partner = await createPartner(page, strategy);
  const invoice = await createOpenItem(page, partner.id, 'AR_INVOICE', `INV-${strategy}`);
  const receipt = await createOpenItem(page, partner.id, 'RECEIPT', `REC-${strategy}`);
  const ctx: CleanupCtx = {
    partnerId: partner.id,
    itemIds: [invoice.id, receipt.id],
    reconciliationIds: [],
  };

  try {
    // runAutoReconciliation：config 门控已启用（-Derp-fin.auto-reconcile=true），引擎匹配 OPEN 对 → create+post
    const result = await callMutationOk(
      page, 'ErpFinReconciliation', 'runAutoReconciliation',
      { direction: 'RECEIVABLE', partnerId: Number(partner.id), strategy },
      'reconciliationIds unmatched { arApItemId unmatchedReason }',
    );

    // 核销单自动创建（1:1 匹配 → 1 张核销单）
    expect(result.reconciliationIds, `${strategy}: should produce at least 1 reconciliation id`).toBeDefined();
    expect(result.reconciliationIds.length, `${strategy}: should auto-create 1 reconciliation`).toBeGreaterThanOrEqual(1);
    ctx.reconciliationIds = result.reconciliationIds.map((id: any) => Number(id));

    // 双方辅助账 openAmount→0 / status=SETTLED（经 __get 权威查库）
    const invAfter = await verifyState(page, 'ErpFinArApItem', invoice.id, 'openAmountFunctional status');
    expect(Number(invAfter.openAmountFunctional), `${strategy}: invoice openAmount should be 0 after auto-recon`).toBe(0);
    expect(invAfter.status, `${strategy}: invoice status should be SETTLED`).toBe('SETTLED');

    const recAfter = await verifyState(page, 'ErpFinArApItem', receipt.id, 'openAmountFunctional status');
    expect(Number(recAfter.openAmountFunctional), `${strategy}: receipt openAmount should be 0 after auto-recon`).toBe(0);
    expect(recAfter.status, `${strategy}: receipt status should be SETTLED`).toBe('SETTLED');

    // 核销单本身为 POSTED 态（create+post 内部编排）
    const reconFinal = await verifyState(page, 'ErpFinReconciliation', ctx.reconciliationIds[0], 'docStatus');
    expect(reconFinal.docStatus, `${strategy}: auto-created reconciliation should be POSTED`).toBe('POSTED');
  } finally {
    await cleanupAutoRecon(page, ctx);
  }
}

test.describe('Finance ErpFinReconciliation runAutoReconciliation three-strategy browser-layer E2E', () => {
  test('FIFO strategy: auto-match OPEN AR pair → reconciliation auto-created + posted + both items SETTLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');
    await runAutoReconStrategy(page, 'FIFO');
  });

  test('BY_AMOUNT strategy: exact-amount 1:1 match → reconciliation auto-created + posted + both items SETTLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');
    await runAutoReconStrategy(page, 'BY_AMOUNT');
  });

  test('BY_RATIO strategy: proportional allocation → reconciliation auto-created + posted + both items SETTLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinReconciliation-main');
    await runAutoReconStrategy(page, 'BY_RATIO');
  });
});
