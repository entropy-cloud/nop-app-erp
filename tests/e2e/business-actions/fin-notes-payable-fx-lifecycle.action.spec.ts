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
 * finance ErpFinNotesPayable 多币种票据 ISSUED/HONORED/writeOff REVERSAL/dishonor
 * 生命周期浏览器层 E2E（plan 2026-07-19-0330-2，承接 0120-1 Deferred「应付票据外币
 * ISSUED/HONORED 多币种路径浏览器层 E2E」successor）。
 *
 * 验证五层：
 * (1) **FX ISSUED 路径**（`ErpFinNotesPayable__issue` @BizMutation）：建 USD 票据
 *     （currencyId=2 + exchangeRate=6.6667 + amountSource=USD 1000 + amountFunctional=CNY 6666.7）
 *     → issue → ISSUED + posted=true + 2 行凭证（Dr 2202 应付账款 / Cr 2203 应付票据，全部
 *     functional CNY，无 6051——Provider ISSUED 路径无 FX 分支为设计选择）。
 * (2) **FX HONORED 路径**：建 USD 票据 + 前置 __save 直置 status=ISSUED 入口 → honor →
 *     HONORED + posted=true + 2 行凭证（Dr 2203 应付票据 / Cr 1002 银行存款，全部 functional CNY）。
 * (3) **FX writeOff REVERSAL 路径**：建 USD 票据 → issue 产 ISSUED+posted=true → writeOff →
 *     WRITE_OFF + posted=false + REVERSAL 红字凭证（Dr 2202=-functional / Cr 2203=-functional
 *     对原 ISSUED NORMAL 凭证）。
 * (4) **FX dishonor 路径**（无 FX 特定语义，spec 行为对称纳入）：建 USD 票据 + 前置 ISSUED 入口
 *     → dishonor → DISHONORED + 显式断言无凭证（对齐 1430-1 test 3 + doDishonor:141-144
 *     仅 setStatus 无 posting 范式）+ 非法迁移守卫断言（非 ISSUED 态直接 dishonor 拒绝）。
 * (5) **单币种对照测试用例**：建 CNY 票据（currencyId=1 + exchangeRate=1 + amountSource=
 *     amountFunctional=1000）→ 同 (1)(2)(3) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，
 *     唯一变量为金额（1000 vs 6666.7），证明 Provider 无 FX 分支语义对单/外币一致。
 *
 * ## 后端路径（实时仓库核实 plan Phase 1 Proof）
 *
 * - `ErpFinNotesPayableProcessor.java`：
 *   - `issue:43-53` Facade → `doIssue:119-128` 调 `postingDispatcher.tryPostPayable(NOTES_PAYABLE_ISSUED)`
 *   - `honor:55-60` Facade → `validateTransitionForHonor:80-85` 守卫 status=ISSUED → `doHonor:130-139`
 *     调 `postingDispatcher.tryPostPayable(NOTES_PAYABLE_HONORED)`
 *   - `dishonor:62-68` Facade → `validateTransitionForHonor:80-85` 守卫 status=ISSUED →
 *     `doDishonor:141-144` 仅 `setStatus(NOTES_PAY_DISHONORED) + updateEntity` **无 postingDispatcher 调用**
 *   - `writeOff:70-76` Facade → `doWriteOff:146-155` if posted=true 调
 *     `postingDispatcher.reversePayable(NOTES_PAYABLE_ISSUED)` → setStatus WRITE_OFF + 清 posted 三件套
 * - `NotesPostingDispatcher.buildPayableEvent:107,117`：currencyId/exchangeRate 透传至 PostingEvent，
 *   `:117` `billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))` → face amount 透传
 *   functional 金额至 Provider，凭证行金额单位为 CNY。
 * - `NotesPostingDispatcher.reversePayable:53-55` → `executor.reverse(note.code, businessType)`：
 *   平台按 billHeadCode + businessType 反查原 NORMAL 凭证复制凭证行同向取负生成 REVERSAL 凭证，
 *   FX 正确性继承自原 NORMAL 已 functional。
 * - `NotesPayableAcctDocProvider.createFacts`：
 *   - ISSUED 路径（:48-53）：Dr 2202 应付账款（partnerId 非空）/ Cr 2203 应付票据 = face，**无 FX 分支**
 *   - HONORED 路径（:55-58）：Dr 2203 应付票据 / Cr 1002 银行存款 = face，**无 FX 分支**
 *
 * ## 数值表（全部以 functional CNY 计）
 *
 * - FX 数值（functional CNY）：
 *   - 汇率方向：functional/source（USD→CNY 6.6667 表示 1 USD = 6.6667 CNY）
 *   - amountSource = USD 1000
 *   - exchangeRate = 6.6667
 *   - amountFunctional = 1000 × 6.6667 = 6666.7000（HALF_UP scale 4，对齐 orm.xml:1376-1377 NP amount 列）
 *   - 凭证行金额 = functional 6666.7（dispatcher 透传 amountFunctional → Provider face）
 * - 对照 CNY 数值（functional CNY）：
 *   - amountSource = amountFunctional = 1000
 *   - 凭证行金额 = 1000
 *
 * ## 自包含 setup + 清理
 *
 * - 自包含建 USD/CNY `ErpFinNotesPayable`（status 直置入口，对齐 1430-1 范式）
 *   ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow，DIRECT 可达。
 * - 主路径用 COMMERCIAL_ACCEPTANCE 短路跳过授信校验（isBankAcceptance=false → reserveCreditIfNeeded 跳过）。
 * - 清理：voucher 经 `cleanupVoucherByBillCode` 删凭证行+凭证+回链；note 经 `deleteById` 删主实体。
 */

