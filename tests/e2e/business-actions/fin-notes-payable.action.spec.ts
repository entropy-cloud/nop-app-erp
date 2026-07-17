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
 * finance ErpFinNotesPayable 应付票据 DIRECT 业务动作生命周期浏览器层 E2E
 * （plan 2026-07-17-1430-1 Phase 3）。
 *
 * 验证 4 动作状态机（issue/honor/dishonor/writeOff）+ 2 业财过账凭证行精确数值断言
 * （NOTES_PAYABLE_ISSUED/HONORED）+ 非法迁移守卫 + 授信强一致校验守卫（BANK_ACCEPTANCE）。
 *
 * 权威凭证结构（NotesPayableAcctDocProvider:42-63，billHeadCode=note.code）：
 *   ISSUED:  Dr 2202(应付账款)=face / Cr 2203(应付票据)=face
 *   HONORED: Dr 2203(应付票据)=face / Cr 1002(银行存款)=face
 *   （NOTES_PAYABLE_ISSUED/HONORED 不在 ErpFinArApItemGenerator.resolveProfile → 不生成辅助账，
 *    故无 buildCode 溢出风险；仍统一紧凑码风格。）
 *
 * writeOff 红冲（ErpFinNotesPayableProcessor.doWriteOff:146-155）：posted==true 时经
 *   executor.reverse(note.code, NOTES_PAYABLE_ISSUED) 写 REVERSAL 凭证（同 billHeadCode），
 *   随后 setPosted(false)+清 postedAt/postedBy。REVERSAL 同向取负。
 *
 * 授信强一致校验（ErpFinNotesPayableProcessor.reserveCreditIfNeeded:104-109 + CreditFacilityBizModel:33-50）：
 *   仅当 isBankAcceptance(note) && credit-check-on-issue(默认 true) && creditFacilityId!=null 时触发；
 *   available < face → ERR_CREDIT_FACILITY_INSUFFICIENT。**主路径用 COMMERCIAL_ACCEPTANCE 短路跳过**
 *   （isBankAcceptance=false），无需建 CreditFacility；守卫负路径建 BANK_ACCEPTANCE + 不足额度 CreditFacility。
 *
 * 自包含 setup：__save 直置 status 入口（ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow，DIRECT 可达）。
 *
 * 种子引用：org id=2 / currency id=1（CNY）/ partner id=3（SUP-001，AP 方向）/ 科目 2202 id=5、2203 id=41、
 *   1002 id=2 经种子补齐（本 plan Phase 2）。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const PARTNER_ID = 3; // SUP-001（AP 方向 partner 维度，-provider ISSUED dr.setPartnerId）
const FACE_AMOUNT = 1000;
const ISSUE_DATE = '2026-07-01';
const DUE_DATE = '2026-07-31';

