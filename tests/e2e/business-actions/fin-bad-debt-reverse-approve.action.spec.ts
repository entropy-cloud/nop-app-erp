import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
  findFirst,
} from './_helper';
import { cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * Finance ErpFinBadDebt 坏账 reverseApprove 红冲闭环浏览器层 E2E（plan 2026-07-18-1745-3 Phase 3）。
 *
 * 验证 finance 域坏账 DIRECT `@BizMutation` `reverseApprove(id)` 经 GraphQL /graphql 的全栈可达性：
 *   (a) 核销路径反向：writeOff→submit→approve 产生 APPROVED + BAD_DEBT_WRITE_OFF 凭证 + ArApItem WRITTEN_OFF
 *       → reverseApprove → APPROVED→REJECTED + 原凭证 isReversed=true + 红字凭证行同向取负
 *       （Dr 1231=-100 / Cr 1122=-100）+ ArApItem 回退 OPEN + openAmount=100。
 *   (b) 收回路径反向：writeOff→approve（WRITTEN_OFF）→ recover→submit→approve（OPEN + BAD_DEBT_RECOVERY 凭证）
 *       → reverseApprove → REJECTED + 原凭证 isReversed=true + ArApItem 回退 WRITTEN_OFF + openAmount=0。
 *
 * 权威设计（docs/design/finance/bad-debt.md + plan 1745-3）：reverseApprove 经 DIRECT 路径
 * （ErpFinBadDebt 无 useWorkflow tagSet），调 FinPostingExecutor.reverse(debt.code, BAD_DEBT_WRITE_OFF|RECOVERY)
 * + 回退 ArApItem 状态对称（writeOff: WRITTEN_OFF→OPEN；recovery: OPEN→WRITTEN_OFF）。
 *
 * 自包含隔离（复用 0413-2 既有坏账 setup 范式）：新建 partner `E2E-BADDEBT-REVA-PN-` + OPEN RECEIVABLE ErpFinArApItem。
 * cleanup 删 partner + 凭证（按 debt.code）+ 坏账单 + AR-AP 项，使 finance 看板/报表基线无漂移。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period id=1（OPEN，endDate=2026-07-31）。
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
      code: uniq('E2E-BADDEBT-REVA-PN'),
      name: `E2E BadDebt Reverse Partner ${tag}`,
      partnerType: 'CUSTOMER',
      status: 'ACTIVE',
    },
    'id',
  );
}

