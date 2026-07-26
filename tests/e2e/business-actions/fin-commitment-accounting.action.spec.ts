import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  findFirst,
  eqFilter,
  deleteById,
  deleteByFilter,
} from './_helper';
import {
  findCommitmentVoucherIdByCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  runP2pChain,
  cleanupP2p,
  runO2cChain,
  cleanupO2c,
  SEED,
} from '../orchestration/_helper';

/**
 * Finance 承付会计（Commitment Accounting）三接入点全栈浏览器层 E2E
 * （plan 2026-07-26-0410-2）。
 *
 * 验证 config-gated `erp-fin.budget-commitment-enabled=true` 启用后，COMMITMENT 影子凭证的
 * 生成（commit）与释放（release）经三接入点的全栈可达性（owner doc `docs/design/finance/budget.md`
 * §承付会计 §3 接入点 + §sales 承付扩展 + `docs/design/finance/posting.md` §承付 COMMITMENT 实际过账段）：
 *
 *   (1) **接入点 #1 订单审核 commit（order-only setup）** —— 采购 + 销售各 approve 订单 →
 *       `findCommitmentVoucherIdByCode(order.code, false)` 非空 + `assertVoucherLines` 单行
 *       （config 科目 + dcDirection=CREDIT + credit=order.totalAmountWithTax / debit=0）
 *   (2) **接入点 #2 订单反审核 release（order-only，发票前反转）** —— reverseApprove 订单 →
 *       原凭证 `isReversed=true`（`__get`）+ `findCommitmentVoucherIdByCode(order.code, true)` 非空 +
 *       红冲行 `dcDirection=CREDIT 不变 / debit-credit 互换`（debit=absAmount / credit=0）
 *   (3) **接入点 #3 发票审核 release（full-chain `runP2pChain`/`runO2cChain`）** —— 链路末端发票
 *       approve 触发 release → 承付原凭证 `isReversed=true` + 红冲凭证存在（经 orderCode 反查）
 *   (4) **采购 release hook 容错回归（full-chain + 订单反审核）** —— full-chain 后 reverseApprove
 *       采购订单不再抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（Phase 1 Fix 生效）
 *
 * **两组 setup 隔离接入点 #3 干扰**：
 *   - (A) order-only（建订单 + submit + approve，**不创建发票**）—— 隔离接入点 #3，专测 #1+#2
 *   - (B) full-chain `runP2pChain`/`runO2cChain` —— 链路末端发票 approve 触发 #3，专测 #3 + #4
 *
 * 权威实现：
 *   - `CommitmentVoucherGenerator.writeCommitmentVoucher:119-182`（单行凭证 + dcDirection 跟随科目 + CREDIT 方向 credit=absAmount）
 *   - `CommitmentVoucherGenerator.writeReversalFromLines:184,222-245`（红冲：dcDirection 不变 + debit↔credit 互换 + amountSource/Functional=origDebit+origCredit）
 *   - `ErpPurOrderProcessor.runCommitmentCommitHook:218-232` + `runCommitmentReleaseHook:239-256`（Phase 1 Fix 后 try-catch 容错）
 *   - `ErpSalOrderProcessor.runCommitmentCommitHook:338-352` + `runCommitmentReleaseHook:359-370`（销售侧容错范式参照）
 *   - `ErpPurInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:306-325`（接入点 #3 + try-catch 容错）
 *   - `ErpSalInvoiceProcessor.runCommitmentReleaseOnInvoiceApproveHook:347-365`（销售接入点 #3）
 *
 * 科目配置（webServer JVM args，playwright.config.ts）：
 *   - `-Derp-fin.budget-commitment-enabled=true`
 *   - `-Derp-fin.budget-commitment-subject-code=2202`（应付账款 LIABILITY CREDIT，id=5）
 *   - `-Derp-fin.budget-commitment-sales-subject-code=5001`（主营业务收入 INCOME CREDIT，id=6）
 *
 * 种子引用：org id=2 / supplier SUP-001 id=3 / customer CUST-001 id=1 / warehouse WH-RAW id=2 /
 *   MAT-001 id=1 / UOM PCS id=1 / currency CNY id=1 / businessDate 2026-07-09 落种子 OPEN 期间 2026-07。
 * 自包含隔离：order-only 用例独立建 PO/SO（code 唯一 E2E-CMT-*），cleanup 删凭证 + 订单行 + 订单。
 */

