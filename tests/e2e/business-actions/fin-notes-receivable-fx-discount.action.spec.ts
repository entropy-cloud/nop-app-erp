import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  deleteById,
} from './_helper';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * finance ErpFinNotesReceivable 多币种票据贴现浏览器层 E2E
 * （plan 2026-07-19-0120-1 浏览器层覆盖 + plan 2026-07-19-0730-1 真实 mutation 路径迁移）。
 *
 * 验证三层（全部经 `ErpFinNotesReceivable__discount` 真实 mutation 路径）：
 *
 * (1) **FX 状态机生命周期 + 6051 抑制（exchangeRate=null 兜底）**：建 USD 票据
 *     （currencyId=2 + exchangeRate=6.6667）→ discount（exchangeRate 省略，走 ZERO 兜底）→
 *     DISCOUNTED + posted=true + 3 行凭证（Dr 1002 netAmount / Dr 6603 interest / Cr 1121 face，
 *     全部 functional 口径）。证明旧 4 参数 GraphQL 调用经新 5 参数方法（exchangeRate=null）透明兼容。
 *
 * (2) **FX 6051 触发分支（真实 mutation，cash-at-spot plug 范式，外币升值场景）**：
 *     `discount(exchangeRate=6.7000)` 真实 mutation 触发 Builder 派生 exchangeGainLoss=-3.3000 →
 *     4 行凭证（Dr 1002=663.3000 netAmount cash-at-spot / Dr 6603=6.67 interest functional /
 *     Cr 6051=3.3000 汇兑收益 / Cr 1121=666.67 face functional）。
 *     **复式平衡：Dr 663.3000 + Dr 6.67 = 669.97 ≡ Cr 3.3000 + Cr 666.67 = 669.97**。
 *
 * (3) **对照：CNY 单币种 6051 抑制（真实 mutation）**：CNY 票据 `discount(...)` →
 *     Builder 判定 isForeignCurrency=false → 走 ZERO 路径 → 3 行凭证（无 6051）。
 *     与 (2) 形成对照（FX 触发 6051 vs CNY 抑制 6051）。
 *
 * ## 后端路径（实时仓库核实 plan 2026-07-19-0730-1 Phase 2）
 *
 * - `ErpFinNotesReceivableBizModel.discount:43-49`（module-finance/erp-fin-service）：
 *     单 5 参数方法（@BizMutation）委派 Processor；GraphQL 层 exchangeRate 为可选 Input 字段。
 * - `ErpFinNotesReceivableProcessor.buildDiscount`（cash-at-spot plug 范式）：
 *     - discountInterestFunctional = amountFunctional × rate × days / 360（HALF_UP scale=2）
 *     - discountInterestSource = amountSource × rate × days / 360（source 口径中间量）
 *     - netAmountSource = amountSource − discountInterestSource
 *     - netAmount = netAmountSource × exchangeRate（cash-at-spot，HALF_UP scale=4）
 *     - exchangeGainLoss = amountFunctional − discountInterestFunctional − netAmount（plug）
 *     - 三联条件（config enabled + 外币票据 + exchangeRate≠null）触发派生；否则走 ZERO 兜底。
 * - `NotesReceivableAcctDocProvider.createFacts:64-80`：DISCOUNTED 路径四件套
 *     - fx>0 → Dr 6051；fx<0 → Cr 6051=-fx；fx==0 → 抑制 6051 行（3 行凭证）。
 * - config `erp-fin.notes-fx-gain-loss-enabled=true` 由 playwright.config.ts webServer JVM args 启用。
 *
 * ## 数值表（全部以 functional CNY 计）
 *
 * - FX（升值场景，cash-at-spot plug 范式，functional CNY）：
 *     - amountSource = USD 100 / note.exchangeRate = 6.6667 / amountFunctional = CNY 666.67
 *     - discountRate = 0.12 / remainingDays = 30（dueDate 2026-07-31 − discountDate 2026-07-01）
 *     - discountInterestFunctional = 666.67 × 0.12 × 30 / 360 = 6.67（Dr 6603，functional 口径不动）
 *     - discountInterestSource = 100 × 0.12 × 30 / 360 = 1.00 USD
 *     - netAmountSource = 100 − 1 = 99 USD
 *     - netAmount = 99 × 6.7000 = 663.3000（Dr 1002，cash-at-spot 按贴现日即期汇率折算）
 *     - exchangeGainLoss = 666.67 − 6.67 − 663.3000 = −3.3000（负数 → Cr 6051 汇兑收益）
 * - FX（exchangeRate 省略兜底路径，functional 口径）：
 *     - netAmount = 666.67 − 6.67 = 660.00 / exchangeGainLoss = 0 → 3 行凭证
 * - 对照 CNY（functional CNY）：face 1000 / interest 10 / net 990 / fx 0。
 *
 * ## 自包含 setup + 清理
 *
 * - 自包含建 USD/CNY ErpFinNotesReceivable（status=RECEIVED 直置入口，对齐 1430-1 范式）
 *   + ErpFinFundAccount(BANK, currencyId=2)（discount bankId FK）。
 * - 清理：voucher 经 `cleanupVoucherByBillCode` 删凭证行+凭证+回链；note/discount/fundAccount 经
 *   `deleteById` 删主实体（post 仅写 voucher/voucher_line/voucher_bill_r，不写 gl_balance，
 *   finance 看板基线不受影响）。
 *
 * ## 迁移注记（plan 2026-07-19-0730-1 Fix 4/5 + Add 5）
 *
 * - 测试 (2)/(3) 原经 `ErpFinVoucher__post` 直驱 PostingEvent 验证 Provider FX 6051 分支（0120-1 期
 *   Java builder 缺陷 workaround，`ErpFinNotesReceivableProcessor.buildDiscount:249` 硬编码 ZERO）。
 *   本计划 Phase 2 修复后改为真实 `discount(exchangeRate)` mutation 路径，验证 Builder → Dispatcher →
 *   Provider 全链 FX 6051 产生；直驱原语 `postVoucher` / `buildNotesReceivableDiscountedEvent` 已删除。
 */