const ORG_ID = 2;
const CURRENCY_CNY = 1;
const CURRENCY_USD = 2;
const PARTNER_ID = 3; // SUP-001（AP 方向 partner 维度，ISSUED dr.setPartnerId）
const NOTES_TYPE = 'COMMERCIAL_ACCEPTANCE'; // 主路径短路授信校验
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
    'ErpFinNotesPayable',
    {
      code: uniq('NP-FX-LC'),
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
    'ErpFinNotesPayable',
    {
      code: uniq('NP-CNY-LC'),
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
  if (note.id != null) await deleteById(page, 'ErpFinNotesPayable', note.id);
}

test.describe('finance ErpFinNotesPayable FX lifecycle — ISSUED/HONORED/writeOff REVERSAL multi-currency voucher lines E2E', () => {
  test('FX ISSUED: USD note → ISSUED + posted + voucher Dr 2202 / Cr 2203 (functional CNY, no 6051)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createFxNote(page);
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX issue → ISSUED').toBe('ISSUED');
      expect(r.posted, 'FX issue → posted=true').toBe(true);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(v.status, '__get confirms ISSUED').toBe('ISSUED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      // 凭证行断言：2 行 Dr 2202 / Cr 2203 全部 functional CNY
      // Provider ISSUED 路径无 FX 分支，无 6051（设计选择非缺陷）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_PAYABLE_ISSUED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_FUNCTIONAL_CNY, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('FX HONORED: USD note ISSUED → HONORED + posted + voucher Dr 2203 / Cr 1002', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    // 前置 __save 直置 status=ISSUED 入口（validateTransitionForHonor:80-85 守卫要求此状态）
    const note = await createFxNote(page, { status: 'ISSUED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'honor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX honor → HONORED').toBe('HONORED');
      expect(r.posted, 'FX honor → posted=true').toBe(true);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(v.status, '__get confirms HONORED').toBe('HONORED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      // 凭证行断言：2 行 Dr 2203 / Cr 1002 全部 functional CNY
      // Provider HONORED 路径无 FX 分支，无 6051（设计选择非缺陷）
      // __save 直置 ISSUED 未过账，honor 产 HONORED NORMAL 凭证（唯一 billCode 凭证）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_PAYABLE_HONORED NORMAL voucher exists for FX note').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2203', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_FUNCTIONAL_CNY, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('FX writeOff: USD note ISSUED(posted) → WRITE_OFF + posted=false + REVERSAL Dr -2202 / Cr -2203', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createFxNote(page);
    try {
      // 先 issue 产 ISSUED + posted=true（writeOff 须 posted==true 才红冲 NOTES_PAYABLE_ISSUED）
      await callMutationOk(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: note.id },
        'id status posted',
      );
      const before = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(before.posted, 'precondition: issue sets posted=true').toBe(true);

      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'writeOff',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX writeOff → WRITE_OFF').toBe('WRITE_OFF');
      expect(r.posted, 'FX writeOff → posted=false (entity-level rollback marker)').toBe(false);

      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(v.status, '__get confirms WRITE_OFF').toBe('WRITE_OFF');
      expect(v.posted, '__get confirms posted=false').toBe(false);

      // REVERSAL 凭证同向取负（doWriteOff 红冲 NOTES_PAYABLE_ISSUED）
      // FX 正确性继承自原 NORMAL 凭证（已 functional），REVERSAL 行金额同样取负 functional
      const reversalVid = await findVoucherIdByBillCode(page, note.code, 'REVERSAL');
      expect(reversalVid, 'FX writeOff should produce REVERSAL voucher').toBeTruthy();
      await assertVoucherLines(page, reversalVid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: -FACE_AMOUNT_FUNCTIONAL_CNY, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -FACE_AMOUNT_FUNCTIONAL_CNY },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('FX dishonor: USD note ISSUED → DISHONORED (no posting) + guard from non-ISSUED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    // 前置 __save 直置 status=ISSUED 入口（validateTransitionForHonor:80-85 守卫要求此状态）
    // dishonor 与 honor 共用 validateTransitionForHonor 守卫
    const note = await createFxNote(page, { status: 'ISSUED' });
    // 守卫断言用：建一张未签发（status=null）票据，dishonor 应被守卫拒绝
    const blankNote = await createFxNote(page);
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'dishonor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'FX dishonor → DISHONORED').toBe('DISHONORED');
      expect(r.posted, 'FX dishonor is terminal marker → no posting').toBe(false);

      // __get 权威查库断言状态翻转
      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status');
      expect(v.status, '__get confirms DISHONORED').toBe('DISHONORED');

      // dishonor 无过账：billCode 无 NORMAL 凭证（对齐 1430-1 test 3 dishonor 范式）
      // doDishonor:141-144 仅 setStatus 无 postingDispatcher 调用 → 永不产凭证
      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'FX dishonor should NOT produce a voucher').toBeNull();

      // 非法迁移守卫：未签发（status=null）态直接 dishonor 抛守卫
      const rejDishonor = await callMutation(
        page, 'ErpFinNotesPayable', 'dishonor',
        { notesId: blankNote.id },
        'id',
      );
      expect(rejDishonor.errors, 'dishonor from non-ISSUED should be rejected').toBeTruthy();

      // 状态不变
      const s = await verifyState(page, 'ErpFinNotesPayable', blankNote.id, 'status posted');
      expect(s.status ?? null, 'blank note status unchanged (null) after guard').toBeNull();
      expect(s.posted, 'blank note posted=false after guard').toBe(false);
    } finally {
      await cleanupNote(page, note);
      await cleanupNote(page, blankNote);
    }
  });

  test('control: single-currency CNY note issue + honor + writeOff → same subjects/directions as FX (only amount differs)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    // honor 路径对照（前置 ISSUED）
    const honorNote = await createCnyNote(page, { status: 'ISSUED' });
    // writeOff 路径对照（issue 前置 posted=true）
    const writeOffNote = await createCnyNote(page);
    // issue 路径对照
    const issueNote = await createCnyNote(page);
    try {
      // (1) issue 路径
      const issued = await callMutationOk(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: issueNote.id },
        'id status posted',
      );
      expect(issued.status, 'CNY issue → ISSUED').toBe('ISSUED');
      expect(issued.posted, 'CNY issue → posted=true').toBe(true);

      const issueVid = await findVoucherIdByBillCode(page, issueNote.code, 'NORMAL');
      expect(issueVid, 'CNY ISSUED voucher exists').toBeTruthy();
      // 凭证行集合 = FX ISSUED 路径科目+方向完全一致（Dr 2202 / Cr 2203），唯一变量为金额 1000 vs 6666.7
      await assertVoucherLines(page, issueVid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);

      // (2) honor 路径
      const honored = await callMutationOk(
        page, 'ErpFinNotesPayable', 'honor',
        { notesId: honorNote.id },
        'id status posted',
      );
      expect(honored.status, 'CNY honor → HONORED').toBe('HONORED');
      expect(honored.posted, 'CNY honor → posted=true').toBe(true);

      const honorVid = await findVoucherIdByBillCode(page, honorNote.code, 'NORMAL');
      expect(honorVid, 'CNY HONORED voucher exists').toBeTruthy();
      // 凭证行集合 = FX HONORED 路径科目+方向完全一致（Dr 2203 / Cr 1002），唯一变量为金额 1000 vs 6666.7
      await assertVoucherLines(page, honorVid, [
        { subjectCode: '2203', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT_CNY },
      ]);

      // (3) writeOff REVERSAL 路径
      await callMutationOk(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: writeOffNote.id },
        'id status posted',
      );
      const writeOffResult = await callMutationOk(
        page, 'ErpFinNotesPayable', 'writeOff',
        { notesId: writeOffNote.id },
        'id status posted',
      );
      expect(writeOffResult.status, 'CNY writeOff → WRITE_OFF').toBe('WRITE_OFF');
      expect(writeOffResult.posted, 'CNY writeOff → posted=false').toBe(false);

      const reversalVid = await findVoucherIdByBillCode(page, writeOffNote.code, 'REVERSAL');
      expect(reversalVid, 'CNY writeOff should produce REVERSAL voucher').toBeTruthy();
      // 凭证行集合 = FX writeOff REVERSAL 路径科目+方向完全一致（Dr -2202 / Cr -2203），唯一变量为金额 -1000 vs -6666.7
      await assertVoucherLines(page, reversalVid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: -FACE_AMOUNT_CNY, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -FACE_AMOUNT_CNY },
      ]);
    } finally {
      await cleanupNote(page, issueNote);
      await cleanupNote(page, honorNote);
      await cleanupNote(page, writeOffNote);
    }
  });
});
