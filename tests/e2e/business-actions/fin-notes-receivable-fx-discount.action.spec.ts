import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  deleteById,
  GraphQLClient,
} from './_helper';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * finance ErpFinNotesReceivable 多币种票据贴现浏览器层 E2E
 * （plan 2026-07-19-0120-1，承接 1430-1 Deferred「多币种票据贴现浏览器层 E2E」successor）。
 *
 * 验证三层：
 * (1) **FX 状态机生命周期**（`ErpFinNotesReceivable__discount` @BizMutation）：建 USD 票据
 *     （currencyId=2 + exchangeRate=6.6667）→ discount → DISCOUNTED + posted=true + 3 行凭证
 *     （Dr 1002 netAmount / Dr 6603 interest / Cr 1121 face，全部本位币计）。证明 FX 状态机
 *     正路径经 discount mutation 可达，凭证行以 functional 金额为单位（dispatcher.buildReceivableEvent
 *     :78 透传 note.exchangeRate，billData.FACE_AMOUNT=note.amountFunctional）。
 *
 * (2) **FX 6051 触发分支**（`ErpFinVoucher__post` 直驱 PostingEvent，billData.EXCHANGE_GAIN_LOSS
 *     ≠0）：构造 NOTES_RECEIVABLE_DISCOUNTED event 经 IErpFinVoucherBiz.post 入口直接过账 → 4 行凭证
 *     （Dr 1002 / Dr 6603 / Dr 6051 fx signum>0 / Cr 1121）。**这是验证 Provider FX 6051 分支的
 *     唯一浏览器层路径**——`ErpFinNotesReceivableProcessor.buildDiscount:249` 无条件
 *     `setExchangeGainLoss(BigDecimal.ZERO)`，经 `discount` mutation 永远不会触发 6051 行，
 *     故 6051 分支须经 `ErpFinVoucher__post` 直驱独立验证（对齐 `finance-voucher-post.action.spec.ts`
 *     范式 + plan Phase 1 Decision (a)）。
 *
 * (3) **对照：单币种 6051 抑制**（`ErpFinVoucher__post` 直驱 billData.EXCHANGE_GAIN_LOSS=0）：
 *     与 (2) 同一 posting 入口、唯一变量 EXCHANGE_GAIN_LOSS（5.00 vs 0），形成 crisp 对照——
 *     signum=0 时 Provider `:72` 抑制 6051 行，3 行凭证（Dr 1002 / Dr 6603 / Cr 1121）。
 *
 * ## 后端路径（实时仓库核实 plan Phase 1 Proof）
 *
 * - `NotesPostingDispatcher.buildReceivableEvent:71-98`（module-finance/erp-fin-service）：
 *     - `:78` event.setExchangeRate(note.getExchangeRate() != null ? ... : BigDecimal.ONE)
 *     - `:84` billData.FACE_AMOUNT = note.getAmountFunctional()  ← 凭证行金额全部 functional
 *     - `:90-92` DISCOUNTED 分支透传 discountInterest/netAmount/exchangeGainLoss
 * - `NotesReceivableAcctDocProvider.createFacts:64-80`：DISCOUNTED 路径四件套
 *     - `:66-69` face/discountInterest/netAmount/fx 四字段从 billData 读取
 *     - `:70` Dr 1002 bank = netAmount
 *     - `:71` Dr 6603 interest = discountInterest
 *     - `:72-78` if (fx.signum() != 0) { fx>0 Dr 6051=fx / fx<0 Cr 6051=-fx } else 抑制
 *     - `:79` Cr 1121 notes_recv = face
 * - `ErpFinNotesReceivableProcessor.buildDiscount:226-251`：`discountInterest = face × rate × days / 360`
 *     （HALF_UP scale 2），`:249` `discount.setExchangeGainLoss(BigDecimal.ZERO)` **无条件硬编码**
 *     （iter-2 MAJOR-1 核实）—— Java builder 缺陷，FX exchangeGainLoss 派生归 `Deferred But
 *     Adjudicated` 显式 successor（不改生产代码即时修，plan 规则 13/14）。
 *
 * ## 数值表（全部以 functional CNY 计，确定性派生 + 显式占位）
 *
 * - 汇率方向：`exchangeRate = functional / source`（经 `ExchangeRevaluationService.java:55`
 *   `diff = openAmountFunctional − (openAmountSource × 期末汇率)` 实测确认）。
 * - FX 数值（functional CNY）：
 *     - amountSource = USD 100
 *     - exchangeRate = 6.6667
 *     - amountFunctional = 100 × 6.6667 = 666.67（HALF_UP scale 4 → 666.6700，spec 直接置 666.67）
 *     - discountRate = 0.12 / remainingDays = 30（dueDate 2026-07-31 − discountDate 2026-07-01）
 *     - discountInterest = 666.67 × 0.12 × 30 / 360 = 6.67（HALF_UP scale 2）
 *     - netAmount = 666.67 − 6.67 = 660.00
 *     - exchangeGainLoss = 5.00（确定性占位非派生，spec 内验证 Provider signum≠0 分支与数值透传，
 *       非重估公式派生；外币重估公式属期末结账路径，见 Non-Goals）
 * - 对照 CNY 数值（functional CNY）：face 1000 / interest 10 / net 990 / fx 0。
 *
 * ## 自包含 setup + 清理
 *
 * - 自包含建 USD ErpFinNotesReceivable（status=RECEIVED 直置入口，对齐 1430-1 范式）
 *   + ErpFinFundAccount(BANK, currencyId=2)（discount bankId FK）。
 * - `ErpFinVoucher__post` 直驱路径无源单据依赖（billHeadCode 唯一字符串幂等键），无须前置 note。
 * - 清理：voucher 经 `cleanupVoucherByBillCode` 删凭证行+凭证+回链；note/discount/fundAccount 经
 *   `deleteById` 删主实体（post 仅写 voucher/voucher_line/voucher_bill_r，不写 gl_balance，
 *   finance 看板基线不受影响）。
 */

