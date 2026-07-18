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
} from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
  findFirst,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * Finance ErpFinEmployeeAdvance reverseCashRepay 反向现金还款红冲闭环浏览器层 E2E
 * （plan 2026-07-18-1745-3 Phase 3）。
 *
 * 验证 `ErpFinEmployeeAdvanceBizModel.reverseCashRepay(advanceId)` 经 GraphQL /graphql 的全栈可达性 +
 * 红冲 EMPLOYEE_ADVANCE_SETTLE(CASH) 凭证（原凭证 isReversed=true + 红字凭证行同向取负 Dr 1002=-X / Cr 1221=-X）
 * + advance 字段回退（settledAmount-=amount / outstandingAmount+=amount）。
 *
 * 权威设计（docs/design/finance/expense-claim.md §红冲联动 l.196，plan 1745-3 兑现 owner-doc 漂移）：
 *   报销单/借款单 CANCELLED 时按业财回链红冲已过账凭证——cashRepay 路径以前无反向入口，
 *   reverseCashRepay 接线既有 EmployeeAdvancePostingDispatcher.reverseSettle 兑现该承诺。
 *
 * reverseCashRepay 经 ErpFinVoucherBillR 反查 billCode 前缀 `EA-CASH-REPAY-<advanceCode>-` +
 * businessType=EMPLOYEE_ADVANCE_SETTLE + voucher.isReversed=false 取最新一笔，调 reverseSettle 红冲。
 *
 * 自包含 setup（复用 0718-2 既有 cashRepay setup 范式）：每用例独立建 partner(EMPLOYEE) +
 * employee(partnerId 非空) + EmployeeAdvance(amountFunctional=500) → submitForApproval → approve
 * （posted=true + EMPLOYEE_ADVANCE 凭证 + receivable 辅助账）→ cashRepay(amount) → reverseCashRepay
 * cleanup：删 EA-CASH-REPAY 凭证（prefix）+ advance 相关凭证/辅助账 + advance + employee + partner。
 *
 * 种子引用（对齐 1218-2 范式）：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07（OPEN）。
 * 科目引用：1221 其他应收款-员工预支 / 1002 银行存款（均种子已就绪）。
 */
