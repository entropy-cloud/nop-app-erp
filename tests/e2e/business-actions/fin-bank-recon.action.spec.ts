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
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * Finance ErpFinBankReconciliation 银行对账生命周期浏览器层 E2E（plan 2026-07-12-0413-2 Phase 1）。
 *
 * 验证 finance 域银行对账 DIRECT `@BizMutation` 三态状态机经 GraphQL /graphql 的全栈可达性：
 *   generate(statementId) → DRAFT → post(reconciliationId) → POSTED（产 BANK_RECON_ADJ 调整凭证）
 *   → reverse(reconciliationId) → CANCELLED（红冲调整凭证）。
 *
 * 权威设计（docs/design/finance/bank-reconciliation.md §实现权威 schema 补注）：
 *   - docStatus 复用 `erp-fin/voucher-status`：DRAFT→POSTED→CANCELLED（无 RECONCILING/REVERSED，
 *     RECONCILING 由「有未勾对行」派生表达，RECONCILED=POSTED，红冲回退=CANCELLED）。
 *   - 平衡恒等式（BankReconciliationBuilder:67-75）：statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded，
 *     其中未达 = UNMATCHED 银行行净值（CREDIT − DEBIT），diff 超精度抛 ERR_BANK_RECON_NOT_BALANCED。
 *   - post 产物：仅 setup 含 UNMATCHED 行时产 BANK_RECON_ADJ 调整凭证（BankReconciliationBuilder.post:125-127
 *     委托 BankReconAdjustmentVoucherBuilder.post 对 unmatched 行构造 PostingEvent → IErpFinVoucherBiz.post）；
 *     零未达项 post 仅翻 docStatus 不产凭证。
 *   - reverse 红冲：经 voucherBiz.reverse(billHeadCode=recon.code, businessType=BANK_RECON_ADJ) 生成红字凭证。
 *
 * 自包含 setup 三约束（最复杂自包含 setup）：
 *   (i) ErpFinFundAccount(accountType=BANK, subjectId=1002 银行存款, currentBalance=1000)——requireFundAccount + post 凭证 bankSubject 解析；
 *   (ii) 平衡等式：statementBalance(1100) − bookBalance(1000) = 100 = bankCreditUnrecorded(100, 1 行 UNMATCHED CREDIT) − bankDebitUnrecorded(0)；
 *   (iii) 故意含 1 条 UNMATCHED CREDIT 行 → post 产 BANK_RECON_ADJ 凭证（Dr 1002 / Cr 2240OTHER，各 100）。
 *
 * 科目依赖（plan Infrastructure And Config Prereqs 裁决：按 1800-1 范式追加）：种子 erp_md_subject.csv 补齐
 * 2240OTHER（未达账项调整对方科目，BankReconAdjustmentVoucherBuilder.resolveAdjSubjectCode 默认值），
 * 使 BankReconAdjAcctDocProvider → ErpFinPostingProcessor.resolveSubjects findByCode('2240OTHER') 可达；
 * bankSubject=1002 已在种子（id=2）。无新增 JVM 属性（adj 科目走默认值）。
 *
 * 清理：删对账调整行 + 调节表 + 凭证（NORMAL+REVERSAL，按 recon.code）+ 对账单行 + 对账单 + 资金账户，
 * 使 finance 看板（读 gl_balance，posting 不写 gl_balance）/ 报表基线无漂移。
 *
 * 种子引用：org id=2 / currency CNY id=1 / subject 1002 银行存款 id=2。
 */
const ORG = 2;
const CURRENCY = 1;
const BANK_SUBJECT_ID = 2; // 1002 银行存款
const BDATE = '2026-07-10';
const BOOK_BALANCE = 1000;
const UNRECONCILED_AMT = 100;
const STATEMENT_BALANCE = BOOK_BALANCE + UNRECONCILED_AMT; // 1100

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createFundAccount(page: import('@playwright/test').Page): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinFundAccount',
    {
      code: uniq('E2E-BANKRECON-FA'),
      name: 'E2E Bank Recon Fund Account',
      orgId: ORG,
      accountType: 'BANK',
      subjectId: BANK_SUBJECT_ID,
      currencyId: CURRENCY,
      currentBalance: BOOK_BALANCE,
      status: 'ACTIVE',
    },
    'id',
  );
}

