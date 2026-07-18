import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  eqFilter,
  deleteById,
} from './_helper';
import {
  findVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  findFirst,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * Finance ErpFinBadDebt reverseBadDebtProvision 反向坏账准备计提红冲闭环浏览器层 E2E
 * （plan 2026-07-18-2251-2 Phase 3）。
 *
 * 验证 finance 域 `ErpFinBadDebt.reverseBadDebtProvision(periodId)` 经 GraphQL /graphql 的全栈可达性 +
 * 反向指定期间全部 BAD_DEBT_RESERVE/RELEASE 已过账未冲销凭证：
 *   - 原凭证 isReversed=true
 *   - 红字凭证行同向取负（Dr 6701=-X / Cr 1231=-X）
 *   - GraphQL response 含 reversedReserveCount + reversedReserveAmount
 *
 * 权威设计（docs/design/finance/bad-debt.md §步骤2b 反向红冲，plan 2026-07-18-2251-2）：
 *   按 ErpFinVoucherBillR 反查 billCode = `BAD-DEBT-RESERVE-{period.code}` 或 `BAD-DEBT-RELEASE-{period.code}`
 *   完全匹配（无 UUID 后缀），过滤 isReversed=false NORMAL 凭证 → 调 FinPostingExecutor.reverse
 *   原子红冲（平台 reverseProcess 内部循环所有未冲销凭证）。
 *
 * 自包含 setup（复用 0413-2 既有 provision setup 范式）：每用例独立建 partner + OPEN RECEIVABLE
 * ErpFinArApItem（确保必需准备 > Allowance 账面，触发 BDR 凭证生成）→ runBadDebtProvision → reverseBadDebtProvision。
 *
 * cleanup：删 BDR/BDL 凭证（按 billCode 完全匹配）+ AR item + partner，使 finance 看板/报表基线无漂移。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1 / period id=1
 * （OPEN，code=2026-07，endDate=2026-07-31）。
 * 科目引用：1231 坏账准备 / 6701 信用减值损失（均种子已就绪，0413-2）。
 */
const ORG = 2;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;
const PERIOD = 1;
const PERIOD_CODE = '2026-07';
const AMOUNT = 5000;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createPartner(page: Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpMdPartner',
    {
      code: uniq('E2E-BADDEBT-PROV-REV-PN'),
      name: `E2E BadDebt Provision Reverse Partner ${tag}`,
      partnerType: 'CUSTOMER',
      status: 'ACTIVE',
    },
    'id',
  );
}

