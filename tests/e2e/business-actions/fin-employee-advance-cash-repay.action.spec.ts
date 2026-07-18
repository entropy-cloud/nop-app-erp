import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteById,
} from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * Finance ErpFinEmployeeAdvance 员工借款现金还款（EMPLOYEE_ADVANCE_SETTLE 现金还款路径）浏览器层 E2E
 * （plan 2026-07-18-0718-2 Phase 3）。
 *
 * 验证 `ErpFinEmployeeAdvanceBizModel.cashRepay(advanceId, amount)` 经 GraphQL `/graphql` 的全栈可达性 +
 * advance 字段翻转（settledAmount/outstandingAmount）+ docStatus 保持不变（派生投影对齐 owner doc）+
 * EMPLOYEE_ADVANCE_SETTLE 现金还款凭证行精确数值（Dr 1002 银行存款 / Cr 1221 其他应收款-员工预支）。
 *
 * 权威设计（docs/design/finance/expense-claim.md §现金还款）：
 *   - 现金还款用 ErpFinVoucher 凭证承载（不建独立还款实体，参 frappe/hrms make_return_entry）
 *   - Dr 1002 银行存款（或库存现金）returnAmount / Cr 1221 其他应收款-员工预支 returnAmount
 *   - docStatus 保持不变（outstandingAmount=0 由查询/UI 派生表达「已结清」，非字典推进）
 *
 * cashRepay billHeadCode = `EA-CASH-REPAY-<advanceCode>-<millis>`（含时间戳避免多次还款碰撞）。
 * 凭证反查经 ErpFinVoucherBillR.businessType=EMPLOYEE_ADVANCE_SETTLE + billCode LIKE 前缀匹配。
 *
 * 自包含 setup：每用例独立建 partner(EMPLOYEE) + employee(partnerId 非空) + EmployeeAdvance(amountFunctional=500)
 *   → submitForApproval → approve（posted=true + EMPLOYEE_ADVANCE 凭证 + receivable 辅助账）
 *   → cashRepay(amount) → 断言字段翻转 + 现金还款凭证
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
  cashRepayBillCodes?: string[];
}

