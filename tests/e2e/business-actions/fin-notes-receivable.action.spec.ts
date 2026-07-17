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
import { cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * finance ErpFinNotesReceivable 应收票据 DIRECT 业务动作生命周期浏览器层 E2E
 * （plan 2026-07-17-1430-1 Phase 2）。
 *
 * 验证 7 动作状态机正路径 + 4 业财过账凭证行精确数值断言（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/
 * COLLECTION）+ collect/dishonor 无过账终态/中间态 + writeOff 红冲回退 + 非法迁移守卫。
 *
 * 权威凭证结构（NotesReceivableAcctDocProvider:42-100，billHeadCode=note.code，NotesPostingDispatcher:74）：
 *   RECEIVED:    Dr 1121(应收票据)=face / Cr 1122(应收账款)=face
 *   DISCOUNTED:  Dr 1002(银行存款)=netAmount / Dr 6603(财务费用-利息支出)=discountInterest / Cr 1121=face
 *                （单币种 exchangeGainLoss=0，6051 fx 行 signum()!=0 抑制不发，三件套）
 *   ENDORSED:    Dr 2202(应付账款)=face / Cr 1121=face
 *   COLLECTION:  Dr 1002(银行存款)=face / Cr 1121=face
 *
 * 贴现确定性派生（ErpFinNotesReceivableProcessor.buildDiscount:226-251，HALF_UP scale 2）：
 *   discountInterest = face × rate × remainingDays / 360；remainingDays = dueDate − discountDate（须 > 0）。
 *   setup：face=1000 / rate=0.12 / discountDate=2026-07-01 / dueDate=2026-07-31 → remainingDays=30
 *          → discountInterest = 1000×0.12×30/360 = 10.00；netAmount = 990。
 *
 * writeOff 红冲（ErpFinNotesReceivableProcessor.doWriteOff:210-222 + businessTypeForStatus:256）：
 *   posted==true 时经 executor.reverse(note.code, businessTypeForStatus(status)) 写 REVERSAL 凭证
 *   （**同 billHeadCode=note.code**，须 findVoucherIdByBillCode(code,'REVERSAL') 按 postingType 区分），
 *   随后 setPosted(false)+清 postedAt/postedBy。REVERSAL 同向取负（dcDirection 不变金额取负）。
 *
 * 自包含 setup：__save 直置 status 入口（ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow，DIRECT 可达）。
 *   必填：code/orgId/notesType/currencyId；amountFunctional>0（requireAmountPositive）；partnerId 驱动凭证
 *   partner 维度（-provider 设 partnerId）；discount 路径须 dueDate!=null + bankId!=null（自包含建 FundAccount）。
 *
 * 种子引用：org id=2 / currency id=1（CNY）/ partner id=1（CUST-001，AR 方向）/ 科目 1121 id=40、1122 id=3、
 *   1002 id=2、2202 id=5、6603 id=42 经种子补齐（本 plan Phase 2）。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const PARTNER_ID = 1; // CUST-001（AR 方向 partner 维度）
const NOTES_TYPE = 'BANK_ACCEPTANCE';
const FACE_AMOUNT = 1000;
const ISSUE_DATE = '2026-07-01';
const DUE_DATE = '2026-07-31';
const DISCOUNT_DATE = '2026-07-01'; // remainingDays = 30（DUE_DATE − DISCOUNT_DATE）
const DISCOUNT_RATE = 0.12;
const DISCOUNT_INTEREST = 10; // 1000 × 0.12 × 30 / 360 = 10.00（HALF_UP scale 2）
const NET_AMOUNT = 990; // 1000 − 10

let _seq = 0;
// 紧凑唯一码：note.code 经 ErpFinArApItemGenerator.buildCode 拼为 AR/AP 辅助账 code
// （"ARI-NOTES_RECEIVABLE-" + code + "-" + uuid8），voucherCode 精度 50，故 note.code 须 ≤ ~19 字符。
// 生产票据码（如 NR-2026-0001）天然简短；此处 base36 时间戳保证跨运行唯一且紧凑。
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now().toString(36)}${_seq}`;
}

async function createNote(
  page: import('@playwright/test').Page,
  overrides: Record<string, unknown> = {},
): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpFinNotesReceivable',
    {
      code: uniq('NR'),
      orgId: ORG_ID,
      notesType: NOTES_TYPE,
      notesNo: uniq('NN'),
      currencyId: CURRENCY_ID,
      exchangeRate: 1,
      amountSource: FACE_AMOUNT,
      amountFunctional: FACE_AMOUNT,
      partnerId: PARTNER_ID,
      issueDate: ISSUE_DATE,
      dueDate: DUE_DATE,
      ...overrides,
    },
    'id code',
  );
}

async function createFundAccount(page: import('@playwright/test').Page): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinFundAccount',
    {
      code: uniq('FA'),
      name: 'E2E NR Discount Bank Account',
      orgId: ORG_ID,
      accountType: 'BANK',
      subjectId: 2, // 1002 银行存款（种子 id=2）
      currencyId: CURRENCY_ID,
      currentBalance: 0,
      status: 'ACTIVE',
    },
    'id',
  );
}

async function cleanupNote(
  page: import('@playwright/test').Page,
  note: { id?: string; code?: string } | null,
  opts: { discountId?: string | number | null; fundAccountId?: string | number | null } = {},
): Promise<void> {
  if (!note) return;
  if (note.code) await cleanupVoucherByBillCode(page, note.code);
  if (opts.discountId != null) await deleteById(page, 'ErpFinNotesDiscount', opts.discountId);
  if (note.id != null) await deleteById(page, 'ErpFinNotesReceivable', note.id);
  if (opts.fundAccountId != null) await deleteById(page, 'ErpFinFundAccount', opts.fundAccountId);
}

test.describe('finance ErpFinNotesReceivable lifecycle + voucher-line assertions', () => {
  test('receive: → RECEIVED + posted + voucher Dr 1121 / Cr 1122', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createNote(page);
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'receive',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'receive → RECEIVED').toBe('RECEIVED');
      expect(r.posted, 'receive → posted=true').toBe(true);

      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(v.status, '__get confirms RECEIVED').toBe('RECEIVED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_RECEIVED NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1121', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1122', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('discount: RECEIVED → DISCOUNTED + voucher Dr 1002(net) / Dr 6603(interest) / Cr 1121(face)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const fund = await createFundAccount(page);
    const note = await createNote(page, { status: 'RECEIVED' });
    let discountId: string | number | null = null;
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'discount',
        { notesId: note.id, discountDate: DISCOUNT_DATE, bankId: fund.id, discountRate: DISCOUNT_RATE },
        'id status posted discountId',
      );
      expect(r.status, 'discount → DISCOUNTED').toBe('DISCOUNTED');
      expect(r.posted, 'discount → posted=true').toBe(true);
      expect(r.discountId, 'discount should set discountId').toBeTruthy();
      discountId = r.discountId;

      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_DISCOUNTED NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: NET_AMOUNT, creditAmount: 0 },
        { subjectCode: '6603', dcDirection: 'DEBIT', debitAmount: DISCOUNT_INTEREST, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note, { discountId, fundAccountId: fund.id });
    }
  });

  test('endorse: RECEIVED → ENDORSED + voucher Dr 2202 / Cr 1121', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createNote(page, { status: 'RECEIVED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'endorse',
        // endorsementFromId 经 GraphQL @Name 标非空（Nop 默认），传 note 自身 id 作有效 FK 自引用
        // （Processor doEndorse 仅记录 endorsementFromId，不影响过账；语义为背书链路来源票据）。
        { notesId: note.id, endorsementFromId: note.id },
        'id status posted',
      );
      expect(r.status, 'endorse → ENDORSED').toBe('ENDORSED');
      expect(r.posted, 'endorse → posted=true').toBe(true);

      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_ENDORSED NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('collect→honor: RECEIVED → COLLECTION_PENDING(no post) → HONORED + voucher Dr 1002 / Cr 1121', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createNote(page, { status: 'RECEIVED' });
    try {
      const collected = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'collect',
        { notesId: note.id },
        'id status posted',
      );
      expect(collected.status, 'collect → COLLECTION_PENDING').toBe('COLLECTION_PENDING');
      expect(collected.posted, 'collect is intermediate → posted unchanged (false)').toBe(false);

      // collect 无过账：billCode 无 NORMAL 凭证
      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'collect should NOT produce a voucher').toBeNull();

      const honored = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'honor',
        { notesId: note.id },
        'id status posted',
      );
      expect(honored.status, 'honor → HONORED').toBe('HONORED');
      expect(honored.posted, 'honor → posted=true').toBe(true);

      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_RECEIVABLE_COLLECTION NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1121', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('dishonor: COLLECTION_PENDING → DISHONORED (terminal, no posting)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createNote(page, { status: 'COLLECTION_PENDING' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'dishonor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'dishonor → DISHONORED').toBe('DISHONORED');
      expect(r.posted, 'dishonor is terminal marker → no posting').toBe(false);

      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status');
      expect(v.status, '__get confirms DISHONORED').toBe('DISHONORED');

      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'dishonor should NOT produce a voucher').toBeNull();
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('writeOff: RECEIVED(posted) → WRITE_OFF + posted=false + REVERSAL voucher Dr -1121 / Cr -1122', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    const note = await createNote(page);
    try {
      // 先 receive 产 RECEIVED + posted=true（writeOff 须 posted==true 才红冲）
      await callMutationOk(
        page, 'ErpFinNotesReceivable', 'receive',
        { notesId: note.id },
        'id status posted',
      );
      const before = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(before.posted, 'precondition: receive sets posted=true').toBe(true);

      const r = await callMutationOk(
        page, 'ErpFinNotesReceivable', 'writeOff',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'writeOff → WRITE_OFF').toBe('WRITE_OFF');
      expect(r.posted, 'writeOff → posted=false (entity-level rollback marker)').toBe(false);

      const v = await verifyState(page, 'ErpFinNotesReceivable', note.id, 'status posted');
      expect(v.status, '__get confirms WRITE_OFF').toBe('WRITE_OFF');
      expect(v.posted, '__get confirms posted=false').toBe(false);

      // REVERSAL 凭证同向取负（businessTypeForStatus(RECEIVED) → NOTES_RECEIVABLE_RECEIVED 红冲）
      const reversalVid = await findVoucherIdByBillCode(page, note.code, 'REVERSAL');
      expect(reversalVid, 'writeOff should produce REVERSAL voucher').toBeTruthy();
      await assertVoucherLines(page, reversalVid, [
        { subjectCode: '1121', dcDirection: 'DEBIT', debitAmount: -FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1122', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('guards: discount from non-RECEIVED + honor from non-COLLECTION_PENDING both rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesReceivable-main');
    // 守卫 1：discount 非 RECEIVED（DISCOUNTED 态再 discount）
    const discounted = await createNote(page, { status: 'DISCOUNTED' });
    // 守卫 2：honor 非 COLLECTION_PENDING（RECEIVED 态直接 honor）
    const received = await createNote(page, { status: 'RECEIVED' });
    try {
      const rejDiscount = await callMutation(
        page, 'ErpFinNotesReceivable', 'discount',
        { notesId: discounted.id, discountDate: DISCOUNT_DATE, bankId: 1, discountRate: DISCOUNT_RATE },
        'id',
      );
      expect(rejDiscount.errors, 'discount from DISCOUNTED should be rejected').toBeTruthy();

      const rejHonor = await callMutation(
        page, 'ErpFinNotesReceivable', 'honor',
        { notesId: received.id },
        'id',
      );
      expect(rejHonor.errors, 'honor from RECEIVED should be rejected').toBeTruthy();

      // 状态不变
      const s1 = await verifyState(page, 'ErpFinNotesReceivable', discounted.id, 'status');
      expect(s1.status, 'discounted note status unchanged after guard').toBe('DISCOUNTED');
      const s2 = await verifyState(page, 'ErpFinNotesReceivable', received.id, 'status');
      expect(s2.status, 'received note status unchanged after guard').toBe('RECEIVED');
    } finally {
      await cleanupNote(page, discounted);
      await cleanupNote(page, received);
    }
  });
});
