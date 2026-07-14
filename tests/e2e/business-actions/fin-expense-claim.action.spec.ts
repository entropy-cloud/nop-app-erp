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
} from '../orchestration/_helper';

/**
 * Finance ErpFinExpenseClaim 费用报销生命周期 + 凭证行数值断言浏览器层 E2E
 * （plan 2026-07-14-1218-2 Phase 1）。
 *
 * 验证 finance 域费用报销 DIRECT useApproval 审批轴经 GraphQL `/graphql` 的全栈可达性 +
 * 三轴状态机迁移 + EXPENSE_CLAIM 业财过账凭证行精确数值断言：
 *   (a) OWN_ACCOUNT 路径：submit→approve→posted=true + Dr 6602 / Dr 2221 / Cr 2241 凭证行断言
 *   (b) COMPANY_ACCOUNT 路径：同上但 Cr 1002（银行存款，非应付-员工）
 *   (c) reverseApprove 红冲：APPROVED→REJECTED + posted=false + 红字凭证同向取负 + 原凭证 isReversed=true
 *   (d) 非法迁移守卫：UNSUBMITTED→approve / APPROVED→submitForApproval 拒绝
 *
 * 权威设计（docs/design/finance/expense-claim.md §业财过账 §状态机 §实现偏离补注 #4）：
 *   - paymentMode 决定贷方科目：OWN_ACCOUNT→Cr 2241 应付-员工 / COMPANY_ACCOUNT→Cr 1002 银行存款
 *   - 价税分离三件套：Dr 6602 管理费用(amountWithoutTax) / Dr 2221 进项税(taxAmount) / Cr 贷方(amountWithTax)
 *   - 员工→partnerId 解析：claimantId→ErpMdEmployee.partnerId 非空（员工无 partner 记录时审核被拒）
 *   - ExpenseClaimAcctDocProvider 硬编码科目 6602（非读行 subjectId）；billHeadCode=claim.code
 *
 * Explore Decision（Phase 1 setup 最小预置集 + 向后兼容裁定）：
 *   - 种子 ErpMdEmployee(id=1~3) 均 partnerId=null → 须自包含建测试 partner(EMPLOYEE) + employee(partnerId)
 *   - validateForApproval 在 submit + approve 均调用：须 claimant ACTIVE+partnerId、行非空+expenseType、
 *     amountWithTax 头=Σ行、reason 非空（config erp-fin.expense-reason-required 默认 true）
 *   - erp-fin.expense-budget-check-enabled=true 启用后无匹配 APPROVED 预算方案时 PASS-on-no-match（向后兼容）
 *   - approve 不改 docStatus（仅 approveStatus+posted）；verifyState 经 __get 核实实际 docStatus 不预设
 *   - reverseApprove 红冲凭证 isReversed=true（buildReversalDraft 同向取负 + persistVoucher isReversed=true
 *     + markOriginalVoucherReversed 原凭证 isReversed=true），区别于域侧 posted=false 实体级回退
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period 2026-07 id=1（OPEN）。
 * 科目引用：6602 折旧费用(id=31,DEBIT) / 2221 应交税费(id=12,CREDIT) / 2241 其他应付款-员工(id=38,CREDIT,本计划补齐)
 *           / 1002 银行存款(id=2,DEBIT)。
 * 自包含隔离：每用例独立建 partner+employee+claim+line（code 唯一 E2E-EC-*），cleanup 删凭证+AR-AP+行+头+员工+partner。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
/** 种子费用科目 6602 折旧费用（id=31, EXPENSE, DEBIT）—— ExpenseClaimAcctDocProvider 硬编码费用科目。 */
const SUBJECT_EXPENSE_ID = 31;
const SUBJECT_EXPENSE_CODE = '6602';
const BDATE = '2026-07-15';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}${Date.now()}${_seq}`;
}

interface Ctx {
  partnerId?: string | number;
  employeeId?: string | number;
  claimId?: string | number;
  claimCode?: string;
  lineId?: string | number;
}

async function setupEmployee(page: import('@playwright/test').Page): Promise<Ctx> {
  const ctx: Ctx = {};
  const partnerCode = uniq('EP');
  const partner = await createViaSave(
    page,
    'ErpMdPartner',
    {
      code: partnerCode, name: `E2E Claim Partner ${partnerCode}`,
      partnerType: 'EMPLOYEE', status: 'ACTIVE', creditLimit: 0, creditPeriodDays: 0,
    },
    'id',
  );
  ctx.partnerId = partner.id;

  const empCode = uniq('EE');
  const employee = await createViaSave(
    page,
    'ErpMdEmployee',
    {
      code: empCode, name: `E2E Claimant ${empCode}`,
      orgId: ORG, status: 'ACTIVE', partnerId: partner.id,
    },
    'id',
  );
  ctx.employeeId = employee.id;
  return ctx;
}

async function setupClaim(
  page: import('@playwright/test').Page,
  ctx: Ctx,
  paymentMode: 'OWN_ACCOUNT' | 'COMPANY_ACCOUNT',
  amountWithoutTax: number,
  taxAmount: number,
  amountWithTax: number,
): Promise<void> {
  const code = uniq('EC');
  const claim = await createViaSave(
    page,
    'ErpFinExpenseClaim',
    {
      code, orgId: ORG, claimantId: ctx.employeeId, departmentId: ORG,
      businessDate: BDATE, paymentMode,
      currencyId: CURRENCY, exchangeRate: 1,
      amountSource: amountWithTax, amountFunctional: amountWithoutTax,
      amountWithoutTax, taxAmount, amountWithTax,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
      reason: `E2E test expense claim ${code}`,
    },
    'id code',
  );
  ctx.claimId = claim.id;
  ctx.claimCode = code;

  const line = await createViaSave(
    page,
    'ErpFinExpenseClaimLine',
    {
      claimId: claim.id, lineNo: 1, expenseType: 'OFFICE',
      subjectId: SUBJECT_EXPENSE_ID, subjectCode: SUBJECT_EXPENSE_CODE,
      amountWithoutTax, taxRate: 13, taxAmount, amountWithTax,
    },
    'id',
  );
  ctx.lineId = line.id;
}

async function cleanupCtx(page: import('@playwright/test').Page, ctx: Ctx): Promise<void> {
  if (!ctx) return;
  if (ctx.claimCode) {
    await cleanupArApByCode(page, ctx.claimCode);
    await cleanupVoucherByBillCode(page, ctx.claimCode);
  }
  if (ctx.lineId) await deleteById(page, 'ErpFinExpenseClaimLine', ctx.lineId);
  if (ctx.claimId) await deleteById(page, 'ErpFinExpenseClaim', ctx.claimId);
  if (ctx.employeeId) await deleteById(page, 'ErpMdEmployee', ctx.employeeId);
  if (ctx.partnerId) await deleteById(page, 'ErpMdPartner', ctx.partnerId);
}

test.describe('Finance ErpFinExpenseClaim lifecycle + voucher lines browser-layer E2E', () => {
  test('(a) OWN_ACCOUNT: submit→approve→posted + Dr 6602/Dr 2221/Cr 2241 voucher lines', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = await setupEmployee(page);
    try {
      await setupClaim(page, ctx, 'OWN_ACCOUNT', 100, 13, 113);

      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id approveStatus');
      const afterSubmit = await verifyState(page, 'ErpFinExpenseClaim', ctx.claimId!, 'approveStatus docStatus');
      expect(afterSubmit.approveStatus, 'submit should transition approveStatus → SUBMITTED').toBe('SUBMITTED');

      const approved = await callMutationOk(
        page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id approveStatus posted',
      );
      expect(approved.approveStatus, 'approve should transition approveStatus → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should set posted=true').toBe(true);

      const afterApprove = await verifyState(page, 'ErpFinExpenseClaim', ctx.claimId!, 'approveStatus posted docStatus');
      expect(afterApprove.approveStatus, '__get should confirm APPROVED').toBe('APPROVED');
      expect(afterApprove.posted, '__get should confirm posted=true').toBe(true);

      const voucherId = await findVoucherIdByBillCode(page, ctx.claimCode!, 'NORMAL');
      expect(voucherId, 'EXPENSE_CLAIM NORMAL voucher should exist by claim.code').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: 100, creditAmount: 0 },
        { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: 13, creditAmount: 0 },
        { subjectCode: '2241', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 113 },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) COMPANY_ACCOUNT: submit→approve→posted + Cr 1002 (bank deposit, not payable-employee)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = await setupEmployee(page);
    try {
      await setupClaim(page, ctx, 'COMPANY_ACCOUNT', 100, 13, 113);

      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id approveStatus');

      const approved = await callMutationOk(
        page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id approveStatus posted',
      );
      expect(approved.approveStatus, 'approve should transition approveStatus → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should set posted=true').toBe(true);

      const voucherId = await findVoucherIdByBillCode(page, ctx.claimCode!, 'NORMAL');
      expect(voucherId, 'EXPENSE_CLAIM NORMAL voucher should exist by claim.code').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: 100, creditAmount: 0 },
        { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: 13, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 113 },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(c) reverseApprove: APPROVED→REJECTED + posted=false + reversal voucher lines same-direction negated + original isReversed', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = await setupEmployee(page);
    try {
      await setupClaim(page, ctx, 'OWN_ACCOUNT', 100, 13, 113);
      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id');
      await callMutationOk(page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id posted');

      const originalVoucherId = await findVoucherIdByBillCode(page, ctx.claimCode!, 'NORMAL');
      expect(originalVoucherId, 'original NORMAL voucher should exist before reverseApprove').toBeTruthy();

      const reversed = await callMutationOk(
        page, 'ErpFinExpenseClaim', 'reverseApprove', { id: ctx.claimId }, 'id approveStatus posted',
      );
      expect(reversed.approveStatus, 'reverseApprove should transition approveStatus → REJECTED').toBe('REJECTED');
      expect(reversed.posted, 'reverseApprove should set posted=false').toBe(false);

      const afterReverse = await verifyState(page, 'ErpFinExpenseClaim', ctx.claimId!, 'approveStatus posted');
      expect(afterReverse.approveStatus, '__get should confirm REJECTED').toBe('REJECTED');
      expect(afterReverse.posted, '__get should confirm posted=false').toBe(false);

      const originalVoucher = await verifyState(page, 'ErpFinVoucher', originalVoucherId!, 'id postingType isReversed');
      expect(originalVoucher.isReversed, 'original NORMAL voucher should be marked isReversed=true').toBe(true);

      const reversalVoucherId = await findVoucherIdByBillCode(page, ctx.claimCode!, 'REVERSAL');
      expect(reversalVoucherId, 'REVERSAL voucher should exist by claim.code').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: -100, creditAmount: 0 },
        { subjectCode: '2221', dcDirection: 'DEBIT', debitAmount: -13, creditAmount: 0 },
        { subjectCode: '2241', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -113 },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(d) illegal transition guards: UNSUBMITTED→approve / APPROVED→submitForApproval rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinExpenseClaim-main');

    const ctx = await setupEmployee(page);
    try {
      await setupClaim(page, ctx, 'OWN_ACCOUNT', 100, 13, 113);

      const rej1 = await callMutation(page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id');
      expect(rej1.errors, 'approve from UNSUBMITTED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej1.errors), 'should carry illegal-transition message').toContain('不允许执行该操作');

      await callMutationOk(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id');
      await callMutationOk(page, 'ErpFinExpenseClaim', 'approve', { id: ctx.claimId }, 'id');

      const rej2 = await callMutation(page, 'ErpFinExpenseClaim', 'submitForApproval', { id: ctx.claimId }, 'id');
      expect(rej2.errors, 'submitForApproval from APPROVED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej2.errors), 'should carry illegal-transition message').toContain('不允许执行该操作');
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