let _seq = 0;
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
    'ErpFinNotesPayable',
    {
      code: uniq('NP'),
      orgId: ORG_ID,
      // 主路径 COMMERCIAL_ACCEPTANCE → isBankAcceptance=false → 授信校验短路跳过（最简自包含 setup）
      notesType: 'COMMERCIAL_ACCEPTANCE',
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

async function cleanupNote(
  page: import('@playwright/test').Page,
  note: { id?: string; code?: string } | null,
  extra: { creditFacilityId?: string | number | null } = {},
): Promise<void> {
  if (!note) return;
  if (note.code) await cleanupVoucherByBillCode(page, note.code);
  if (note.id != null) await deleteById(page, 'ErpFinNotesPayable', note.id);
  if (extra.creditFacilityId != null) await deleteById(page, 'ErpFinCreditFacility', extra.creditFacilityId);
}

test.describe('finance ErpFinNotesPayable lifecycle + voucher-line assertions', () => {
  test('issue: → ISSUED + posted + voucher Dr 2202 / Cr 2203', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createNote(page);
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'issue → ISSUED').toBe('ISSUED');
      expect(r.posted, 'issue → posted=true').toBe(true);

      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(v.status, '__get confirms ISSUED').toBe('ISSUED');
      expect(v.posted, '__get confirms posted=true').toBe(true);

      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_PAYABLE_ISSUED NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('honor: ISSUED → HONORED + posted + voucher Dr 2203 / Cr 1002', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createNote(page, { status: 'ISSUED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'honor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'honor → HONORED').toBe('HONORED');
      expect(r.posted, 'honor → posted=true').toBe(true);

      // ISSUED 未过账（__save 直置），honor 产 HONORED NORMAL 凭证（唯一 billCode 凭证）
      const vid = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(vid, 'NOTES_PAYABLE_HONORED NORMAL voucher exists').toBeTruthy();
      await assertVoucherLines(page, vid, [
        { subjectCode: '2203', dcDirection: 'DEBIT', debitAmount: FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('dishonor: ISSUED → DISHONORED (no posting)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createNote(page, { status: 'ISSUED' });
    try {
      const r = await callMutationOk(
        page, 'ErpFinNotesPayable', 'dishonor',
        { notesId: note.id },
        'id status posted',
      );
      expect(r.status, 'dishonor → DISHONORED').toBe('DISHONORED');
      expect(r.posted, 'dishonor → no posting').toBe(false);

      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status');
      expect(v.status, '__get confirms DISHONORED').toBe('DISHONORED');

      const noVoucher = await findVoucherIdByBillCode(page, note.code, 'NORMAL');
      expect(noVoucher, 'dishonor should NOT produce a voucher').toBeNull();
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('writeOff: ISSUED(posted) → WRITE_OFF + posted=false + REVERSAL Dr -2202 / Cr -2203', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    const note = await createNote(page);
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
      expect(r.status, 'writeOff → WRITE_OFF').toBe('WRITE_OFF');
      expect(r.posted, 'writeOff → posted=false (entity-level rollback marker)').toBe(false);

      const v = await verifyState(page, 'ErpFinNotesPayable', note.id, 'status posted');
      expect(v.status, '__get confirms WRITE_OFF').toBe('WRITE_OFF');
      expect(v.posted, '__get confirms posted=false').toBe(false);

      // REVERSAL 凭证同向取负（doWriteOff 红冲 NOTES_PAYABLE_ISSUED）
      const reversalVid = await findVoucherIdByBillCode(page, note.code, 'REVERSAL');
      expect(reversalVid, 'writeOff should produce REVERSAL voucher').toBeTruthy();
      await assertVoucherLines(page, reversalVid, [
        { subjectCode: '2202', dcDirection: 'DEBIT', debitAmount: -FACE_AMOUNT, creditAmount: 0 },
        { subjectCode: '2203', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -FACE_AMOUNT },
      ]);
    } finally {
      await cleanupNote(page, note);
    }
  });

  test('guards: honor from non-ISSUED rejected + BANK_ACCEPTANCE credit-check insufficient rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinNotesPayable-main');
    // 守卫 1：honor 非 ISSUED（status=null 直接 honor）
    const blankNote = await createNote(page);
    // 守卫 2：BANK_ACCEPTANCE + 不足额度 CreditFacility → issue 抛 ERR_CREDIT_FACILITY_INSUFFICIENT
    const facility = await createViaSave(
      page,
      'ErpFinCreditFacility',
      {
        code: uniq('CF'),
        orgId: ORG_ID,
        facilityType: 'BANK_ACCEPTANCE_LINE',
        totalAmount: 100, // < face 1000
        usedAmount: 0,
        availableAmount: 100,
        status: 'ACTIVE',
      },
      'id',
    );
    const bankNote = await createNote(page, {
      notesType: 'BANK_ACCEPTANCE',
      creditFacilityId: facility.id,
    });
    try {
      const rejHonor = await callMutation(
        page, 'ErpFinNotesPayable', 'honor',
        { notesId: blankNote.id },
        'id',
      );
      expect(rejHonor.errors, 'honor from non-ISSUED should be rejected').toBeTruthy();

      const rejIssue = await callMutation(
        page, 'ErpFinNotesPayable', 'issue',
        { notesId: bankNote.id },
        'id',
      );
      expect(rejIssue.errors, 'BANK_ACCEPTANCE issue with insufficient credit should be rejected').toBeTruthy();

      // 状态不变（授信不足：reserveCredit 抛 ERR_CREDIT_FACILITY_INSUFFICIENT，事务回滚，issue 未推进）
      const s = await verifyState(page, 'ErpFinNotesPayable', bankNote.id, 'status posted');
      expect(s.status ?? null, 'bankNote status unchanged (null) after credit guard').toBeNull();
      expect(s.posted, 'bankNote posted=false after credit guard').toBe(false);
    } finally {
      await cleanupNote(page, blankNote);
      await cleanupNote(page, bankNote, { creditFacilityId: facility.id });
    }
  });
});