const ORG_ID = 2;
const ACCT_SCHEMA_ID = 1;
const CURRENCY_CNY = 1;
const CURRENCY_USD = 2;
const PARTNER_ID = 1; // CUST-001
const BDATE = '2026-07-01';
const DUE_DATE = '2026-07-31';
const DISCOUNT_DATE = '2026-07-01'; // remainingDays = 30

// FX 数值（functional CNY 计入凭证）
const EXCHANGE_RATE_USD_CNY = 6.6667; // functional/source
const FACE_AMOUNT_SOURCE_USD = 100; // USD 票面（amountSource）
const FACE_AMOUNT_FUNCTIONAL_CNY = 666.67; // CNY 本位币票面（amountFunctional）
const DISCOUNT_RATE = 0.12;
const DISCOUNT_INTEREST_FX = 6.67; // 666.67 × 0.12 × 30 / 360 = 6.6667 → 6.67（HALF_UP scale 2）
const NET_AMOUNT_FX = 660.00; // 666.67 − 6.67 — discount mutation builder 派生（无 FX）
const EXCHANGE_GAIN_LOSS_FX = 5.00; // 确定性占位非派生（FX 6051 plug）
// FX voucher via ErpFinVoucher__post 经非零 exchangeGainLoss 时须满足复式记账平衡：
//   Dr 1002 (netAmount) + Dr 6603 (interest) + Dr 6051 (fx, signum>0) = Cr 1121 (face)
//   → netAmountFxVoucher = face − interest − fx = 666.67 − 6.67 − 5.00 = 655.00
// 语义：贴现日 spot rate 较入账日 book rate 减弱（USD 走弱），银行实付 CNY 现金 655.00 少于
// 入账预期 660.00，差额 5.00 经 6051 Dr plug 平衡（book value 666.67 vs realized 661.67）。
const NET_AMOUNT_FX_VOUCHER = 655.00;

// 对照 CNY 数值（functional CNY）
const FACE_AMOUNT_CNY = 1000;
const DISCOUNT_INTEREST_CNY = 10; // 1000 × 0.12 × 30 / 360
const NET_AMOUNT_CNY = 990; // 1000 − 10

const POSTING_EVENT_INPUT = 'PostingEventInput';

let _seq = 0;
// 紧凑唯一码（对齐 1430-1 紧凑 base36 范式：ErpFinArApItemGenerator.buildCode 拼为 AR/AP 辅助账
// code 超 voucherCode precision 50 时溢出，紧凑码绕过；详见 docs/bugs/2026-07-17-1430-...）。
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now().toString(36)}${_seq}`;
}

async function createFxNote(
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
      exchangeRate: EXCHANGE_RATE_USD_CNY,
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

async function createFxComputedFundAccount(page: Page): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinFundAccount',
    {
      code: uniq('FA-FX'),
      name: 'E2E FX Discount Bank Account (USD)',
      orgId: ORG_ID,
      accountType: 'BANK',
      subjectId: 2, // 1002 银行存款（种子 id=2）
      currencyId: CURRENCY_USD,
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

/**
 * 经 `ErpFinVoucher__post(event:PostingEventInput)` 直接构造过账凭证。返回新凭证 id。
 *
 * `post` 返回 Long scalar（非实体），不经选择集——直接构造原始 mutation（与
 * `finance-voucher-post.action.spec.ts:50` + `orchestration/_helper.ts:runP2pReverse` 范式一致）。
 * `event` 经 GraphQL variable + 显式 input 类型 `PostingEventInput` 传递。
 */
async function postVoucher(
  page: Page,
  event: Record<string, unknown>,
): Promise<{ voucherId: number | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation($e:${POSTING_EVENT_INPUT}){ ErpFinVoucher__post(event:$e) }`,
    { e: event },
  );
  const raw = json?.data?.ErpFinVoucher__post;
  const voucherId = raw == null ? null : Number(raw);
  return { voucherId, errors: json?.errors ?? null, json };
}