const ORG = 2;
const CURRENCY = 1;
const BDATE = '2026-07-15';
const ADVANCE_AMOUNT = 500;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now()}${_seq}`;
}

interface Ctx {
  partnerId?: string | number;
  employeeId?: string | number;
  advanceId?: string | number;
  advanceCode?: string;
}

async function setupEmployee(page: Page): Promise<Ctx> {
  const ctx: Ctx = {};
  const partnerCode = uniq('EP');
  const partner = await createViaSave(page, 'ErpMdPartner', {
    code: partnerCode, name: `E2E Reverse Cash Repay Partner ${partnerCode}`,
    partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
  }, 'id');
  ctx.partnerId = partner.id;
  const empCode = uniq('EE');
  const employee = await createViaSave(page, 'ErpMdEmployee', {
    code: empCode, name: `E2E Reverse Cash Repay Employee ${empCode}`,
    orgId: ORG, status: 'ACTIVE', partnerId: partner.id,
  }, 'id');
  ctx.employeeId = employee.id;
  return ctx;
}

async function setupPostedAdvance(page: Page, ctx: Ctx, amount: number): Promise<void> {
  const code = uniq('EA');
  const advance = await createViaSave(page, 'ErpFinEmployeeAdvance', {
    code, orgId: ORG, employeeId: ctx.employeeId, advanceType: 'EXPENSE_ADVANCE',
    businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
    amountSource: amount, amountFunctional: amount,
    settledAmount: 0, outstandingAmount: amount,
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
  }, 'id code');
  ctx.advanceId = advance.id;
  ctx.advanceCode = code;
  // submit → approve（触发 EMPLOYEE_ADVANCE 过账：posted=true + receivable 辅助账）
  await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
  await callMutationOk(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id posted');
}

/**
 * 经 ErpFinVoucherBillR 反查现金还款凭证 id（按 advanceCode 前缀 + postingType 区分原/红字）。
 */
async function findCashRepayVoucherId(
  page: Page,
  advanceCode: string,
  postingType: 'NORMAL' | 'REVERSAL',
): Promise<number | null> {
  const prefix = `EA-CASH-REPAY-${advanceCode}-`;
  const links = await findItems<any>(
    page, 'ErpFinVoucherBillR',
    eqFilter('businessType', 'EMPLOYEE_ADVANCE_SETTLE'),
    'billCode voucherId',
  );
  for (const lnk of links) {
    if (!lnk.billCode || !lnk.billCode.startsWith(prefix)) continue;
    const v = await findFirst<any>(
      page, 'ErpFinVoucher', eqFilter('id', Number(lnk.voucherId)), 'id postingType',
    );
    if (v && v.postingType === postingType) {
      return Number(v.id);
    }
  }
  return null;
}

async function cleanupCtx(page: Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  // 删 cashRepay 凭证（prefix 匹配所有 EA-CASH-REPAY-{code}-* 凭证）
  if (ctx.advanceCode) {
    const prefix = `EA-CASH-REPAY-${ctx.advanceCode}-`;
    const cashLinks = await findItems<any>(
      page, 'ErpFinVoucherBillR',
      eqFilter('businessType', 'EMPLOYEE_ADVANCE_SETTLE'),
      'id billCode voucherId',
    );
    const matching = cashLinks.filter((l) => l.billCode && l.billCode.startsWith(prefix));
    for (const lnk of matching) {
      const voucherId = Number(lnk.voucherId);
      if (voucherId) {
        await deleteByFilter(page, 'ErpFinVoucherLine', eqFilter('voucherId', voucherId));
        await deleteById(page, 'ErpFinVoucher', voucherId);
      }
      if (lnk.id != null) {
        await deleteById(page, 'ErpFinVoucherBillR', lnk.id);
      }
    }
    // 删 advance 相关凭证（EMPLOYEE_ADVANCE + EMPLOYEE_ADVANCE_SETTLE offset if any）+ 辅助账
    await cleanupArApByCode(page, ctx.advanceCode);
    await cleanupVoucherByBillCode(page, ctx.advanceCode);
  }
  if (ctx.advanceId) await deleteById(page, 'ErpFinEmployeeAdvance', ctx.advanceId);
  if (ctx.employeeId) await deleteById(page, 'ErpMdEmployee', ctx.employeeId);
  if (ctx.partnerId) await deleteById(page, 'ErpMdPartner', ctx.partnerId);
}

test.describe('Finance ErpFinEmployeeAdvance reverseCashRepay browser-layer E2E (red reversal closure)', () => {
  test('(a) cashRepay(500) → reverseCashRepay: voucher reversed + settled=0/outstanding=500 restored', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupPostedAdvance(page, ctx, ADVANCE_AMOUNT);
      await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'cashRepay',
        { advanceId: ctx.advanceId, amount: ADVANCE_AMOUNT },
        'id settledAmount outstandingAmount',
      );

      // 反向现金还款
      const reversed = await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'reverseCashRepay',
        { advanceId: ctx.advanceId },
        'id settledAmount outstandingAmount approveStatus',
      );
      expect(Number(reversed.settledAmount), 'reverseCashRepay 后 settled=0').toBe(0);
      expect(Number(reversed.outstandingAmount), 'reverseCashRepay 后 outstanding=500').toBe(ADVANCE_AMOUNT);

      // 经 __get 独立断言字段回退
      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount approveStatus',
      );
      expect(Number(advanceState.settledAmount), 'verifyState settled=0').toBe(0);
      expect(Number(advanceState.outstandingAmount), 'verifyState outstanding=500').toBe(ADVANCE_AMOUNT);
      expect(advanceState.approveStatus, 'approveStatus 保持 APPROVED').toBe('APPROVED');

      // 原 cashRepay 凭证 isReversed=true
      const originalVoucherId = await findCashRepayVoucherId(page, ctx.advanceCode!, 'NORMAL');
      expect(originalVoucherId, '原 cashRepay NORMAL 凭证应存在').toBeTruthy();
      const originalVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id isReversed',
      );
      expect(originalVoucher?.isReversed, '原 cashRepay 凭证 isReversed=true').toBe(true);

      // 红字凭证存在 + 同向取负（Dr 1002=-500 / Cr 1221=-500）
      const reversalVoucherId = await findCashRepayVoucherId(page, ctx.advanceCode!, 'REVERSAL');
      expect(reversalVoucherId, '红字凭证应生成').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: -ADVANCE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1221', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -ADVANCE_AMOUNT },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) no cashRepay voucher: reverseCashRepay rejected with not-found guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupPostedAdvance(page, ctx, ADVANCE_AMOUNT);
      // 已过账 advance 但无 cashRepay 凭证 → 守卫拒绝
      const rej = await callMutation(
        page, 'ErpFinEmployeeAdvance', 'reverseCashRepay',
        { advanceId: ctx.advanceId },
        'id',
      );
      expect(rej.errors, '无 cashRepay 凭证应被守卫拒绝').toBeTruthy();
      expect(JSON.stringify(rej.errors), '错误消息含「未找到」/「现金还款」语义').toMatch(/未找到|现金还款/);

      // advance 字段不变
      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount',
      );
      expect(Number(advanceState.settledAmount), 'settled 保持 0').toBe(0);
      expect(Number(advanceState.outstandingAmount), 'outstanding 保持 500').toBe(ADVANCE_AMOUNT);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