async function createStatement(page: import('@playwright/test').Page, fundAccountId: string | number): Promise<{ id: string; code: string }> {
  return createViaSave(
    page,
    'ErpFinBankStatement',
    {
      code: uniq('E2E-BANKSTMT'),
      orgId: ORG,
      fundAccountId,
      statementDate: BDATE,
      beginningBalance: BOOK_BALANCE,
      endingBalance: STATEMENT_BALANCE,
      docStatus: 'DRAFT',
    },
    'id code',
  );
}

async function createStatementLine(
  page: import('@playwright/test').Page,
  statementId: string | number,
  dcDirection: string,
  amount: number,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinBankStatementLine',
    {
      statementId,
      lineNo: 1,
      transactionDate: BDATE,
      dcDirection,
      amount,
      currencyId: CURRENCY,
      matchStatus: 'UNMATCHED',
    },
    'id',
  );
}

async function cleanupBankRecon(
  page: import('@playwright/test').Page,
  reconId: string | number | null,
  reconCode: string | null,
  statementId: string | number | null,
  fundAccountId: string | number | null,
): Promise<void> {
  if (reconId != null) {
    await deleteByFilter(page, 'ErpFinBankReconciliationLine', eqFilter('reconciliationId', Number(reconId)));
    await deleteById(page, 'ErpFinBankReconciliation', reconId);
  }
  if (reconCode) {
    await cleanupVoucherByBillCode(page, reconCode);
  }
  if (statementId != null) {
    await deleteByFilter(page, 'ErpFinBankStatementLine', eqFilter('statementId', Number(statementId)));
    await deleteById(page, 'ErpFinBankStatement', statementId);
  }
  if (fundAccountId != null) {
    await deleteById(page, 'ErpFinFundAccount', fundAccountId);
  }
}