const ORG = SEED.ORG;
const SUPPLIER = SEED.SUPPLIER;
const CUSTOMER = SEED.CUSTOMER;
const WAREHOUSE = SEED.WH_RAW;
const MAT_1 = SEED.MAT_1;
const UOM = SEED.UOM;
const CURRENCY = SEED.CURRENCY;
const BDATE = '2026-07-09';

/** 采购承付科目（webServer JVM arg `budget-commitment-subject-code=2202`，种子 id=5 LIABILITY CREDIT）。 */
const PUR_COMMITMENT_SUBJECT_CODE = '2202';
/** 销售承付科目（webServer JVM arg `budget-commitment-sales-subject-code=5001`，种子 id=6 INCOME CREDIT）。 */
const SAL_COMMITMENT_SUBJECT_CODE = '5001';

/** order-only PO 期望值：10 × 5 = 50 含税（与 P2P 一致，totalAmountWithTax 必填使 commit hook amount 确定性）。 */
const PO_NET = 50;
const PO_WITH_TAX = 50;
/** order-only SO 期望值：10 × 10 = 100 含税。 */
const SO_NET = 100;
const SO_WITH_TAX = 100;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

// ---------- order-only setup（隔离接入点 #3） ----------

interface OrderOnlyCtx {
  orderEntity: string;
  lineEntity: string;
  orderId?: string | number;
  orderCode?: string;
  lineIds: Array<string | number>;
}

function newOrderCtx(entity: string, lineEntity: string): OrderOnlyCtx {
  return { orderEntity: entity, lineEntity, lineIds: [] };
}

async function setupOrderOnly(
  page: import('@playwright/test').Page,
  entity: string,
  lineEntity: string,
  partnerField: string,
  partnerId: number,
  totalWithTax: number,
  statusField: string,
): Promise<OrderOnlyCtx> {
  const ctx = newOrderCtx(entity, lineEntity);
  const code = uniq(`E2E-CMT-${entity}`);
  const order = await createViaSave(
    page,
    entity,
    {
      code, orgId: ORG, [partnerField]: partnerId, warehouseId: WAREHOUSE,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      totalAmount: totalWithTax, totalAmountWithTax: totalWithTax,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', [statusField]: statusField === 'receiveStatus' ? 'UNRECEIVED' : 'UNDELIVERED',
    },
    'id approveStatus',
  );
  ctx.orderId = order.id;
  ctx.orderCode = code;
  const line = await createViaSave(
    page,
    lineEntity,
    { orderId: order.id, lineNo: 1, materialId: MAT_1, uoMId: UOM, quantity: 10, unitPrice: totalWithTax / 10, amount: totalWithTax },
    'id',
  );
  ctx.lineIds.push(line.id);
  return ctx;
}

async function setupPurchaseOrderOnly(page: import('@playwright/test').Page): Promise<OrderOnlyCtx> {
  return setupOrderOnly(page, 'ErpPurOrder', 'ErpPurOrderLine', 'supplierId', SUPPLIER, PO_WITH_TAX, 'receiveStatus');
}

async function setupSalesOrderOnly(page: import('@playwright/test').Page): Promise<OrderOnlyCtx> {
  return setupOrderOnly(page, 'ErpSalOrder', 'ErpSalOrderLine', 'customerId', CUSTOMER, SO_WITH_TAX, 'deliveryStatus');
}

async function cleanupOrderOnly(page: import('@playwright/test').Page, ctx: OrderOnlyCtx): Promise<void> {
  if (!ctx) return;
  // 承付凭证 + 红冲凭证（共用 order.code 经 voucher_bill_r 反查，cleanupVoucherByBillCode 已 postingType-agnostic 覆盖）
  if (ctx.orderCode) {
    await cleanupVoucherByBillCode(page, ctx.orderCode);
  }
  for (const id of ctx.lineIds ?? []) {
    await deleteById(page, ctx.lineEntity, id);
  }
  if (ctx.orderId != null) {
    await deleteById(page, ctx.orderEntity, ctx.orderId);
  }
}