/**
 * 构造 NOTES_RECEIVABLE_DISCOUNTED PostingEvent（对齐 NotesPostingDispatcher.buildReceivableEvent:71-98
 * 派生的 event 结构）。spec 直驱此 event 验证 Provider FX 6051 分支（discount mutation 因 builder
 * `:249` 硬编码 ZERO 不可达此分支，见 spec 头部说明 + plan Phase 1 Decision (a)）。
 */
function buildNotesReceivableDiscountedEvent(
  billHeadCode: string,
  opts: {
    currencyId: number;
    exchangeRate: number;
    faceAmount: number;
    discountInterest: number;
    netAmount: number;
    exchangeGainLoss: number;
  },
): Record<string, unknown> {
  return {
    businessType: 'NOTES_RECEIVABLE_DISCOUNTED',
    billHeadCode,
    orgId: ORG_ID,
    acctSchemaId: ACCT_SCHEMA_ID,
    currencyId: opts.currencyId,
    exchangeRate: opts.exchangeRate,
    voucherDate: DISCOUNT_DATE,
    billData: {
      partnerId: PARTNER_ID,
      FACE_AMOUNT: opts.faceAmount,
      businessDate: BDATE,
      dueDate: DUE_DATE,
      DISCOUNT_INTEREST: opts.discountInterest,
      NET_AMOUNT: opts.netAmount,
      EXCHANGE_GAIN_LOSS: opts.exchangeGainLoss,
    },
  };
}

test.describe('finance ErpFinNotesReceivable FX discount — multi-currency voucher lines E2E', () => {
  test('FX lifecycle via discount mutation: USD note → DISCOUNTED + 3-line voucher (6051 suppressed by builder ZERO)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const fund = await createFxComputedFundAccount(page);
    const note = await createFxNote(page, { status: 'RECEIVED' });
    let discountId: string | number | null = null;
    try {
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

      // 凭证行断言：3 行（builder :249 硬编码 exchangeGainLoss=ZERO → Provider :72 signum=0 抑制 6051）
      // 金额全部 functional CNY（dispatcher :84 billData.FACE_AMOUNT=note.amountFunctional=666.67）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_DISCOUNTED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_FX, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_FX, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note, { discountId, fundAccountId: fund.id });
    }
  });

  test('FX voucher via ErpFinVoucher__post: 4 lines with Dr 6051 (exchangeGainLoss signum>0 branch)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinVoucher-main');

    const billHeadCode = `E2E-FX-NR-POST-${Date.now()}`;
    const event = buildNotesReceivableDiscountedEvent(billHeadCode, {
      currencyId: CURRENCY_USD,
      exchangeRate: EXCHANGE_RATE_USD_CNY,
      faceAmount: FACE_AMOUNT_FUNCTIONAL_CNY,
      discountInterest: DISCOUNT_INTEREST_FX,
      netAmount: NET_AMOUNT_FX_VOUCHER,
      exchangeGainLoss: EXCHANGE_GAIN_LOSS_FX,
    });

    const { voucherId, errors } = await postVoucher(page, event);
    expect(errors, `ErpFinVoucher__post should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
    expect(voucherId, 'post should return non-null voucherId').not.toBeNull();

    try {
      // FX 4 行凭证断言：Dr 1002 netAmount / Dr 6603 interest / Dr 6051 fx (signum>0) / Cr 1121 face
      // Provider :72-78 if (fx.signum() != 0) 分支命中，fx=5.00>0 走 :73-74 Dr 6051=5.00
      // 平衡：655.00 + 6.67 + 5.00 = 666.67（详见 NET_AMOUNT_FX_VOUCHER 派生注释）
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_FX_VOUCHER, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_FX, creditAmount: 0 },
        { subjectCode: '6051', dcDirection: 'DEBIT', debitAmount: EXCHANGE_GAIN_LOSS_FX, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupVoucherByBillCode(page, billHeadCode);
    }
  });

  test('control: single-currency voucher via ErpFinVoucher__post with exchangeGainLoss=0 → 3 lines (6051 suppressed)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinVoucher-main');

    const billHeadCode = `E2E-CNY-NR-POST-${Date.now()}`;
    const event = buildNotesReceivableDiscountedEvent(billHeadCode, {
      currencyId: CURRENCY_CNY,
      exchangeRate: 1,
      faceAmount: FACE_AMOUNT_CNY,
      discountInterest: DISCOUNT_INTEREST_CNY,
      netAmount: NET_AMOUNT_CNY,
      exchangeGainLoss: 0,
    });

    const { voucherId, errors } = await postVoucher(page, event);
    expect(errors, `ErpFinVoucher__post should not return GraphQL errors: ${JSON.stringify(errors)}`).toBeNull();
    expect(voucherId, 'post should return non-null voucherId').not.toBeNull();

    try {
      // 单币种 3 行凭证断言：signum=0 → Provider :72 抑制 6051 行
      // 与 FX test 唯一变量：billData.EXCHANGE_GAIN_LOSS（5.00 vs 0） → 4 行 vs 3 行 crisp 对照
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);
    } finally {
      await cleanupVoucherByBillCode(page, billHeadCode);
    }
  });
});
