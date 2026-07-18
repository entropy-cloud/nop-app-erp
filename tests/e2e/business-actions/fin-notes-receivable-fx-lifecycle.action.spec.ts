import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
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
 * finance ErpFinNotesReceivable 多币种票据 honor/endorse/collect 生命周期浏览器层 E2E
 * （plan 2026-07-19-0330-1，承接 0120-1 Deferred「外币票据 honor/dishonor/endorse/collect
 * 路径浏览器层 E2E」successor）。
 *
 * 验证四层：
 * (1) **FX endorse 路径**（`ErpFinNotesReceivable__endorse` @BizMutation）：建 USD 票据
 *     （currencyId=2 + exchangeRate=6.6667 + amountSource=USD 1000 + amountFunctional=CNY 6666.7）
 *     → endorse → ENDORSED + posted=true + 2 行凭证（Dr 2202 应付账款 / Cr 1121 应收票据，全部
 *     functional CNY，无 6051——Provider ENDORSED 路径无 FX 分支为设计选择）。
 * (2) **FX collect（无凭证）+ honor 路径**：建 USD 票据 → collect → COLLECTION_PENDING + posted=false
 *     + 无凭证（对齐 1430-1 l.213-215 范式）→ honor → HONORED + posted=true + 2 行凭证
 *     （Dr 1002 银行存款 / Cr 1121 应收票据，全部 functional CNY，经 NOTES_RECEIVABLE_COLLECTION
 *     业务类型过账）。
 * (3) **FX dishonor 路径**：建 USD 票据 + 前置 __save 直置 status=COLLECTION_PENDING 入口
 *     （`validateTransitionForHonorOrDishonor` 守卫要求此状态）→ dishonor → DISHONORED + 显式
 *     断言无凭证（对齐 1430-1 dishonor 范式）+ 非法迁移守卫断言（RECEIVED 态直接 dishonor 拒绝）。
 * (4) **单币种对照测试用例**：建 CNY 票据（currencyId=1 + exchangeRate=1 + amountSource=
 *     amountFunctional=1000）→ 同 (1)(2) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，
 *     唯一变量为金额（1000 vs 6666.7），证明 Provider 无 FX 分支语义对单/外币一致。
 *
 * ## 后端路径（实时仓库核实 plan Phase 1 Proof）
 *
 * - `ErpFinNotesReceivableProcessor.java`：
 *   - `endorse:62-66` Facade → `doEndorse:184-197` 调 `postingDispatcher.tryPostReceivable(NOTES_RECEIVABLE_ENDORSED)`
 *   - `collect:68-74` 仅 `setStatus(NOTES_RECV_COLLECTION_PENDING) + updateEntity` **无 postingDispatcher 调用**
 *   - `honor:76-80` Facade → `doHonor:199-208` 调 `postingDispatcher.tryPostReceivable(NOTES_RECEIVABLE_COLLECTION)`
 *   - `dishonor:82-89` 仅 `setStatus(NOTES_RECV_DISHONORED) + updateEntity` **无 postingDispatcher 调用**
 *   - `validateTransitionForHonorOrDishonor:124-129` 守卫 status=COLLECTION_PENDING
 * - `NotesPostingDispatcher.buildReceivableEvent:84`：`billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))`
 *   → face amount 透传 functional 金额至 Provider，凭证行金额单位为 CNY。
 * - `NotesReceivableAcctDocProvider.createFacts`：
 *   - ENDORSED 路径（:82-89）：Dr 2202 应付账款（partnerId 非空）/ Cr 1121 应收票据 = face，**无 FX 分支**
 *   - COLLECTION 路径（:90-95）：Dr 1002 银行存款 / Cr 1121 应收票据 = face，**无 FX 分支**
 *
 * ## 数值表（全部以 functional CNY 计）
 *
 * - FX 数值（functional CNY）：
 *   - 汇率方向：functional/source（USD→CNY 6.6667 表示 1 USD = 6.6667 CNY，经 0120-1 实测确认）
 *   - amountSource = USD 1000
 *   - exchangeRate = 6.6667
 *   - amountFunctional = 1000 × 6.6667 = 6666.7000（HALF_UP scale 4，对齐 orm.xml:1335-1336 NR amount 列）
 *   - 凭证行金额 = functional 6666.7（dispatcher 透传 amountFunctional → Provider face）
 * - 对照 CNY 数值（functional CNY）：
 *   - amountSource = amountFunctional = 1000
 *   - 凭证行金额 = 1000
 *
 * ## 自包含 setup + 清理
 *
 * - 自包含建 USD/CNY `ErpFinNotesReceivable`（status 直置入口，对齐 1430-1 l.238 + 0120-1 范式）
 *   ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow，DIRECT 可达。
 * - 清理：voucher 经 `cleanupVoucherByBillCode` 删凭证行+凭证+回链；note 经 `deleteById` 删主实体。
 */

