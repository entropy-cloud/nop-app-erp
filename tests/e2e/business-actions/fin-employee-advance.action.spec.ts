import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
} from '../orchestration/_helper';

/**
 * Finance ErpFinEmployeeAdvance 员工借款生命周期 + 凭证行数值断言 + 报销抵扣联动浏览器层 E2E
 * （plan 2026-07-14-1218-2 Phase 3）。
 *
 * 验证 finance 域员工借款 DIRECT useApproval 审批轴经 GraphQL `/graphql` 的全栈可达性 +
 * EMPLOYEE_ADVANCE 业财过账凭证行精确数值断言 + 报销抵扣借款联动（EMPLOYEE_ADVANCE_SETTLE 凭证）：
 *   (a) 借款审核路径：submit→approve→posted=true + Dr 1221/Cr 1002 凭证行断言
 *   (b) reverseApprove 红冲：APPROVED→REJECTED + posted=false + 红字凭证同向取负 + 原凭证 isReversed=true
 *   (c) 报销抵扣联动：先 approve EmployeeAdvance(500) → 再 approve ExpenseClaim(226, OWN_ACCOUNT)
 *       → 断言三张凭证：EMPLOYEE_ADVANCE + EXPENSE_CLAIM + EMPLOYEE_ADVANCE_SETTLE
 *       + advance.settledAmount 增量=226
 *   (d) 非法迁移守卫：UNSUBMITTED→approve / APPROVED→submitForApproval 拒绝
 *
 * 权威设计（docs/design/finance/expense-claim.md §业财过账 §借款清算三路径 §关键业务规则1）：
 *   - EMPLOYEE_ADVANCE 凭证：Dr 1221 其他应收款-员工预支(amount) / Cr 1002 银行存款(amount)
 *   - 报销抵扣（advance-auto-offset-on-expense 默认 true）：ExpenseClaim approve 成功后，
 *     AdvanceOffsetOrchestrator.offset 自动抵扣同员工未还借款：
 *     net = min(payableOpen=claim.amountWithTax, receivableOpen=advance.amountFunctional)
 *     → EMPLOYEE_ADVANCE_SETTLE 凭证 Dr 2241 应付-员工(net) / Cr 1221 应收-员工预支(net)
 *     + advance.settledAmount += net + claim.settleAdvanceId = advance.id
 *   - SETTLE 凭证 billHeadCode = claim.code（非 advance.code）
 *
 * Explore Decision（Phase 3 抵扣联动裁定）：
 *   - AdvanceOffsetOrchestrator.offset 在 ExpenseClaim approve-post 成功后自动调用（浏览器层经 approve mutation 可达）
 *   - 抵扣前提：同 partnerId 下有 OPEN receivable AR/AP item（sourceBillType=EMPLOYEE_ADVANCE）
 *   - net = min(226, 500) = 226；SETTLE 凭证 Dr 2241=226/Cr 1221=226
 *   - 三张凭证共用两个 billHeadCode：advance.code(EMPLOYEE_ADVANCE) + claim.code(EXPENSE_CLAIM + SETTLE)
 *     → 须按 ErpFinVoucherBillR.billType 区分 EXPENSE_CLAIM 与 EMPLOYEE_ADVANCE_SETTLE（同 billCode 同 postingType=NORMAL）
 *   - cashRepay 方法不存在（Explore 核实），现金还款路径归 Deferred（已在计划 Non-Goals 记录）
 *
 * 科目引用：1221 其他应收款-员工预支(id=39,ASSET,DEBIT,本计划补齐) / 1002 银行存款(id=2,ASSET,DEBIT)
 *           / 6602 折旧费用(id=31) / 2221 应交税费(id=12) / 2241 其他应付款-员工(id=38,本计划补齐)。
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN）。
 * 自包含隔离：每用例独立建 partner+employee+advance(+claim+line)，cleanup 删凭证+AR-AP+行+头+员工+partner。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const SUBJECT_EXPENSE_ID = 31;
const SUBJECT_EXPENSE_CODE = '6602';
const BDATE = '2026-07-15';
const ADVANCE_AMOUNT = 500;
const CLAIM_AMOUNT_WITHOUT_TAX = 200;
const CLAIM_TAX = 26;
const CLAIM_WITH_TAX = 226;

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
  claimId?: string | number;
  claimCode?: string;
  lineId?: string | number;
}

async function setupEmployee(page: import('@playwright/test').Page): Promise<Ctx> {
  const ctx: Ctx = {};
  const partnerCode = uniq('EP');
  const partner = await createViaSave(page, 'ErpMdPartner', {
    code: partnerCode, name: `E2E Adv Partner ${partnerCode}`,
    partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
  }, 'id');
  ctx.partnerId = partner.id;
  const empCode = uniq('EE');
  const employee = await createViaSave(page, 'ErpMdEmployee', {
    code: empCode, name: `E2E Adv Employee ${empCode}`,
    orgId: ORG, status: 'ACTIVE', partnerId: partner.id,
  }, 'id');
  ctx.employeeId = employee.id;
  return ctx;
}

async function setupAdvance(page: import('@playwright/test').Page, ctx: Ctx, amount: number): Promise<void> {
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
}

async function setupClaim(
  page: import('@playwright/test').Page,
  ctx: Ctx,
  amountWithoutTax: number,
  taxAmount: number,
  amountWithTax: number,
): Promise<void> {
  const code = uniq('EC');
  const claim = await createViaSave(page, 'ErpFinExpenseClaim', {
    code, orgId: ORG, claimantId: ctx.employeeId, departmentId: ORG,
    businessDate: BDATE, paymentMode: 'OWN_ACCOUNT',
    currencyId: CURRENCY, exchangeRate: 1,
    amountSource: amountWithTax, amountFunctional: amountWithoutTax,
    amountWithoutTax, taxAmount, amountWithTax,
    docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    reason: `E2E advance offset test ${code}`,
  }, 'id code');
  ctx.claimId = claim.id;
  ctx.claimCode = code;
  const line = await createViaSave(page, 'ErpFinExpenseClaimLine', {
    claimId: claim.id, lineNo: 1, expenseType: 'OFFICE',
    subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
    amountWithoutTax, taxRate: 13, taxAmount, amountWithTax,
  }, 'id');
  ctx.lineId = line.id;
}

/** 经 ErpFinVoucherBillR(billCode + billType) 反查凭证 id（同 billCode 多业务类型区分）。 */
async function findVoucherIdByBillCodeAndType(
  page: import('@playwright/test').Page,
  billCode: string,
  billType: string,
): Promise<number | null> {
  const links = await findItems<any>(
    page, 'ErpFinVoucherBillR',
    andFilter(eqFilter('billCode', billCode), eqFilter('billType', billType)),
    'voucherId',
  );
  return links.length > 0 ? Number(links[0].voucherId) : null;
}