const ORG_ID = 2;
const CURRENCY_CNY = 1;
const CURRENCY_USD = 2;
const PARTNER_ID = 1; // CUST-001
const BDATE = '2026-07-01';
const DUE_DATE = '2026-07-31';
const DISCOUNT_DATE = '2026-07-01'; // remainingDays = 30

// FX 数值（functional CNY 计入凭证）
const EXCHANGE_RATE_NOTE_USD_CNY = 6.6667; // note.exchangeRate（functional/source）
const SPOT_RATE_USD_CNY = 6.7000; // 贴现日即期汇率（外币升值场景）
const FACE_AMOUNT_SOURCE_USD = 100; // USD 票面（amountSource）
const FACE_AMOUNT_FUNCTIONAL_CNY = 666.67; // CNY 本位币票面（amountFunctional）
const DISCOUNT_RATE = 0.12;
const DISCOUNT_INTEREST_FX = 6.67; // 666.67 × 0.12 × 30 / 360 = 6.6667 → 6.67（HALF_UP scale 2）
// cash-at-spot plug 范式派生（exchangeRate=spotRate=6.7000）
const NET_AMOUNT_FX_CASH_AT_SPOT = 663.3; // 99 USD × 6.7000（Dr 1002 cash-at-spot）
const EXCHANGE_GAIN_LOSS_FX = -3.3; // 666.67 − 6.67 − 663.3000（负数 → Cr 6051 汇兑收益）
const EXCHANGE_GAIN_LOSS_FX_CREDIT = 3.3; // Cr 6051 = -exchangeGainLoss
// 兜底路径（exchangeRate=null 或省略）
const NET_AMOUNT_FX_FALLBACK = 660.0; // 666.67 − 6.67（functional 口径兜底）

// 对照 CNY 数值（functional CNY）
const FACE_AMOUNT_CNY = 1000;
const DISCOUNT_INTEREST_CNY = 10; // 1000 × 0.12 × 30 / 360
const NET_AMOUNT_CNY = 990; // 1000 − 10

