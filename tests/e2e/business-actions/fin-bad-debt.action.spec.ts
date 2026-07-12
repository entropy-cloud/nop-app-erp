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
 * Finance ErpFinBadDebt 坏账生命周期浏览器层 E2E（plan 2026-07-12-0413-2 Phase 2）。
 *
 * 验证 finance 域坏账 DIRECT `@BizMutation` 三路径经 GraphQL /graphql 的全栈可达性：
 *   (a) 核销正路径：writeOff(arApItemId,reason) → submit(id) → approve(id) DIRECT 审批轴
 *       （UNSUBMITTED→SUBMITTED→APPROVED；approve 触发 executeWriteOff：ArApItem status OPEN→WRITTEN_OFF
 *        + openAmount→0 + BAD_DEBT_WRITE_OFF 凭证 借Allowance/贷AR，不进 P&L）。
 *   (b) 收回路径：recover(arApItemId,reason) → submit(id) → approve(id)
 *       （approve 触发 executeRecovery：ArApItem status WRITTEN_OFF→OPEN + openAmount 恢复
 *        + BAD_DEBT_RECOVERY 凭证 借AR/贷Allowance）。
 *   (c) 期末计提：runBadDebtProvision(periodId) → BadDebtProvisionResult 结构非空 + action ∈ {RESERVE,RELEASE,NONE}。
 *
 * 权威设计（docs/design/finance/bad-debt.md）：
 *   - 核销/收回不进 P&L（损失在计提时已确认），经 CloseVoucherWriter 直接持久化凭证（非 Provider 模型，
 *     避免触发 ArApItem 生成），凭证 billCode = debt.code。
 *   - 审批门控 `erp-fin.bad-debt-write-off-require-approval`（默认 true）：开启时 writeOff/recover 创建=UNSUBMITTED，
 *     approve 才执行 ArApItem 变异 + 凭证；关闭时创建即自动审批执行。本 spec 验证默认开启的审批轴正路径。
 *   - 期末计提（BadDebtProvisionService）：必需准备 > Allowance 账面 → RESERVE（借信用减值损失/贷坏账准备）；
 *     < → RELEASE；相等 → NONE。ALLOWANCE 充足性门控 config-gated（科目未配置时告警跳过）。
 *
 * 状态字段（ErpFinBadDebt.approvalStatus，dict wf/approve-status）：
 *   UNSUBMITTED（writeOff/recover 创建态）→ SUBMITTED（submit）→ APPROVED（approve，执行生效）/ REJECTED（reject）。
 *   ErpFinArApItem.status：OPEN →（writeOff approve）→ WRITTEN_OFF →（recover approve）→ OPEN。
 *
 * 科目依赖（plan Infrastructure And Config Prereqs 裁决：按 1800-1 范式追加 webServer 属性 + 种子科目补齐）：
 *   executeWriteOff/recover requireSubject 读 config `erp-fin.bad-debt-allowance-subject-code`（1231 坏账准备）
 *   + `erp-fin.ar-subject-code`（1122 应收账款）；provision requireSubject 读 `erp-fin.bad-debt-expense-subject-code`
 *   （6701 信用减值损失）+ allowance。config 默认 null → 未配置抛 ERR_CLOSE_SUBJECT_NOT_CONFIGURED，故 webServer JVM
 *   属性追加 3 项；种子 erp_md_subject.csv 补齐 1231/6701 两科目（1122 已在种子 id=3）使 resolveSubjects findByCode 可达。
 *
 * 自包含隔离（复用 0204-2 partner 隔离）：新建 partner `E2E-BADDEBT-PN-` + OPEN RECEIVABLE ErpFinArApItem
 * （direction=RECEIVABLE, sourceBillType=AR_INVOICE, status=OPEN, openAmount>0），避开种子 ar_ap_item（3/4 SETTLED，
 * 5 EMPLOYEE_ADVANCE）。cleanup 删 partner + 凭证（按 debt.code）+ 坏账单 + AR-AP 项，使 finance 看板/报表基线无漂移。
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
      code: uniq('E2E-BADDEBT-PN'),
      name: `E2E BadDebt Partner ${tag}`,
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
      code: uniq(`E2E-BADDEBT-AR-${tag}`),
      orgId: ORG,
      acctSchemaId: ACCT_SCHEMA,
      direction: 'RECEIVABLE',
      partnerId,
      sourceBillType: 'AR_INVOICE',
      sourceBillCode: uniq(`E2E-BADDEBT-INV-${tag}`),
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
  provisionVoucherId?: number | null;
}