async function setupEmployee(page: Page): Promise<Ctx> {
  const ctx: Ctx = { cashRepayBillCodes: [] };
  const partnerCode = uniq('EP');
  const partner = await createViaSave(page, 'ErpMdPartner', {
    code: partnerCode, name: `E2E Cash Repay Partner ${partnerCode}`,
    partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
  }, 'id');
  ctx.partnerId = partner.id;
  const empCode = uniq('EE');
  const employee = await createViaSave(page, 'ErpMdEmployee', {
    code: empCode, name: `E2E Cash Repay Employee ${empCode}`,
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
  // submit → approve（触发 EMPLOYEE_ADVANCE 过账：posted=true + receivable 辅助账 + Dr 1221/Cr 1002 凭证）
  await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
  await callMutationOk(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id posted');
}

/**
 * 经 ErpFinVoucherBillR 反查现金还款凭证 id（businessType=EMPLOYEE_ADVANCE_SETTLE + billCode 前缀匹配）。
 * billHeadCode 格式 `EA-CASH-REPAY-<advanceCode>-<millis>`，按 advanceCode 前缀在 JS 层过滤
 *（Nop GraphQL Map filter 的 like 操作符经实测被当作 eq 处理，故改用 eq+JS 过滤范式）。
 */
async function findCashRepayVoucherId(page: Page, advanceCode: string): Promise<number | null> {
  const prefix = `EA-CASH-REPAY-${advanceCode}-`;
  const links = await findItems<any>(
    page, 'ErpFinVoucherBillR',
    eqFilter('businessType', 'EMPLOYEE_ADVANCE_SETTLE'),
    'billCode voucherId',
  );
  const match = links.find((l) => l.billCode && l.billCode.startsWith(prefix));
  return match ? Number(match.voucherId) : null;
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
        const { deleteByFilter } = await import('./_helper');
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

test.describe('Finance ErpFinEmployeeAdvance cashRepay browser-layer E2E (EMPLOYEE_ADVANCE_SETTLE CASH path)', () => {
  test('(a) full cash repay: settledAmount=500 + outstandingAmount=0 + docStatus unchanged + Dr 1002/Cr 1221 voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupPostedAdvance(page, ctx, ADVANCE_AMOUNT);

      const repaid = await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'cashRepay',
        { advanceId: ctx.advanceId, amount: ADVANCE_AMOUNT },
        'id settledAmount outstandingAmount approveStatus docStatus',
      );
      expect(Number(repaid.settledAmount), 'cashRepay 后 settledAmount = 500').toBe(ADVANCE_AMOUNT);
      expect(Number(repaid.outstandingAmount), 'cashRepay 后 outstandingAmount = 0（已结清派生投影）').toBe(0);

      // 经 __get 独立断言字段翻转（非仅 mutation 返回值）
      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount approveStatus docStatus',
      );
      expect(Number(advanceState.settledAmount), 'verifyState settledAmount = 500').toBe(ADVANCE_AMOUNT);
      expect(Number(advanceState.outstandingAmount), 'verifyState outstandingAmount = 0').toBe(0);
      expect(advanceState.approveStatus, 'approveStatus 保持 APPROVED').toBe('APPROVED');
      expect(advanceState.docStatus, 'docStatus 保持不变（派生投影，非字典推进）').not.toBe('CANCELLED');

      // 反查现金还款凭证 + 凭证行精确数值
      const voucherId = await findCashRepayVoucherId(page, ctx.advanceCode!);
      expect(voucherId, 'EMPLOYEE_ADVANCE_SETTLE CASH 凭证应存在').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: ADVANCE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1221', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: ADVANCE_AMOUNT },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) partial cash repay: settledAmount=200 + outstandingAmount=300 + docStatus unchanged', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupPostedAdvance(page, ctx, ADVANCE_AMOUNT);

      const PARTIAL = 200;
      const repaid = await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'cashRepay',
        { advanceId: ctx.advanceId, amount: PARTIAL },
        'id settledAmount outstandingAmount',
      );
      expect(Number(repaid.settledAmount), '部分还款 settledAmount = 200').toBe(PARTIAL);
      expect(Number(repaid.outstandingAmount), '部分还款 outstandingAmount = 300').toBe(ADVANCE_AMOUNT - PARTIAL);

      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount approveStatus',
      );
      expect(Number(advanceState.settledAmount), 'verifyState settledAmount = 200').toBe(PARTIAL);
      expect(Number(advanceState.outstandingAmount), 'verifyState outstandingAmount = 300').toBe(ADVANCE_AMOUNT - PARTIAL);
      expect(advanceState.approveStatus, 'approveStatus 保持 APPROVED').toBe('APPROVED');

      const voucherId = await findCashRepayVoucherId(page, ctx.advanceCode!);
      expect(voucherId, '部分还款凭证应存在').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1002', dcDirection: 'DEBIT', debitAmount: PARTIAL, creditAmount: 0 },
        { subjectCode: '1221', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: PARTIAL },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(c) exceeds outstanding guard: cashRepay(500) on outstandingAmount=300 rejected with exceeds error', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupPostedAdvance(page, ctx, ADVANCE_AMOUNT);
      // 先部分还款 200 使 outstandingAmount=300
      await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'cashRepay',
        { advanceId: ctx.advanceId, amount: 200 },
        'id',
      );

      // 再尝试还款 500（超过 outstanding=300）→ 应被守卫拒绝
      const OVER = 500;
      const rej = await callMutation(
        page, 'ErpFinEmployeeAdvance', 'cashRepay',
        { advanceId: ctx.advanceId, amount: OVER },
        'id',
      );
      expect(rej.errors, '超额还款应被守卫拒绝').toBeTruthy();
      expect(JSON.stringify(rej.errors), '错误消息含「超过」或「未还余额」').toMatch(/超过|未还余额/);

      // 字段不变（settledAmount=200, outstandingAmount=300）
      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount',
      );
      expect(Number(advanceState.settledAmount), '超额拒绝后 settledAmount 保持 200').toBe(200);
      expect(Number(advanceState.outstandingAmount), '超额拒绝后 outstandingAmount 保持 300').toBe(300);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