const ORG_ID = 2;
const CURRENCY_CNY = 1;
const CURRENCY_USD = 2;
const PARTNER_ID = 1; // CUST-001（partner 维度，ENDORSED 路径凭证 partnerId 维度）
const NOTES_TYPE = 'BANK_ACCEPTANCE';
const ISSUE_DATE = '2026-07-01';
const DUE_DATE = '2026-07-31';

// FX 数值（functional CNY 计入凭证）
const EXCHANGE_RATE_USD_CNY = 6.6667; // functional/source
const FACE_AMOUNT_SOURCE_USD = 1000; // USD 票面（amountSource）
const FACE_AMOUNT_FUNCTIONAL_CNY = 6666.7; // CNY 本位币票面（amountFunctional，6666.7000）

// 对照 CNY 数值（functional CNY）
const FACE_AMOUNT_CNY = 1000;

let _seq = 0;
// 紧凑唯一码（对齐 1430-1 紧凑 base36 范式：ErpFinArApItemGenerator.buildCode 拼为 AR/AP 辅助账
// code 超 voucherCode precision 50 时溢出，紧凑码绕过；详见 1430-1 spec 头部注释）。
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
      code: uniq('NR-FX-LC'),
      orgId: ORG_ID,
      notesType: NOTES_TYPE,
      notesNo: uniq('NN-FX-LC'),
      currencyId: CURRENCY_USD,
      exchangeRate: EXCHANGE_RATE_USD_CNY,
      amountSource: FACE_AMOUNT_SOURCE_USD,
      amountFunctional: FACE_AMOUNT_FUNCTIONAL_CNY,
      partnerId: PARTNER_ID,
      issueDate: ISSUE_DATE,
      dueDate: DUE_DATE,
      ...overrides,
    },
    'id code',
  );
}

async function createCnyNote(
  page: Page,
  overrides: Record<string, unknown> = {},
): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpFinNotesReceivable',
    {
      code: uniq('NR-CNY-LC'),
      orgId: ORG_ID,
      notesType: NOTES_TYPE,
      notesNo: uniq('NN-CNY-LC'),
      currencyId: CURRENCY_CNY,
      exchangeRate: 1,
      amountSource: FACE_AMOUNT_CNY,
      amountFunctional: FACE_AMOUNT_CNY,
      partnerId: PARTNER_ID,
      issueDate: ISSUE_DATE,
      dueDate: DUE_DATE,
      ...overrides,
    },
    'id code',
  );
}

async function cleanupNote(
  page: Page,
  note: { id?: string; code?: string } | null,
): Promise<void> {
  if (!note) return;
  if (note.code) await cleanupVoucherByBillCode(page, note.code);
  if (note.id != null) await deleteById(page, 'ErpFinNotesReceivable', note.id);
}