async function createOpenArItem(
  page: Page,
  partnerId: string | number,
  tag: string,
  amount = AMOUNT,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpFinArApItem',
    {
      code: uniq(`E2E-BADDEBT-PROV-REV-AR-${tag}`),
      orgId: ORG,
      acctSchemaId: ACCT_SCHEMA,
      direction: 'RECEIVABLE',
      partnerId,
      sourceBillType: 'AR_INVOICE',
      sourceBillCode: uniq(`E2E-BADDEBT-PROV-REV-INV-${tag}`),
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
  billCodes: string[];
}

async function cleanupCtx(page: Page, ctx: CleanupCtx): Promise<void> {
  for (const code of ctx.billCodes ?? []) {
    await cleanupVoucherByBillCode(page, code);
  }
  if (ctx.arItemId != null) {
    await deleteById(page, 'ErpFinArApItem', ctx.arItemId);
  }
  if (ctx.partnerId != null) {
    await deleteById(page, 'ErpMdPartner', ctx.partnerId);
  }
}

test.describe('Finance ErpFinBadDebt reverseBadDebtProvision browser-layer E2E (red reversal closure)', () => {
  test('(a) runBadDebtProvision → reverseBadDebtProvision: BDR voucher reversed + reversal lines negative', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    const partner = await createPartner(page, 'prov-rev');
    const item = await createOpenArItem(page, partner.id, 'PROV-REV');
    const reserveBillCode = `BAD-DEBT-RESERVE-${PERIOD_CODE}`;
    const ctx: CleanupCtx = {
      partnerId: partner.id,
      arItemId: item.id,
      billCodes: [reserveBillCode],
    };

    try {
      // 前置：runBadDebtProvision → 必需 > 账面 → RESERVE，产 1 张 BDR 凭证
      const provision = await callMutationOk(
        page, 'ErpFinBadDebt', 'runBadDebtProvision',
        { periodId: PERIOD },
        'action voucherId requiredProvision allowanceBalance',
      );
      expect(provision.action, 'precondition: 必需 > 账面 → RESERVE').toBe('RESERVE');
      expect(provision.voucherId, 'precondition: BDR 凭证应生成').toBeTruthy();
      const reserveAmount = Number(provision.requiredProvision) - Number(provision.allowanceBalance);

      // 原凭证存在 + isReversed=false（反向前）
      const originalVoucherId = await findVoucherIdByBillCode(page, reserveBillCode, 'NORMAL');
      expect(originalVoucherId, '原 BDR NORMAL 凭证应存在').toBeTruthy();
      const originalBefore = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id isReversed',
      );
      expect(originalBefore?.isReversed, '反向前原凭证 isReversed=false').toBe(false);

      // 反向坏账准备计提
      const reversed = await callMutationOk(
        page, 'ErpFinBadDebt', 'reverseBadDebtProvision',
        { periodId: PERIOD },
        'periodCode reversedReserveCount reversedReleaseCount reversedReserveAmount reversedReleaseAmount totalReversedCount',
      );
      expect(reversed.periodCode, '返回 periodCode').toBe(PERIOD_CODE);
      expect(reversed.reversedReserveCount, 'reversedReserveCount=1').toBe(1);
      expect(reversed.reversedReleaseCount, 'reversedReleaseCount=0').toBe(0);
      expect(Number(reversed.reversedReserveAmount), 'reversedReserveAmount=reserveAmount').toBe(reserveAmount);
      expect(reversed.totalReversedCount, 'totalReversedCount=1').toBe(1);

      // 原 NORMAL 凭证 isReversed=true（经 __get 独立断言）
      const originalAfter = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', originalVoucherId), 'id isReversed',
      );
      expect(originalAfter?.isReversed, '反向后原凭证 isReversed=true').toBe(true);

      // 红字 REVERSAL 凭证存在 + 行同向取负（Dr 6701=-X / Cr 1231=-X）
      const reversalVoucherId = await findVoucherIdByBillCode(page, reserveBillCode, 'REVERSAL');
      expect(reversalVoucherId, '红字 REVERSAL 凭证应生成').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '6701', dcDirection: 'DEBIT', debitAmount: -reserveAmount, creditAmount: 0 },
        { subjectCode: '1231', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -reserveAmount },
      ]);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });

  test('(b) guard: no BDR/BDL voucher for period → GraphQL error with not-found token', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinBadDebt-main');

    // 反向一个无 BDR/BDL 凭证的 period（自包含预清理 period 1 上的 BDR/BDL 凭证）
    const reserveBillCode = `BAD-DEBT-RESERVE-${PERIOD_CODE}`;
    const releaseBillCode = `BAD-DEBT-RELEASE-${PERIOD_CODE}`;
    await cleanupVoucherByBillCode(page, reserveBillCode);
    await cleanupVoucherByBillCode(page, releaseBillCode);

    const ctx: CleanupCtx = { billCodes: [reserveBillCode, releaseBillCode] };
    try {
      // 无 BDR/BDL 凭证 → GraphQL errors 含「未找到」/「坏账准备」语义 token
      const rej = await callMutation(
        page, 'ErpFinBadDebt', 'reverseBadDebtProvision',
        { periodId: PERIOD },
        'periodCode reversedReserveCount',
      );
      expect(rej.errors, '无 BDR/BDL 凭证应被守卫拒绝').toBeTruthy();
      expect(JSON.stringify(rej.errors), '错误消息含「未找到」或「坏账准备」语义').toMatch(/未找到|坏账准备/);
    } finally {
      await cleanupCtx(page, ctx);
    }
  });
});