// ---------- spec ----------

test.describe('Finance commitment accounting (3 entry points) browser-layer E2E', () => {
  test('(1) entry-point #1 order approve commit: PO + SO approve → COMMITMENT voucher single CREDIT line', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    // ---- 采购订单 approve → COMMITMENT 凭证 ----
    const poCtx = await setupPurchaseOrderOnly(page);
    try {
      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: poCtx.orderId }, 'id');
      await callMutationOk(page, 'ErpPurOrder', 'approve', { id: poCtx.orderId }, 'id');

      const poCommitmentId = await findCommitmentVoucherIdByCode(page, poCtx.orderCode!, false);
      expect(poCommitmentId, 'PO approve should produce a forward COMMITMENT voucher').toBeTruthy();

      // 凭证行：单行 CREDIT 方向（科目 2202 应付账款）credit=含税 50 / debit=0
      await assertVoucherLines(page, poCommitmentId, [
        { subjectCode: PUR_COMMITMENT_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: PO_WITH_TAX },
      ]);
    } finally {
      await cleanupOrderOnly(page, poCtx);
    }

    // ---- 销售订单 approve → COMMITMENT 凭证 ----
    const soCtx = await setupSalesOrderOnly(page);
    try {
      await callMutationOk(page, 'ErpSalOrder', 'submitForApproval', { id: soCtx.orderId }, 'id');
      await callMutationOk(page, 'ErpSalOrder', 'approve', { id: soCtx.orderId }, 'id');

      const soCommitmentId = await findCommitmentVoucherIdByCode(page, soCtx.orderCode!, false);
      expect(soCommitmentId, 'SO approve should produce a forward COMMITMENT voucher').toBeTruthy();

      // 凭证行：单行 CREDIT 方向（科目 5001 主营业务收入）credit=含税 100 / debit=0
      await assertVoucherLines(page, soCommitmentId, [
        { subjectCode: SAL_COMMITMENT_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SO_WITH_TAX },
      ]);
    } finally {
      await cleanupOrderOnly(page, soCtx);
    }
  });

  test('(2) entry-point #2 order reverseApprove release: COMMITMENT voucher reversed + original isReversed=true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    // ---- 采购订单 approve → reverseApprove → 红冲凭证 ----
    const poCtx = await setupPurchaseOrderOnly(page);
    try {
      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: poCtx.orderId }, 'id');
      await callMutationOk(page, 'ErpPurOrder', 'approve', { id: poCtx.orderId }, 'id');

      const originalId = await findCommitmentVoucherIdByCode(page, poCtx.orderCode!, false);
      expect(originalId, 'pre: forward COMMITMENT voucher should exist').toBeTruthy();

      // reverseApprove → 接入点 #2 release → 红冲凭证
      await callMutationOk(page, 'ErpPurOrder', 'reverseApprove', { id: poCtx.orderId }, 'id');

      // 原凭证 isReversed=true
      const origVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalId), 'id postingType isReversed',
      );
      expect(origVoucher?.postingType, 'original commitment postingType=COMMITMENT').toBe('COMMITMENT');
      expect(origVoucher?.isReversed, 'original commitment isReversed=true after reverseApprove').toBe(true);

      // 红冲凭证存在
      const reversalId = await findCommitmentVoucherIdByCode(page, poCtx.orderCode!, true);
      expect(reversalId, 'reverseApprove should produce a reversal COMMITMENT voucher').toBeTruthy();

      // 红冲行：dcDirection 不变 CREDIT / debit↔credit 互换 → debit=50 / credit=0
      await assertVoucherLines(page, reversalId, [
        { subjectCode: PUR_COMMITMENT_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: PO_WITH_TAX, creditAmount: 0 },
      ]);
    } finally {
      await cleanupOrderOnly(page, poCtx);
    }
  });

  test('(3) entry-point #3 invoice approve release (full-chain): P2P + O2C chain → commitment reversed by invoice approve', async ({ page }) => {
    test.setTimeout(120000);
    await loginAndNavigate(page, '/ErpPurOrder-main');

    // ---- P2P full-chain：末端发票 approve 触发接入点 #3 release ----
    const p2p = await runP2pChain(page);
    try {
      // 链路返回时 PO.approve 已产 COMMITMENT 凭证，invoice.approve 已 release
      const poOriginalId = await findCommitmentVoucherIdByCode(page, p2p.codes.po, false);
      expect(poOriginalId, 'P2P chain: PO approve should produce forward COMMITMENT voucher').toBeTruthy();

      // 接入点 #3：invoice approve 后原凭证 isReversed=true
      const poOrigVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', poOriginalId), 'id postingType isReversed',
      );
      expect(poOrigVoucher?.isReversed, 'P2P chain: invoice approve should reverse the commitment (isReversed=true)').toBe(true);

      // 红冲凭证存在（经 PO code 反查）
      const poReversalId = await findCommitmentVoucherIdByCode(page, p2p.codes.po, true);
      expect(poReversalId, 'P2P chain: invoice approve should produce a reversal COMMITMENT voucher').toBeTruthy();
    } finally {
      await cleanupP2p(page, p2p);
    }

    // ---- O2C full-chain：末端发票 approve 触发接入点 #3 release ----
    const o2c = await runO2cChain(page);
    try {
      const soOriginalId = await findCommitmentVoucherIdByCode(page, o2c.codes.so, false);
      expect(soOriginalId, 'O2C chain: SO approve should produce forward COMMITMENT voucher').toBeTruthy();

      const soOrigVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', soOriginalId), 'id postingType isReversed',
      );
      expect(soOrigVoucher?.isReversed, 'O2C chain: invoice approve should reverse the commitment (isReversed=true)').toBe(true);

      const soReversalId = await findCommitmentVoucherIdByCode(page, o2c.codes.so, true);
      expect(soReversalId, 'O2C chain: invoice approve should produce a reversal COMMITMENT voucher').toBeTruthy();
    } finally {
      await cleanupO2c(page, o2c);
    }
  });

  test('(4) purchase release hook tolerance regression: full-chain + PO reverseApprove → no ERR_BUDGET_COMMITMENT_ALREADY_RELEASED', async ({ page }) => {
    test.setTimeout(120000);
    await loginAndNavigate(page, '/ErpPurOrder-main');

    // full-chain：末端发票 approve 已 release 承付（接入点 #3）
    const p2p = await runP2pChain(page);
    try {
      const poOriginalId = await findCommitmentVoucherIdByCode(page, p2p.codes.po, false);
      expect(poOriginalId, 'pre: forward COMMITMENT voucher should exist').toBeTruthy();
      const origVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', poOriginalId), 'isReversed',
      );
      expect(origVoucher?.isReversed, 'pre: commitment already reversed by invoice approve (entry-point #3)').toBe(true);

      // 承付已被 #3 释放 → 此时 PO reverseApprove 调 release() 会抛 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED
      // Phase 1 Fix：catch NopException 后 LOG.debug 跳过，不阻断 reverseApprove
      const result = await callMutation(page, 'ErpPurOrder', 'reverseApprove', { id: p2p.po.id }, 'id approveStatus');
      expect(result.errors, 'PO reverseApprove should NOT fail after commitment already released by invoice approve (Phase 1 Fix)').toBeNull();
      expect(result.data, 'PO reverseApprove should succeed').toBeTruthy();

      // 状态回退确证（reverseApprove 成功执行）
      const after = await verifyState(page, 'ErpPurOrder', p2p.po.id, 'approveStatus');
      expect(after.approveStatus, 'PO approveStatus=REJECTED after reverseApprove').toBe('REJECTED');
    } finally {
      await cleanupP2p(page, p2p);
    }
  });
});

// 显式标注种子引用（供 lint/可读性；SEED 复用避免硬编码漂移）
void SEED;