let _seq = 0;
// 紧凑唯一码（对齐 1430-1 紧凑 base36 范式：ErpFinArApItemGenerator.buildCode 拼为 AR/AP 辅助账
// code 超 voucherCode precision 50 时溢出，紧凑码绕过；详见 docs/bugs/2026-07-17-1430-...）。
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now().toString(36)}${_seq}`;
}

async function createNote(
  page: Page,
  overrides: Record<string, unknown> = {},
): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpFinNotesReceivable',
    {
      code: uniq('NR-FX'),
      orgId: ORG_ID,
      notesType: 'BANK_ACCEPTANCE',
      notesNo: uniq('NN-FX'),
      currencyId: CURRENCY_USD,
      exchangeRate: EXCHANGE_RATE_NOTE_USD_CNY,
      amountSource: FACE_AMOUNT_SOURCE_USD,
      amountFunctional: FACE_AMOUNT_FUNCTIONAL_CNY,
      partnerId: PARTNER_ID,
      issueDate: BDATE,
      dueDate: DUE_DATE,
      ...overrides,
    },
    'id code',
  );
}

async function createFundAccount(
  page: Page,
  currencyId: number = CURRENCY_USD,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinFundAccount',
    {
      code: uniq('FA-FX'),
      name: 'E2E FX Discount Bank Account',
      orgId: ORG_ID,
      accountType: 'BANK',
      subjectId: 2, // 1002 银行存款（种子 id=2）
      currencyId,
      currentBalance: 0,
      status: 'ACTIVE',
    },
    'id',
  );
}

async function cleanupNote(
  page: Page,
  note: { id?: string; code?: string } | null,
  opts: { discountId?: string | number | null; fundAccountId?: string | number | null } = {},
): Promise<void> {
  if (!note) return;
  if (note.code) await cleanupVoucherByBillCode(page, note.code);
  if (opts.discountId != null) await deleteById(page, 'ErpFinNotesDiscount', opts.discountId);
  if (note.id != null) await deleteById(page, 'ErpFinNotesReceivable', note.id);
  if (opts.fundAccountId != null) await deleteById(page, 'ErpFinFundAccount', opts.fundAccountId);
}

test.describe('finance ErpFinNotesReceivable FX discount — multi-currency voucher lines E2E', () => {
  test('FX lifecycle via discount mutation (exchangeRate omitted): USD note → DISCOUNTED + 3-line voucher (6051 suppressed by ZERO fallback)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const fund = await createFundAccount(page);
    const note = await createNote(page, { status: 'RECEIVED' });
    let discountId: string | number | null = null;
    try {
      // exchangeRate 省略 → 走 ZERO 兜底路径（向后兼容，对齐 1430-1 单币种路径行为）
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'discount',
        { notesId: note.id, discountDate: DISCOUNT_DATE, bankId: fund.id, discountRate: DISCOUNT_RATE },
        'id status posted discountId',
      );
      expect(r.status, 'FX discount → DISCOUNTED').toBe('DISCOUNTED');
      expect(r.posted, 'FX discount → posted=true').toBe(true);
      expect(r.discountId, 'FX discount should set discountId').toBeTruthy();
      discountId = r.discountId;

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(v.status, '__get confirms DISCOUNTED').toBe('DISCOUNTED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      // 凭证行断言：3 行（exchangeRate 省略 → Builder ZERO 兜底 → Provider fx.signum=0 抑制 6051）
      // 金额全部 functional CNY（dispatcher :84 billData.FACE_AMOUNT=note.amountFunctional=666.67）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_DISCOUNTED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_FX_FALLBACK, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_FX, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note, { discountId, fundAccountId: fund.id });
    }
  });

  test('FX discount(exchangeRate=spotRate) via real mutation: 4 lines with Cr 6051 (cash-at-spot plug, currency appreciation)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const fund = await createFundAccount(page);
    const note = await createNote(page, { status: 'RECEIVED' });
    let discountId: string | number | null = null;
    try {
      // exchangeRate=spotRate=6.7000（外币升值场景）→ Builder cash-at-spot plug 派生 exchangeGainLoss=-3.3000
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'discount',
        { notesId: note.id, discountDate: DISCOUNT_DATE, bankId: fund.id, discountRate: DISCOUNT_RATE, exchangeRate: SPOT_RATE_USD_CNY },
        'id status posted discountId',
      );
      expect(r.status, 'FX discount(exchangeRate) → DISCOUNTED').toBe('DISCOUNTED');
      expect(r.posted, 'FX discount(exchangeRate) → posted=true').toBe(true);
      expect(r.discountId, 'FX discount should set discountId').toBeTruthy();
      discountId = r.discountId;

      // FX 4 行凭证断言（cash-at-spot plug 范式，外币升值）：
      //   Dr 1002=663.3000 netAmount cash-at-spot / Dr 6603=6.67 interest functional
      //   Cr 6051=3.3000 汇兑收益（exchangeGainLoss=-3.3000 取负 Cr 方向）
      //   Cr 1121=666.67 faceAmount functional
      // 复式平衡：Dr 663.3000 + Dr 6.67 = 669.97 ≡ Cr 3.3000 + Cr 666.67 = 669.97
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_DISCOUNTED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_FX_CASH_AT_SPOT, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_FX, creditAmount: 0 },
        { subjectCode: '6051', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: EXCHANGE_GAIN_LOSS_FX_CREDIT },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note, { discountId, fundAccountId: fund.id });
    }
  });

  test('control: single-currency discount mutation with 6051 suppressed (CNY note → 3-line voucher)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const fund = await createFundAccount(page, CURRENCY_CNY);
    const note = await createNote(page, {
      status: 'RECEIVED',
      currencyId: CURRENCY_CNY,
      exchangeRate: 1,
      amountSource: FACE_AMOUNT_CNY,
      amountFunctional: FACE_AMOUNT_CNY,
    });
    let discountId: string | number | null = null;
    try {
      // CNY 票据：Builder isForeignCurrency=false → 走 ZERO 路径（即使 config 启用）
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'discount',
        { notesId: note.id, discountDate: DISCOUNT_DATE, bankId: fund.id, discountRate: DISCOUNT_RATE },
        'id status posted discountId',
      );
      expect(r.status, 'CNY discount → DISCOUNTED').toBe('DISCOUNTED');
      expect(r.posted, 'CNY discount → posted=true').toBe(true);
      expect(r.discountId, 'CNY discount should set discountId').toBeTruthy();
      discountId = r.discountId;

      // 单币种 3 行凭证断言：isForeignCurrency=false → fxPlugEnabled=false → exchangeGainLoss=0 → 抑制 6051 行
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_DISCOUNTED NORMAL voucher exists for CNY note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);
    } finally {
      await cleanupNote(page, note, { discountId, fundAccountId: fund.id });
    }
  });
});