async function cleanupCtx(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  if (ctx.claimCode) {
    await cleanupArApByCode(page, ctx.claimCode);
    await cleanupVoucherByBillCode(page, ctx.claimCode);
  }
  if (ctx.lineId) await deleteById(page, 'ErpFinExpenseClaimLine', ctx.lineId);
  if (ctx.claimId) await deleteById(page, 'ErpFinExpenseClaim', ctx.claimId);
  if (ctx.advanceCode) {
    await cleanupArApByCode(page, ctx.advanceCode);
    await cleanupVoucherByBillCode(page, ctx.advanceCode);
  }
  if (ctx.advanceId) await deleteById(page, 'ErpFinEmployeeAdvance', ctx.advanceId);
  if (ctx.employeeId) await deleteById(page, 'ErpMdEmployee', ctx.employeeId);
  if (ctx.partnerId) await deleteById(page, 'ErpMdPartner', ctx.partnerId);
}

test.describe('Finance ErpFinEmployeeAdvance lifecycle + voucher lines + offset linkage browser-layer E2E', () => {
  test('(a) advance approve: submit→approve→posted + Dr 1221/Cr 1002 voucher lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupAdvance(page, ctx, ADVANCE_AMOUNT);

      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id approveStatus');
      const afterSubmit = await verifyState(page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'approveStatus');
      expect(afterSubmit.approveStatus, 'submit should transition approveStatus → SUBMITTED').toBe('SUBMITTED');

      const approved = await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id approveStatus posted',
      );
      expect(approved.approveStatus, 'approve should transition approveStatus → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should set posted=true').toBe(true);

      const voucherId = await findVoucherIdByBillCode(page, ctx.advanceCode!, 'NORMAL');
      expect(voucherId, 'EMPLOYEE_ADVANCE NORMAL voucher should exist by advance.code').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1221', dcDirection: 'DEBIT', debitAmount: ADVANCE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: ADVANCE_AMOUNT },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) reverseApprove: APPROVED→REJECTED + posted=false + reversal voucher lines negated + original isReversed', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupAdvance(page, ctx, ADVANCE_AMOUNT);
      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id posted');

      const originalVoucherId = await findVoucherIdByBillCode(page, ctx.advanceCode!, 'NORMAL');
      expect(originalVoucherId, 'original NORMAL voucher should exist before reverseApprove').toBeTruthy();

      const reversed = await callMutationOk(
        page, 'ErpFinEmployeeAdvance', 'reverseApprove', { id: ctx.advanceId }, 'id approveStatus posted',
      );
      expect(reversed.approveStatus, 'reverseApprove should transition approveStatus → REJECTED').toBe('REJECTED');
      expect(reversed.posted, 'reverseApprove should set posted=false').toBe(false);

      const originalVoucher = await verifyState(page, 'ErpFinVoucher', originalVoucherId!, 'id isReversed');
      expect(originalVoucher.isReversed, 'original NORMAL voucher should be marked isReversed=true').toBe(true);

      const reversalVoucherId = await findVoucherIdByBillCode(page, ctx.advanceCode!, 'REVERSAL');
      expect(reversalVoucherId, 'REVERSAL voucher should exist by advance.code').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '1221', dcDirection: 'DEBIT', debitAmount: -ADVANCE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -ADVANCE_AMOUNT },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(c) offset linkage: approve advance(500) + approve claim(226) → 3 vouchers + advance.settledAmount=226', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupAdvance(page, ctx, ADVANCE_AMOUNT);
      await setupClaim(page, ctx, CLAIM_AMOUNT_WITHOUT_TAX, CLAIM_TAX, CLAIM_WITH_TAX);

      // 先 approve EmployeeAdvance → creates receivable AR/AP item (openAmount=500)
      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id posted');

      // 再 approve ExpenseClaim → creates payable AR/AP item (openAmount=226) + auto-offset
      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id');
      const claimApproved = await callMutationOk(
        page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id approveStatus posted settleAdvanceId',
      );
      expect(claimApproved.posted, 'claim should be posted=true').toBe(true);
      expect(claimApproved.settleAdvanceId, 'claim.settleAdvanceId should link to advance').toBeTruthy();

      // 断言三张凭证
      // 1. EMPLOYEE_ADVANCE 凭证（billHeadCode=advance.code, billType=EMPLOYEE_ADVANCE）
      const advanceVoucherId = await findVoucherIdByBillCodeAndType(page, ctx.advanceCode!, 'EMPLOYEE_ADVANCE');
      expect(advanceVoucherId, 'EMPLOYEE_ADVANCE voucher should exist by advance.code').toBeTruthy();
      await assertVoucherLines(page, advanceVoucherId, [
        { subjectCode: '1221', dcDirection: 'DEBIT', debitAmount: ADVANCE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: ADVANCE_AMOUNT },
      ]);

      // 2. EXPENSE_CLAIM 凭证（billHeadCode=claim.code, billType=EXPENSE_CLAIM）
      const claimVoucherId = await findVoucherIdByBillCodeAndType(page, ctx.claimCode!, 'EXPENSE_CLAIM');
      expect(claimVoucherId, 'EXPENSE_CLAIM voucher should exist by claim.code').toBeTruthy();
      await assertVoucherLines(page, claimVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: CLAIM_AMOUNT_WITHOUT_TAX, creditAmount: 0 },
        { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: CLAIM_TAX, creditAmount: 0 },
        { subjectCode: '2241', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: CLAIM_WITH_TAX },
      ]);

      // 3. EMPLOYEE_ADVANCE_SETTLE 凭证（billHeadCode=claim.code, billType=EMPLOYEE_ADVANCE_SETTLE）
      const settleVoucherId = await findVoucherIdByBillCodeAndType(page, ctx.claimCode!, 'EMPLOYEE_ADVANCE_SETTLE');
      expect(settleVoucherId, 'EMPLOYEE_ADVANCE_SETTLE voucher should exist by claim.code').toBeTruthy();
      await assertVoucherLines(page, settleVoucherId, [
        { subjectCode: '2241', dcDirection: 'DEBIT', debitAmount: CLAIM_WITH_TAX, creditAmount: 0 },
        { subjectCode: '1221', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: CLAIM_WITH_TAX },
      ]);

      // 断定 advance.settledAmount 增量=226
      const advanceState = await verifyState(
        page, 'ErpFinEmployeeAdvance', ctx.advanceId!, 'settledAmount outstandingAmount',
      );
      expect(Number(advanceState.settledAmount), 'advance.settledAmount should be 226 after offset').toBe(CLAIM_WITH_TAX);
      expect(Number(advanceState.outstandingAmount), 'advance.outstandingAmount should be 274 (500-226)').toBe(ADVANCE_AMOUNT - CLAIM_WITH_TAX);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(d) illegal transition guards: UNSUBMITTED→approve / APPROVED→submitForApproval rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinEmployeeAdvance-main');

    const ctx = await setupEmployee(page);
    try {
      await setupAdvance(page, ctx, ADVANCE_AMOUNT);

      const rej1 = await callMutation(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id');
      expect(rej1.errors, 'approve from UNSUBMITTED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej1.errors), 'should carry illegal-transition message').toContain('不允许执行该操作');

      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
      await callMutationOk(page, 'ErpFinEmployeeAdvance', 'approve', { id: ctx.advanceId }, 'id');

      const rej2 = await callMutation(page, 'ErpFinEmployeeAdvance', 'submitForApproval', { id: ctx.advanceId }, 'id');
      expect(rej2.errors, 'submitForApproval from APPROVED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej2.errors), 'should carry illegal-transition message').toContain('不允许执行该操作');
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