async function createOpenArItem(
  page: import('@playwright/test').Page,
  partnerId: string | number,
  tag: string,
  amount = AMOUNT,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinArApItem',
    {
      code: uniq(`E2E-BADDEBT-REVA-AR-${tag}`),
      orgId: ORG,
      acctSchemaId: ACCT_SCHEMA,
      direction: 'RECEIVABLE',
      partnerId,
      sourceBillType: 'AR_INVOICE',
      sourceBillCode: uniq(`E2E-BADDEBT-REVA-INV-${tag}`),
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
  arItemId?: string | number | null;
  debtIds?: Array<string | number>;
  debtCodes?: string[];
}

async function cleanupBadDebt(page: import('@playwright/test').Page, ctx: CleanupCtx): Promise<void> {
  for (const code of ctx.debtCodes ?? []) {
    await cleanupVoucherByBillCode(page, code);
  }
  for (const id of ctx.debtIds ?? []) {
    await deleteById(page, 'ErpFinBadDebt', id);
  }
  if (ctx.arItemId != null) {
    await deleteById(page, 'ErpFinArApItem', ctx.arItemId);
  }
  if (ctx.partnerId != null) {
    await deleteById(page, 'ErpMdPartner', ctx.partnerId);
  }
}

test.describe('Finance ErpFinBadDebt reverseApprove browser-layer E2E (red reversal closure)', () => {
  test('(a) writeOff approve → reverseApprove: APPROVED→REJECTED + BAD_DEBT_WRITE_OFF voucher reversed + ArApItem OPEN restored', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    const partner = await createPartner(page, 'wo-rev');
    const item = await createOpenArItem(page, partner.id, 'WO-REV');
    const ctx: CleanupCtx = { partnerId: partner.id, arItemId: item.id, debtIds: [], debtCodes: [] };

    try {
      // 前置：writeOff → submit → approve（ArApItem WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证）
      const writtenOff = await callMutationOk(
        page, 'ErpFinBadDebt', 'writeOff',
        { arApItemId: item.id, reason: 'E2E setup for reverse' },
        'id code docType approvalStatus voucherId',
      );
      expect(writtenOff.approvalStatus, 'precondition: UNSUBMITTED after writeOff').toBe('UNSUBMITTED');
      ctx.debtIds!.push(writtenOff.id);
      ctx.debtCodes!.push(writtenOff.code);

      await callMutationOk(page, 'ErpFinBadDebt', 'submit', { id: writtenOff.id }, 'id approvalStatus');
      await callMutationOk(page, 'ErpFinBadDebt', 'approve', { id: writtenOff.id }, 'id approvalStatus');

      const beforeReverse = await verifyState(page, 'ErpFinBadDebt', writtenOff.id, 'approvalStatus');
      expect(beforeReverse.approvalStatus, 'precondition: APPROVED after approve').toBe('APPROVED');

      const itemWo = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemWo.status, 'precondition: ArApItem WRITTEN_OFF').toBe('WRITTEN_OFF');

      // 反审核
      const reversed = await callMutationOk(
        page, 'ErpFinBadDebt', 'reverseApprove', { id: writtenOff.id },
        'id approvalStatus',
      );
      expect(reversed.approvalStatus, 'reverseApprove → REJECTED').toBe('REJECTED');

      // 经 __get 独立断言
      const finalState = await verifyState(page, 'ErpFinBadDebt', writtenOff.id, 'approvalStatus');
      expect(finalState.approvalStatus, 'verifyState REJECTED').toBe('REJECTED');

      // ArApItem 回退 OPEN + openAmount=100
      const itemAfter = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemAfter.status, 'ArApItem 回退 OPEN').toBe('OPEN');
      expect(Number(itemAfter.openAmountFunctional), 'openAmount 恢复 100').toBe(AMOUNT);

      // 原凭证 isReversed=true
      const originalVoucherId = await findVoucherIdByBillCode(page, writtenOff.code, 'NORMAL');
      expect(originalVoucherId, '原 BAD_DEBT_WRITE_OFF 凭证应存在').toBeTruthy();
      const originalVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id isReversed',
      );
      expect(originalVoucher?.isReversed, '原凭证 isReversed=true').toBe(true);

      // 红字凭证存在 + 同向取负（Dr 1231=-100 / Cr 1122=-100）
      const reversalVoucherId = await findVoucherIdByBillCode(page, writtenOff.code, 'REVERSAL');
      expect(reversalVoucherId, '红字凭证应生成').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '1231', dcDirection: 'DEBIT', debitAmount: -AMOUNT, creditAmount: 0 },
        { subjectCode: '1122', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -AMOUNT },
      ]);
    } finally {
      await cleanupBadDebt(page, ctx);
    }
  });

  test('(b) recover approve → reverseApprove: RECOVERY voucher reversed + ArApItem WRITTEN_OFF restored', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    const partner = await createPartner(page, 'rc-rev');
    const item = await createOpenArItem(page, partner.id, 'RC-REV');
    const ctx: CleanupCtx = { partnerId: partner.id, arItemId: item.id, debtIds: [], debtCodes: [] };

    try {
      // 前置：writeOff→approve 建 WRITTEN_OFF（recover 前置态）
      const writtenOff = await callMutationOk(
        page, 'ErpFinBadDebt', 'writeOff',
        { arApItemId: item.id, reason: 'E2E setup write-off before recover reverse' },
        'id code approvalStatus',
      );
      ctx.debtIds!.push(writtenOff.id);
      ctx.debtCodes!.push(writtenOff.code);
      await callMutationOk(page, 'ErpFinBadDebt', 'submit', { id: writtenOff.id }, 'id approvalStatus');
      await callMutationOk(page, 'ErpFinBadDebt', 'approve', { id: writtenOff.id }, 'id approvalStatus');

      // recover → submit → approve（ArApItem OPEN + BAD_DEBT_RECOVERY 凭证）
      const recovered = await callMutationOk(
        page, 'ErpFinBadDebt', 'recover',
        { arApItemId: item.id, reason: 'E2E recover before reverse' },
        'id code docType approvalStatus',
      );
      expect(recovered.docType, 'precondition: docType=RECOVERY').toBe('RECOVERY');
      ctx.debtIds!.push(recovered.id);
      ctx.debtCodes!.push(recovered.code);

      await callMutationOk(page, 'ErpFinBadDebt', 'submit', { id: recovered.id }, 'id approvalStatus');
      await callMutationOk(page, 'ErpFinBadDebt', 'approve', { id: recovered.id }, 'id approvalStatus');

      const beforeReverse = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(beforeReverse.status, 'precondition: ArApItem OPEN after recover').toBe('OPEN');
      expect(Number(beforeReverse.openAmountFunctional), 'precondition: openAmount=100').toBe(AMOUNT);

      // 反审核 recovery
      const reversed = await callMutationOk(
        page, 'ErpFinBadDebt', 'reverseApprove', { id: recovered.id },
        'id approvalStatus',
      );
      expect(reversed.approvalStatus, 'recovery reverseApprove → REJECTED').toBe('REJECTED');

      // ArApItem 回退 WRITTEN_OFF + openAmount=0（recovery 反向）
      const itemAfter = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemAfter.status, 'ArApItem 回退 WRITTEN_OFF（recovery 反向）').toBe('WRITTEN_OFF');
      expect(Number(itemAfter.openAmountFunctional), 'openAmount 回退 0').toBe(0);

      // 原 RECOVERY 凭证 isReversed=true
      const originalVoucherId = await findVoucherIdByBillCode(page, recovered.code, 'NORMAL');
      expect(originalVoucherId, '原 BAD_DEBT_RECOVERY 凭证应存在').toBeTruthy();
      const originalVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id isReversed',
      );
      expect(originalVoucher?.isReversed, '原 RECOVERY 凭证 isReversed=true').toBe(true);

      // 红字凭证行同向取负（Dr 1122=-100 / Cr 1231=-100）
      const reversalVoucherId = await findVoucherIdByBillCode(page, recovered.code, 'REVERSAL');
      expect(reversalVoucherId, 'recovery 红字凭证应生成').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '1122', dcDirection: 'DEBIT', debitAmount: -AMOUNT, creditAmount: 0 },
        { subjectCode: '1231', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -AMOUNT },
      ]);
    } finally {
      await cleanupBadDebt(page, ctx);
    }
  });
});