test.describe('finance ErpFinNotesReceivable FX lifecycle — honor/endorse/collect multi-currency voucher lines E2E', () => {
  test('FX endorse: USD note RECEIVED → ENDORSED + voucher Dr 2202 / Cr 1121 (functional CNY, no 6051)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createFxNote(page, { status: 'RECEIVED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'endorse',
        // endorsementFromId 经 GraphQL @Name 标非空（Nop 默认），传 note 自身 id 作有效 FK 自引用
        // （Processor doEndorse 仅记录 endorsementFromId，不影响过账；语义为背书链路来源票据）。
        { notesId: note.id, endorsementFromId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX endorse → ENDORSED').toBe('ENDORSED');
      expect(r.posted, 'FX endorse → posted=true').toBe(true);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(v.status, '__get confirms ENDORSED').toBe('ENDORSED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      // 凭证行断言：2 行 Dr 2202 / Cr 1121 全部 functional CNY
      // Provider ENDORSED 路径无 FX 分支，无 6051（设计选择非缺陷）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_ENDORSED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_FUNCTIONAL_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('FX collect (no voucher) → honor: USD note → COLLECTION_PENDING + no voucher → HONORED + voucher Dr 1002 / Cr 1121', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createFxNote(page, { status: 'RECEIVED' });
    try {
      const collected = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'collect',
        { notesId: note.id },
        'id status posted',
      );
      expect(collected.status, 'FX collect → COLLECTION_PENDING').toBe('COLLECTION_PENDING');
      expect(collected.posted, 'FX collect is intermediate → posted unchanged (false)').toBe(false);

      // collect 无过账：billCode 无 NORMAL 凭证（对齐 1430-1 spec l.213-215 范式）
      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'FX collect should NOT produce a voucher').toBeNull();

      const honored = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'honor',
        { notesId: note.id },
        'id status posted',
      );
      expect(honored.status, 'FX honor → HONORED').toBe('HONORED');
      expect(honored.posted, 'FX honor → posted=true').toBe(true);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(v.status, '__get confirms HONORED').toBe('HONORED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      // 凭证行断言：2 行 Dr 1002 / Cr 1121 全部 functional CNY
      // Provider COLLECTION 路径无 FX 分支，无 6051（设计选择非缺陷）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_COLLECTION NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_FUNCTIONAL_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('FX dishonor: USD note COLLECTION_PENDING → DISHONORED (no posting) + guard from non-COLLECTION_PENDING', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    // 前置 __save 直置 status=COLLECTION_PENDING 入口（对齐 1430-1 spec l.238 范式）
    // 因 validateTransitionForHonorOrDishonor:124-129 守卫要求此状态
    const note = await createFxNote(page, { status: 'COLLECTION_PENDING' });
    // 守卫断言用：建一张 RECEIVED 状态票据，dishonor 应被守卫拒绝
    const receivedNote = await createFxNote(page, { status: 'RECEIVED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'dishonor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX dishonor → DISHONORED').toBe('DISHONORED');
      expect(r.posted, 'FX dishonor is terminal marker → no posting').toBe(false);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status');
      expect(v.status, '__get confirms DISHONORED').toBe('DISHONORED');

      // dishonor 无过账：billCode 无 NORMAL 凭证（对齐 1430-1 dishonor 范式）
      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'FX dishonor should NOT produce a voucher').toBeNull();

      // 非法迁移守卫：RECEIVED 态直接 dishonor 抛守卫
      const rejDishonor = await callMutation(
        page, 'ErpFinNotesReceivable', 'dishonor',
        { notesId: receivedNote.id },
        'id',
      );
      expect(rejDishonor.errors, 'dishonor from RECEIVED should be rejected').toBeTruthy();

      // 状态不变
      const s = await verifyState(page, 'ErpFinNotesReceivable', receivedNote.id, 'status');
      expect(s.status, 'received note status unchanged after guard').toBe('RECEIVED');
    } finally {
      await cleanupNote(page, note);
      await cleanupNote(page, receivedNote);
    }
  });

  test('control: single-currency CNY note endorse + collect→honor → same subjects/directions as FX (only amount differs)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    // endorse 路径对照
    const endorseNote = await createCnyNote(page, { status: 'RECEIVED' });
    // collect→honor 路径对照
    const collectNote = await createCnyNote(page, { status: 'RECEIVED' });
    try {
      // endorse 路径
      const endorsed = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'endorse',
        { notesId: endorseNote.id, endorsementFromId: endorseNote.id },
        'id status posted',
      );
      expect(endorsed.status, 'CNY endorse → ENDORSED').toBe('ENDORSED');
      expect(endorsed.posted, 'CNY endorse → posted=true').toBe(true);

      const endorseVid = await findVoucherIdByBillCode(page, endorseNote.code, 'NORMAL');
      expect(endorseVid, 'CNY ENDORSED voucher exists').toBeTruthy();
      // 凭证行集合 = FX endorse 路径科目+方向完全一致（Dr 2202 / Cr 1121），唯一变量为金额 1000 vs 6666.7
      await assertVoucherLines(page, endorseVid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);

      // collect → honor 路径
      const collected = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'collect',
        { notesId: collectNote.id },
        'id status posted',
      );
      expect(collected.status, 'CNY collect → COLLECTION_PENDING').toBe('COLLECTION_PENDING');
      expect(collected.posted, 'CNY collect → posted=false').toBe(false);

      const noVoucher = await findVoucherIdByBillCode(page, collectNote.code, 'NORMAL');
      expect(noVoucher, 'CNY collect should NOT produce a voucher').toBeNull();

      const honored = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'honor',
        { notesId: collectNote.id },
        'id status posted',
      );
      expect(honored.status, 'CNY honor → HONORED').toBe('HONORED');
      expect(honored.posted, 'CNY honor → posted=true').toBe(true);

      const honorVid = await findVoucherIdByBillCode(page, collectNote.code, 'NORMAL');
      expect(honorVid, 'CNY COLLECTION voucher exists').toBeTruthy();
      // 凭证行集合 = FX honor 路径科目+方向完全一致（Dr 1002 / Cr 1121），唯一变量为金额 1000 vs 6666.7
      await assertVoucherLines(page, honorVid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);
    } finally {
      await cleanupNote(page, endorseNote);
      await cleanupNote(page, collectNote);
    }
  });
});