async function cleanupBadDebt(page: import('@playwright/test').Page, ctx: CleanupCtx): Promise<void> {
  for (const code of ctx.debtCodes ?? []) {
    await cleanupVoucherByBillCode(page, code);
  }
  for (const id of ctx.debtIds ?? []) {
    await deleteById(page, 'ErpFinBadDebt', id);
  }
  if (ctx.provisionVoucherId != null) {
    const vid = Number(ctx.provisionVoucherId);
    await deleteByFilter(page, 'ErpFinVoucherLine', eqFilter('voucherId', vid));
    await deleteByFilter(page, 'ErpFinVoucherBillR', eqFilter('voucherId', vid));
    await deleteById(page, 'ErpFinVoucher', vid);
  }
  if (ctx.arItemId != null) {
    await deleteById(page, 'ErpFinArApItem', ctx.arItemId);
  }
  if (ctx.partnerId != null) {
    await deleteById(page, 'ErpMdPartner', ctx.partnerId);
  }
}

test.describe('Finance ErpFinBadDebt lifecycle browser-layer E2E', () => {
  test('(a) writeOff → submit → approve: UNSUBMITTED→SUBMITTED→APPROVED + ArApItem OPEN→WRITTEN_OFF + BAD_DEBT_WRITE_OFF voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    const partner = await createPartner(page, 'wo');
    const item = await createOpenArItem(page, partner.id, 'WO');
    const ctx: CleanupCtx = { partnerId: partner.id, arItemId: item.id, debtIds: [], debtCodes: [] };

    try {
      // writeOff：创建 WRITE_OFF 坏账单（approvalStatus=UNSUBMITTED，审批门控默认 true 故不立即生效）
      const writtenOff = await callMutationOk(
        page, 'ErpFinBadDebt', 'writeOff',
        { arApItemId: item.id, reason: 'E2E bad debt write-off' },
        'id code docType approvalStatus',
      );
      expect(writtenOff.id, 'writeOff should return bad-debt id').toBeTruthy();
      expect(writtenOff.docType, 'writeOff should produce docType=WRITE_OFF').toBe('WRITE_OFF');
      expect(writtenOff.approvalStatus, 'writeOff should produce approvalStatus=UNSUBMITTED').toBe('UNSUBMITTED');
      ctx.debtIds!.push(writtenOff.id);
      ctx.debtCodes!.push(writtenOff.code);

      const debtFinal0 = await verifyState(page, 'ErpFinBadDebt', writtenOff.id, 'approvalStatus');
      expect(debtFinal0.approvalStatus, '__get should confirm UNSUBMITTED after writeOff').toBe('UNSUBMITTED');

      // writeOff 创建态不立即变异 ArApItem（审批门控）
      const itemAfterCreate = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemAfterCreate.status, 'ArApItem should remain OPEN before approve').toBe('OPEN');

      // submit：UNSUBMITTED → SUBMITTED
      const submitted = await callMutationOk(
        page, 'ErpFinBadDebt', 'submit', { id: writtenOff.id }, 'id approvalStatus',
      );
      expect(submitted.approvalStatus, 'submit should transition UNSUBMITTED → SUBMITTED').toBe('SUBMITTED');

      // approve：SUBMITTED → APPROVED（执行 executeWriteOff：ArApItem WRITTEN_OFF + openAmount→0 + 凭证）
      const approved = await callMutationOk(
        page, 'ErpFinBadDebt', 'approve', { id: writtenOff.id }, 'id approvalStatus',
      );
      expect(approved.approvalStatus, 'approve should transition SUBMITTED → APPROVED').toBe('APPROVED');

      const debtFinal1 = await verifyState(page, 'ErpFinBadDebt', writtenOff.id, 'approvalStatus');
      expect(debtFinal1.approvalStatus, '__get should confirm APPROVED after approve').toBe('APPROVED');

      // ArApItem 已变异：status→WRITTEN_OFF + openAmount→0
      const itemAfterApprove = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemAfterApprove.status, 'ArApItem should be WRITTEN_OFF after approve').toBe('WRITTEN_OFF');
      expect(Number(itemAfterApprove.openAmountFunctional), 'ArApItem openAmount should be 0 after approve').toBe(0);

      // BAD_DEBT_WRITE_OFF 凭证存在性（billCode=debt.code，经 ErpFinVoucherBillR 反查）
      const billR = await findFirst<any>(
        page, 'ErpFinVoucherBillR', eqFilter('billCode', writtenOff.code), 'voucherId',
      );
      expect(billR, 'approve should produce BAD_DEBT_WRITE_OFF voucher bill-link by debt.code').toBeTruthy();
      expect(billR.voucherId, 'BAD_DEBT_WRITE_OFF voucher id should be non-null').toBeTruthy();

      // BAD_DEBT_WRITE_OFF 凭证行精确数值断言（Dr 1231 坏账准备 / Cr 1122 应收账款，金额=AR 项 openAmount=100，
      // plan 2026-07-12-1321-2 Phase 1；ErpFinBadDebtProcessor.executeWriteOff :130-136）
      const writeOffVoucherId = await findVoucherIdByBillCode(page, writtenOff.code, 'NORMAL');
      await assertVoucherLines(page, writeOffVoucherId, [
        { subjectCode: '1231', dcDirection: 'DEBIT', debitAmount: AMOUNT, creditAmount: 0 },
        { subjectCode: '1122', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: AMOUNT },
      ]);
    } finally {
      await cleanupBadDebt(page, ctx);
    }
  });

  test('(b) recover → submit → approve: WRITTEN_OFF→OPEN restored + BAD_DEBT_RECOVERY voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    const partner = await createPartner(page, 'rc');
    const item = await createOpenArItem(page, partner.id, 'RC');
    const ctx: CleanupCtx = { partnerId: partner.id, arItemId: item.id, debtIds: [], debtCodes: [] };

    try {
      // 前置：先 writeOff→submit→approve 使 ArApItem 进入 WRITTEN_OFF（recover 前置态）
      const writtenOff = await callMutationOk(
        page, 'ErpFinBadDebt', 'writeOff',
        { arApItemId: item.id, reason: 'E2E setup write-off before recover' },
        'id code approvalStatus',
      );
      ctx.debtIds!.push(writtenOff.id);
      ctx.debtCodes!.push(writtenOff.code);
      await callMutationOk(page, 'ErpFinBadDebt', 'submit', { id: writtenOff.id }, 'id approvalStatus');
      await callMutationOk(page, 'ErpFinBadDebt', 'approve', { id: writtenOff.id }, 'id approvalStatus');
      const itemWo = await verifyState(page, 'ErpFinArApItem', item.id, 'status');
      expect(itemWo.status, 'precondition: ArApItem should be WRITTEN_OFF before recover').toBe('WRITTEN_OFF');

      // recover：创建 RECOVERY 坏账单（approvalStatus=UNSUBMITTED）
      const recovered = await callMutationOk(
        page, 'ErpFinBadDebt', 'recover',
        { arApItemId: item.id, reason: 'E2E bad debt recovery' },
        'id code docType approvalStatus',
      );
      expect(recovered.docType, 'recover should produce docType=RECOVERY').toBe('RECOVERY');
      expect(recovered.approvalStatus, 'recover should produce approvalStatus=UNSUBMITTED').toBe('UNSUBMITTED');
      ctx.debtIds!.push(recovered.id);
      ctx.debtCodes!.push(recovered.code);

      // submit → approve（执行 executeRecovery：ArApItem status→OPEN + openAmount 恢复 + 凭证）
      await callMutationOk(page, 'ErpFinBadDebt', 'submit', { id: recovered.id }, 'id approvalStatus');
      const approved = await callMutationOk(
        page, 'ErpFinBadDebt', 'approve', { id: recovered.id }, 'id approvalStatus',
      );
      expect(approved.approvalStatus, 'recover approve should reach APPROVED').toBe('APPROVED');

      // ArApItem 已恢复：status→OPEN + openAmount 恢复
      const itemAfterRecover = await verifyState(page, 'ErpFinArApItem', item.id, 'status openAmountFunctional');
      expect(itemAfterRecover.status, 'ArApItem should be restored OPEN after recover approve').toBe('OPEN');
      expect(Number(itemAfterRecover.openAmountFunctional), 'ArApItem openAmount should restore after recover approve').toBe(AMOUNT);

      // BAD_DEBT_RECOVERY 凭证存在性（billCode=recovered.code）
      const billR = await findFirst<any>(
        page, 'ErpFinVoucherBillR', eqFilter('billCode', recovered.code), 'voucherId',
      );
      expect(billR, 'recover approve should produce BAD_DEBT_RECOVERY voucher bill-link by debt.code').toBeTruthy();
      expect(billR.voucherId, 'BAD_DEBT_RECOVERY voucher id should be non-null').toBeTruthy();

      // BAD_DEBT_RECOVERY 凭证行精确数值断言（Dr 1122 应收账款 / Cr 1231 坏账准备，金额=核销时金额=100，
      // plan 2026-07-12-1321-2 Phase 1；ErpFinBadDebtProcessor.executeRecovery :156-160；CloseVoucherWriter postingType=NORMAL）
      const recoveryVoucherId = await findVoucherIdByBillCode(page, recovered.code, 'NORMAL');
      await assertVoucherLines(page, recoveryVoucherId, [
        { subjectCode: '1122', dcDirection: 'DEBIT', debitAmount: AMOUNT, creditAmount: 0 },
        { subjectCode: '1231', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: AMOUNT },
      ]);
    } finally {
      await cleanupBadDebt(page, ctx);
    }
  });

  test('(c) runBadDebtProvision(periodId): BadDebtProvisionResult non-empty structure + action ∈ {RESERVE,RELEASE,NONE}', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    // 计提是期间批量动作（无审批），返回 BadDebtProvisionResult DTO（非实体）。action 取决于必需准备 vs Allowance 账面：
    //   必需 > 账面 → RESERVE（产 BAD_DEBT_RESERVE 凭证）；< → RELEASE（产 BAD_DEBT_RELEASE 凭证）；= → NONE。
    // 本用例断言结构非空 + action 合法 + voucherId 与 action 一致性，不断言精确数值（属 0704-1 结果面 successor）。
    const result = await callMutationOk(
      page, 'ErpFinBadDebt', 'runBadDebtProvision',
      { periodId: PERIOD },
      'action voucherId requiredProvision allowanceBalance totalConsidered',
    );

    expect(['RESERVE', 'RELEASE', 'NONE'], `action should be RESERVE/RELEASE/NONE, got ${result.action}`).toContain(result.action);
    expect(Number(result.requiredProvision), 'requiredProvision should be a non-negative number').toBeGreaterThanOrEqual(0);
    expect(Number(result.allowanceBalance), 'allowanceBalance should be a number').toBeGreaterThanOrEqual(0);
    expect(Number(result.totalConsidered), 'totalConsidered should be a non-negative number').toBeGreaterThanOrEqual(0);

    if (result.action !== 'NONE') {
      expect(result.voucherId, `${result.action} action should produce a voucher id`).toBeTruthy();
    }

    // BAD_DEBT_RESERVE 凭证行精确数值断言（Dr 6701 信用减值损失 / Cr 1231 坏账准备，
    // 金额=requiredProvision − allowanceBalance，plan 2026-07-12-1321-2 Phase 1；
    // BadDebtProvisionService :88-104；RELEASE 反向科目经 Deferred 裁决 Successor Required: no 不覆盖）
    if (result.action === 'RESERVE') {
      const reserveAmount = Number(result.requiredProvision) - Number(result.allowanceBalance);
      await assertVoucherLines(page, result.voucherId, [
        { subjectCode: '6701', dcDirection: 'DEBIT', debitAmount: reserveAmount, creditAmount: 0 },
        { subjectCode: '1231', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: reserveAmount },
      ]);
    }

    // cleanup 计提凭证（按 voucherId 直接删 lines + voucher + bill_r，使 Allowance 账面恢复，不污染后续/基线）
    const ctx: CleanupCtx = { provisionVoucherId: result.voucherId ? Number(result.voucherId) : null };
    await cleanupBadDebt(page, ctx);
  });
});