test.describe('Finance ErpFinBankReconciliation lifecycle browser-layer E2E', () => {
  test('happy path: generate(DRAFT) → post(POSTED + BANK_RECON_ADJ voucher) → reverse(CANCELLED + reversal voucher)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBankReconciliation-main');

    const fundAccount = await createFundAccount(page);
    const statement = await createStatement(page, fundAccount.id);
    const line = await createStatementLine(page, statement.id, 'CREDIT', UNRECONCILED_AMT);

    try {
      // generate：平衡门控（statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded: 1100 − 1000 = 100 − 0）
      // → 产 DRAFT 调节表 + 1 条 adjustment line（UNMATCHED 行）
      const generated = await callMutationOk(
        page, 'ErpFinBankReconciliation', 'generate', { statementId: statement.id }, 'id code docStatus',
      );
      expect(generated.id, 'generate should return reconciliation id').toBeTruthy();
      expect(generated.docStatus, 'generate should produce DRAFT').toBe('DRAFT');
      expect(generated.code, 'generate should produce code').toBeTruthy();

      const reconFinal0 = await verifyState(page, 'ErpFinBankReconciliation', generated.id, 'docStatus');
      expect(reconFinal0.docStatus, '__get should confirm DRAFT after generate').toBe('DRAFT');

      // post：DRAFT → POSTED + unmatched 行存在故产 BANK_RECON_ADJ 调整凭证
      const posted = await callMutationOk(
        page, 'ErpFinBankReconciliation', 'post', { reconciliationId: generated.id }, 'id docStatus',
      );
      expect(posted.docStatus, 'post should transition DRAFT → POSTED').toBe('POSTED');

      const reconFinal1 = await verifyState(page, 'ErpFinBankReconciliation', generated.id, 'docStatus');
      expect(reconFinal1.docStatus, '__get should confirm POSTED after post').toBe('POSTED');

      // BANK_RECON_ADJ 调整凭证存在性断言（经 ErpFinVoucherBillR billCode=recon.code + businessType=BANK_RECON_ADJ 反查）
      const billR = await findFirst<any>(
        page, 'ErpFinVoucherBillR',
        eqFilter('billCode', generated.code),
        'voucherId',
      );
      expect(billR, 'post should produce BANK_RECON_ADJ voucher bill-link by recon.code').toBeTruthy();
      expect(billR.voucherId, 'BANK_RECON_ADJ voucher id should be non-null').toBeTruthy();

      // reverse：POSTED → CANCELLED + 红冲凭证
      const reversed = await callMutationOk(
        page, 'ErpFinBankReconciliation', 'reverse', { reconciliationId: generated.id }, 'id docStatus',
      );
      expect(reversed.docStatus, 'reverse should transition POSTED → CANCELLED').toBe('CANCELLED');

      const reconFinal2 = await verifyState(page, 'ErpFinBankReconciliation', generated.id, 'docStatus');
      expect(reconFinal2.docStatus, '__get should confirm CANCELLED after reverse').toBe('CANCELLED');

      // 红冲后存在 REVERSAL 凭证（与原 NORMAL 凭证共用 billCode=recon.code，postingType 区分）
      const links = await page.request.post('/graphql', {
        data: {
          query: `query($f:Map){ ErpFinVoucherBillR__findPage(query:{offset:0,limit:10,filter:$f}){ items{ voucherId } } }`,
          variables: { f: eqFilter('billCode', generated.code) },
        },
      });
      const linksJson: any = await links.json();
      const voucherIds: any[] = linksJson?.data?.ErpFinVoucherBillR__findPage?.items?.map((i: any) => i.voucherId) ?? [];
      let hasReversal = false;
      for (const vid of voucherIds) {
        const v = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', Number(vid)), 'id postingType');
        if (v && v.postingType === 'REVERSAL') {
          hasReversal = true;
          break;
        }
      }
      expect(hasReversal, 'reverse should produce a REVERSAL voucher (by postingType)').toBe(true);
    } finally {
      await cleanupBankRecon(page, null, null, statement.id, fundAccount.id);
      await deleteById(page, 'ErpFinBankStatementLine', line.id);
    }
  });

  test('negative: post on already-POSTED reconciliation → ERR_BANK_RECON_ILLEGAL_DOC_STATUS_TRANSITION', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBankReconciliation-main');

    const fundAccount = await createFundAccount(page);
    const statement = await createStatement(page, fundAccount.id);
    const line = await createStatementLine(page, statement.id, 'CREDIT', UNRECONCILED_AMT);

    try {
      const generated = await callMutationOk(
        page, 'ErpFinBankReconciliation', 'generate', { statementId: statement.id }, 'id code docStatus',
      );
      await callMutationOk(
        page, 'ErpFinBankReconciliation', 'post', { reconciliationId: generated.id }, 'id docStatus',
      );

      // 重复 post（已 POSTED）→ illegal transition
      const rej = await callMutation(
        page, 'ErpFinBankReconciliation', 'post', { reconciliationId: generated.id }, 'id docStatus',
      );
      expect(rej.errors, 're-post on POSTED should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

      const reconFinal = await verifyState(page, 'ErpFinBankReconciliation', generated.id, 'docStatus');
      expect(reconFinal.docStatus, 'rejected re-post should leave POSTED').toBe('POSTED');
    } finally {
      await cleanupBankRecon(page, null, null, statement.id, fundAccount.id);
      await deleteById(page, 'ErpFinBankStatementLine', line.id);
    }
  });

  test('negative: reverse on DRAFT (not POSTED) → ERR_BANK_RECON_ILLEGAL_DOC_STATUS_TRANSITION', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBankReconciliation-main');

    const fundAccount = await createFundAccount(page);
    const statement = await createStatement(page, fundAccount.id);
    const line = await createStatementLine(page, statement.id, 'CREDIT', UNRECONCILED_AMT);

    try {
      const generated = await callMutationOk(
        page, 'ErpFinBankReconciliation', 'generate', { statementId: statement.id }, 'id code docStatus',
      );

      // reverse on DRAFT（未 POSTED）→ illegal transition
      const rej = await callMutation(
        page, 'ErpFinBankReconciliation', 'reverse', { reconciliationId: generated.id }, 'id docStatus',
      );
      expect(rej.errors, 'reverse on DRAFT should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

      const reconFinal = await verifyState(page, 'ErpFinBankReconciliation', generated.id, 'docStatus');
      expect(reconFinal.docStatus, 'rejected reverse should leave DRAFT').toBe('DRAFT');
    } finally {
      await cleanupBankRecon(page, null, null, statement.id, fundAccount.id);
      await deleteById(page, 'ErpFinBankStatementLine', line.id);
    }
  });
});
